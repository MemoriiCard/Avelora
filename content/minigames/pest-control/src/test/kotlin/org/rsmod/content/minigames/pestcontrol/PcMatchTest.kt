package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PcMatchTest {
    private fun started(vararg ids: Int, gameTicks: Int = PcMatch.GAME_TICKS): PcMatch {
        val match = PcMatch(gameTicks = gameTicks)
        ids.forEach { match.join(it, 0) }
        match.update(PcMatch.WAIT_TICKS)
        return match
    }

    private fun ended(events: List<PcEvent>): PcResult? =
        events.filterIsInstance<PcEvent.GameEnded>().singleOrNull()?.result

    @Test
    fun `a single player can start a game`() {
        val match = PcMatch()
        match.join(1, 0)
        assertTrue(match.update(PcMatch.WAIT_TICKS - 1).isEmpty())
        assertTrue(match.update(PcMatch.WAIT_TICKS).single() is PcEvent.GameStarted)
        assertEquals(PcPhase.Playing, match.phase)
        assertEquals(4, match.portalsAlive)
    }

    @Test
    fun `a full lander departs early`() {
        val match = PcMatch(maxPlayers = 2)
        match.join(1, 0)
        match.join(2, 0)
        assertFalse(match.join(3, 0))
        assertTrue(match.update(1).single() is PcEvent.GameStarted)
    }

    @Test
    fun `cannot join a running game`() {
        val match = started(1)
        assertFalse(match.join(2, 10))
    }

    @Test
    fun `destroying every portal wins`() {
        val match = started(1)
        for (i in 0..2) assertTrue(match.portalKilled(i, 1, 200).none { it is PcEvent.GameEnded })
        val result = ended(match.portalKilled(3, 1, 300))!!
        assertTrue(result.won)
        assertEquals(PcEnding.PortalsDestroyed, result.ending)
        assertEquals(4, result.portalKills.getValue(1))
        assertEquals(PcPhase.Idle, match.phase)
    }

    @Test
    fun `the same portal cannot die twice`() {
        val match = started(1)
        match.portalKilled(0, 1, 200)
        assertTrue(match.portalKilled(0, 1, 201).isEmpty())
        assertEquals(3, match.portalsAlive)
    }

    @Test
    fun `pests chip the knight until he falls`() {
        val match = started(1)
        var cycle = PcMatch.WAIT_TICKS
        var result: PcResult? = null
        while (result == null && cycle < PcMatch.WAIT_TICKS + PcMatch.GAME_TICKS - 1) {
            cycle++
            result = ended(match.update(cycle, pestsAlive = 30))
        }
        assertEquals(PcEnding.KnightFell, result!!.ending)
        assertFalse(result.won)
        assertEquals(0, match.knightHp)
    }

    @Test
    fun `no pests means the knight is never hurt`() {
        val match = started(1)
        for (cycle in 101..400) assertTrue(match.update(cycle, 0).isEmpty())
        assertEquals(PcMatch.KNIGHT_HITPOINTS, match.knightHp)
    }

    @Test
    fun `running out the clock loses`() {
        val match = started(1, gameTicks = 200)
        val result = ended(match.update(PcMatch.WAIT_TICKS + 200))!!
        assertEquals(PcEnding.Timeout, result.ending)
    }

    @Test
    fun `everyone leaving abandons the game`() {
        val match = started(1, 2)
        assertTrue(match.leave(1, 150).isEmpty())
        val result = ended(match.leave(2, 180))!!
        assertEquals(PcEnding.Abandoned, result.ending)
        assertEquals(50, result.participation.getValue(1))
        assertEquals(80, result.participation.getValue(2))
    }

    @Test
    fun `kills only count for active players`() {
        val match = started(1, 2)
        match.pestKilled(1)
        match.leave(2, 150)
        match.pestKilled(2)
        assertEquals(1, match.killsOf(1))
        assertEquals(0, match.killsOf(2))
    }

    @Test
    fun `portal hitpoints grow with the squad`() {
        assertTrue(PcMatch.portalHitpoints(10) > PcMatch.portalHitpoints(1))
        assertEquals(PcMatch.portalHitpoints(25), PcMatch.portalHitpoints(60))
    }
}
