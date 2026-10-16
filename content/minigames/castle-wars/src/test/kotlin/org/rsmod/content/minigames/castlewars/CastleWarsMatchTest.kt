package org.rsmod.content.minigames.castlewars

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private inline fun <reified T> assertIs(value: Any?): T = assertInstanceOf(T::class.java, value)

class CastleWarsMatchTest {
    private fun started(wait: Int = 10, game: Int = 100): CastleWarsMatch {
        val match = CastleWarsMatch(wait, game)
        match.join(1, CwTeam.Saradomin, 0)
        match.join(2, CwTeam.Zamorak, 0)
        val events = match.update(wait)
        assertIs<CwEvent.GameStarted>(events.single())
        return match
    }

    @Test
    fun `random join fills the smaller team`() {
        val match = CastleWarsMatch()
        assertEquals(CwTeam.Saradomin, match.join(1, null, 0))
        assertEquals(CwTeam.Zamorak, match.join(2, null, 0))
        assertEquals(CwTeam.Saradomin, match.join(3, null, 0))
    }

    @Test
    fun `preferred team is refused once it is two players ahead`() {
        val match = CastleWarsMatch()
        assertEquals(CwTeam.Saradomin, match.join(1, CwTeam.Saradomin, 0))
        assertEquals(CwTeam.Saradomin, match.join(2, CwTeam.Saradomin, 0))
        assertNull(match.join(3, CwTeam.Saradomin, 0))
        assertEquals(CwTeam.Zamorak, match.join(3, CwTeam.Zamorak, 0))
    }

    @Test
    fun `game does not start with an empty team and the wait restarts`() {
        val match = CastleWarsMatch(waitTicks = 10)
        match.join(1, CwTeam.Saradomin, 0)
        assertIs<CwEvent.WaitExtended>(match.update(10).single())
        assertEquals(CwPhase.Waiting, match.phase)
        assertEquals(10, match.ticksLeft(10))
    }

    @Test
    fun `game starts once both teams have a player`() {
        val match = started()
        assertEquals(CwPhase.Playing, match.phase)
        assertEquals(mapOf(1 to CwTeam.Saradomin, 2 to CwTeam.Zamorak), match.playing)
        assertTrue(match.waiting.isEmpty())
    }

    @Test
    fun `taking and capturing the enemy flag scores for the capturer`() {
        val match = started()
        assertIs<CwEvent.FlagTaken>(match.takeFlag(1, CwTeam.Zamorak))
        assertEquals(1, match.carrierOf(CwTeam.Zamorak))
        assertIs<CwEvent.Scored>(match.capture(1))
        assertEquals(1, match.score(CwTeam.Saradomin))
        assertNull(match.carrierOf(CwTeam.Zamorak))
    }

    @Test
    fun `cannot take your own flag or a flag that is already carried`() {
        val match = started()
        assertNull(match.takeFlag(1, CwTeam.Saradomin))
        match.takeFlag(1, CwTeam.Zamorak)
        assertNull(match.takeFlag(1, CwTeam.Zamorak))
    }

    @Test
    fun `cannot capture while your own flag is away from base`() {
        val match = started()
        match.takeFlag(1, CwTeam.Zamorak)
        match.takeFlag(2, CwTeam.Saradomin)
        assertNull(match.capture(1))
        assertEquals(0, match.score(CwTeam.Saradomin))
    }

    @Test
    fun `dying with a flag returns it to base`() {
        val match = started()
        match.takeFlag(1, CwTeam.Zamorak)
        assertEquals(CwEvent.FlagReturned(CwTeam.Zamorak), match.onDeath(1))
        assertNull(match.carrierOf(CwTeam.Zamorak))
        assertNull(match.onDeath(2))
    }

    @Test
    fun `higher score wins when time runs out and equal scores draw`() {
        val match = started()
        match.takeFlag(1, CwTeam.Zamorak)
        match.capture(1)
        val ended = assertIs<CwEvent.GameEnded>(match.update(10 + 100).single())
        assertEquals(CwTeam.Saradomin, ended.result.winner)
        assertEquals(CwPhase.Idle, match.phase)

        val draw = started()
        val drawn = assertIs<CwEvent.GameEnded>(draw.update(10 + 100).single())
        assertNull(drawn.result.winner)
    }

    @Test
    fun `a team that empties forfeits and the other team wins`() {
        val match = started()
        val events = match.leave(2, 50)
        val ended = assertIs<CwEvent.GameEnded>(events.single())
        assertEquals(CwTeam.Saradomin, ended.result.winner)
        assertEquals(mapOf(1 to CwTeam.Saradomin), ended.result.players)
    }

    @Test
    fun `leaving with a flag returns it`() {
        val match = CastleWarsMatch(10, 100)
        listOf(1 to CwTeam.Saradomin, 2 to CwTeam.Zamorak, 3 to CwTeam.Zamorak).forEach { match.join(it.first, it.second, 0) }
        match.update(10)
        match.takeFlag(3, CwTeam.Saradomin)
        val events = match.leave(3, 20)
        assertEquals(listOf<CwEvent>(CwEvent.FlagReturned(CwTeam.Saradomin)), events)
        assertEquals(CwPhase.Playing, match.phase)
    }

    @Test
    fun `players who joined during a game wait for the next one`() {
        val match = started()
        match.join(9, null, 20)
        assertEquals(CwPhase.Playing, match.phase)
        assertTrue(9 in match.waiting)
        match.update(110)
        assertEquals(CwPhase.Waiting, match.phase)
        assertEquals(10, match.ticksLeft(110))
    }

    @Test
    fun `participation ticks are recorded per player`() {
        val match = started()
        val ended = assertIs<CwEvent.GameEnded>(match.update(110).single())
        assertEquals(mapOf(1 to 100, 2 to 100), ended.result.participation)
    }
}
