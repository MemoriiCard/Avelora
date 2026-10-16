package org.rsmod.content.minigames.barbassault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaMatchTest {
    private fun started(vararg ids: Int, waves: Int = 2): BaMatch {
        val match = BaMatch(waves = waves)
        ids.forEach { match.join(it, 0) }
        match.update(BaMatch.WAIT_TICKS)
        return match
    }

    private fun clearWave(match: BaMatch, cycle: Int, by: Int): List<BaEvent> {
        val events = mutableListOf<BaEvent>()
        while (match.monstersLeft > 0) events += match.monsterKilled(by, cycle)
        return events
    }

    @Test
    fun `solo player starts after the wait`() {
        val match = BaMatch()
        match.join(1, 0)
        assertEquals(BaPhase.Waiting, match.phase)
        assertTrue(match.update(BaMatch.WAIT_TICKS - 1).isEmpty())
        val events = match.update(BaMatch.WAIT_TICKS)
        assertTrue(events.first() is BaEvent.GameStarted)
        assertEquals(BaPhase.Wave, match.phase)
        assertEquals(1, match.wave)
    }

    @Test
    fun `full squad starts immediately`() {
        val match = BaMatch()
        listOf(1, 2, 3).forEach { match.join(it, 0) }
        assertFalse(match.join(4, 0).also { })
        assertTrue(match.update(1).first() is BaEvent.GameStarted)
    }

    @Test
    fun `monster count scales with wave and squad size`() {
        assertEquals(6, BaMatch.monstersFor(1, 1))
        assertEquals(8, BaMatch.monstersFor(1, 3))
        assertTrue(BaMatch.monstersFor(5, 3) > BaMatch.monstersFor(5, 1))
    }

    @Test
    fun `clearing a wave moves to intermission then the next wave`() {
        val match = started(1)
        val events = clearWave(match, 50, 1)
        assertTrue(events.any { it is BaEvent.WaveCleared && it.wave == 1 })
        assertEquals(BaPhase.Intermission, match.phase)
        val next = match.update(50 + BaMatch.INTERMISSION_TICKS)
        assertTrue(next.single() is BaEvent.WaveStarted)
        assertEquals(2, match.wave)
    }

    @Test
    fun `clearing the last wave ends as cleared with kills credited`() {
        val match = started(1, 2)
        clearWave(match, 50, 1)
        match.update(50 + BaMatch.INTERMISSION_TICKS)
        val events = clearWave(match, 120, 2)
        val ended = events.filterIsInstance<BaEvent.GameEnded>().single()
        assertTrue(ended.result.cleared)
        assertEquals(2, ended.result.wavesCleared)
        assertTrue(ended.result.kills.getValue(1) > 0)
        assertTrue(ended.result.kills.getValue(2) > 0)
        assertEquals(BaPhase.Idle, match.phase)
    }

    @Test
    fun `wave timeout fails the run keeping cleared waves`() {
        val match = started(1)
        clearWave(match, 50, 1)
        match.update(50 + BaMatch.INTERMISSION_TICKS)
        val events = match.update(50 + BaMatch.INTERMISSION_TICKS + BaMatch.WAVE_TICKS)
        val ended = events.filterIsInstance<BaEvent.GameEnded>().single()
        assertFalse(ended.result.cleared)
        assertEquals(1, ended.result.wavesCleared)
    }

    @Test
    fun `last player leaving ends the run`() {
        val match = started(1, 2)
        assertTrue(match.leave(1, 140).none { it is BaEvent.GameEnded })
        val events = match.leave(2, 160)
        val ended = events.filterIsInstance<BaEvent.GameEnded>().single()
        assertFalse(ended.result.cleared)
        assertEquals(40, ended.result.participation.getValue(1))
        assertEquals(60, ended.result.participation.getValue(2))
    }

    @Test
    fun `leaving the queue does not affect the run`() {
        val match = BaMatch()
        match.join(1, 0)
        match.leave(1, 5)
        assertEquals(BaPhase.Idle, match.phase)
    }

    @Test
    fun `players cannot join mid-run`() {
        val match = started(1)
        assertFalse(match.join(2, 10))
    }

    @Test
    fun `kills by players who left are not credited`() {
        val match = started(1, 2)
        match.leave(2, 10)
        match.monsterKilled(2, 11)
        assertEquals(0, match.killsOf(2))
    }
}
