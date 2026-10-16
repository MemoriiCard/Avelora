package org.rsmod.content.minigames.barbassault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaMatchTest {
    private fun started(vararg roles: Pair<Int, BaRole>, waves: Int = 2): BaMatch {
        val match = BaMatch(waves = waves)
        roles.forEach { match.join(it.first, it.second, 0) }
        match.update(BaMatch.WAIT_TICKS)
        return match
    }

    private fun clearWave(match: BaMatch, cycle: Int, by: Int, kind: BaKind): List<BaEvent> {
        val events = mutableListOf<BaEvent>()
        while (match.monstersLeft > 0) events += match.monsterKilled(kind, by, cycle)
        return events
    }

    @Test
    fun `solo player starts after the wait`() {
        val match = BaMatch()
        match.join(1, BaRole.Attacker, 0)
        assertEquals(BaPhase.Waiting, match.phase)
        assertTrue(match.update(BaMatch.WAIT_TICKS - 1).isEmpty())
        val events = match.update(BaMatch.WAIT_TICKS)
        assertTrue(events.first() is BaEvent.GameStarted)
        assertEquals(BaPhase.Wave, match.phase)
        assertEquals(1, match.wave)
    }

    @Test
    fun `full squad of five starts immediately`() {
        val match = BaMatch()
        (1..5).forEach { match.join(it, BaRole.entries[it % 4], 0) }
        assertFalse(match.join(6, BaRole.Healer, 0))
        assertTrue(match.update(1).first() is BaEvent.GameStarted)
    }

    @Test
    fun `clearing a wave moves to intermission then the next wave`() {
        val match = started(1 to BaRole.Attacker)
        val events = clearWave(match, 50, 1, BaKind.Fighter)
        assertTrue(events.any { it is BaEvent.WaveCleared && it.wave == 1 })
        assertEquals(BaPhase.Intermission, match.phase)
        val next = match.update(50 + BaMatch.INTERMISSION_TICKS)
        assertTrue(next.single() is BaEvent.WaveStarted)
        assertEquals(2, match.wave)
    }

    @Test
    fun `kills outside a role's targets earn nothing`() {
        val match = started(1 to BaRole.Attacker, 2 to BaRole.Defender, waves = 5)
        val total = match.monstersLeft
        repeat(total - 2) { match.monsterKilled(BaKind.Fighter, 1, 40) }
        match.monsterKilled(BaKind.Fighter, 2, 40)
        assertEquals(total - 2, match.killsOf(1))
        assertEquals(1, match.killsOf(2))
        val events = match.monsterKilled(BaKind.Fighter, 2, 41)
        assertTrue(events.any { it is BaEvent.WaveCleared })
        assertEquals(0, match.pointsOf(2, BaRole.Defender))
        assertTrue(match.pointsOf(1, BaRole.Attacker) > 0)
    }

    @Test
    fun `cleared waves score their role`() {
        val match = started(1 to BaRole.Attacker)
        clearWave(match, 50, 1, BaKind.Fighter)
        assertTrue(match.pointsOf(1, BaRole.Attacker) > 0)
    }

    @Test
    fun `escaped runners count against defenders and still progress the wave`() {
        val match = started(1 to BaRole.Defender, waves = 5)
        val before = match.monstersLeft
        match.runnerEscaped(30)
        assertEquals(before - 1, match.monstersLeft)
    }

    @Test
    fun `healing and eggs only credit their roles`() {
        val match = started(1 to BaRole.Healer, 2 to BaRole.Collector, 3 to BaRole.Attacker)
        match.healed(1, 180)
        match.healed(3, 180)
        match.eggCollected(2)
        match.eggCollected(3)
        clearWave(match, 60, 3, BaKind.Fighter)
        assertEquals(10, match.pointsOf(1, BaRole.Healer))
        assertTrue(match.pointsOf(3, BaRole.Healer) == 0)
    }

    @Test
    fun `final wave pays eighty in role and five in the others and records the queen kill`() {
        val match = started(1 to BaRole.Collector, 2 to BaRole.Attacker, waves = 1)
        val events = clearWave(match, 70, 2, BaKind.Fighter)
        val result = events.filterIsInstance<BaEvent.GameEnded>().single().result
        assertTrue(result.cleared)
        assertFalse(result.queenKilled)

        val full = BaMatch(waves = BaWave.FINAL)
        full.join(1, BaRole.Collector, 0)
        full.update(BaMatch.WAIT_TICKS)
        var cycle = BaMatch.WAIT_TICKS
        for (wave in 1 until BaWave.FINAL) {
            clearWave(full, cycle, 1, BaKind.Fighter)
            cycle += BaMatch.INTERMISSION_TICKS
            full.update(cycle)
        }
        val ended = full.monsterKilled(BaKind.Queen, 1, cycle).filterIsInstance<BaEvent.GameEnded>().single()
        assertTrue(ended.result.queenKilled)
        val points = ended.result.points.getValue(1)
        assertTrue(points.getValue(BaRole.Collector) >= BaScoring.FINAL_WAVE_ROLE)
        assertEquals(BaScoring.FINAL_WAVE_OTHER, points.getValue(BaRole.Attacker))
    }

    @Test
    fun `wave timeout fails the run keeping cleared waves`() {
        val match = started(1 to BaRole.Attacker)
        clearWave(match, 50, 1, BaKind.Fighter)
        match.update(50 + BaMatch.INTERMISSION_TICKS)
        val events = match.update(50 + BaMatch.INTERMISSION_TICKS + BaMatch.WAVE_TICKS)
        val ended = events.filterIsInstance<BaEvent.GameEnded>().single()
        assertFalse(ended.result.cleared)
        assertEquals(1, ended.result.wavesCleared)
    }

    @Test
    fun `last player leaving ends the run`() {
        val match = started(1 to BaRole.Attacker, 2 to BaRole.Healer)
        assertTrue(match.leave(1, 140).none { it is BaEvent.GameEnded })
        val events = match.leave(2, 160)
        val ended = events.filterIsInstance<BaEvent.GameEnded>().single()
        assertFalse(ended.result.cleared)
        assertEquals(40, ended.result.participation.getValue(1))
        assertEquals(60, ended.result.participation.getValue(2))
    }

    @Test
    fun `leaving the queue does not affect the run and players cannot join mid-run`() {
        val match = BaMatch()
        match.join(1, BaRole.Attacker, 0)
        match.leave(1, 5)
        assertEquals(BaPhase.Idle, match.phase)
        val running = started(1 to BaRole.Attacker)
        assertFalse(running.join(2, BaRole.Healer, 10))
    }

    @Test
    fun `kills by players who left are not credited`() {
        val match = started(1 to BaRole.Attacker, 2 to BaRole.Attacker)
        match.leave(2, 10)
        match.monsterKilled(BaKind.Fighter, 2, 11)
        assertEquals(0, match.killsOf(2))
    }
}
