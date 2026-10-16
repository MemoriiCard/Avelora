package org.rsmod.content.minigames.lms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private inline fun <reified T> assertIs(value: Any?): T = assertInstanceOf(T::class.java, value)

class LmsMatchTest {
    private fun started(count: Int = 3, wait: Int = 10, game: Int = 200): LmsMatch {
        val match = LmsMatch(wait, game)
        for (id in 1..count) match.join(id, 0)
        assertIs<LmsEvent.GameStarted>(match.update(wait).single())
        return match
    }

    @Test
    fun `wait restarts until enough players have queued`() {
        val match = LmsMatch(waitTicks = 10)
        match.join(1, 0)
        assertIs<LmsEvent.WaitExtended>(match.update(10).single())
        assertEquals(10, match.ticksLeft(10))
        match.join(2, 12)
        assertIs<LmsEvent.GameStarted>(match.update(20).single())
    }

    @Test
    fun `a player cannot queue twice or join mid game`() {
        val match = started()
        assertFalse(match.join(1, 20))
        assertTrue(match.join(9, 20))
        assertTrue(9 in match.waiting)
        assertFalse(match.join(9, 21))
    }

    @Test
    fun `queue is capped`() {
        val match = LmsMatch(maxPlayers = 2)
        assertTrue(match.join(1, 0))
        assertTrue(match.join(2, 0))
        assertFalse(match.join(3, 0))
    }

    @Test
    fun `eliminations take placements from the bottom and credit living killers`() {
        val match = started(3)
        val first = match.eliminate(3, 1, 40)
        assertEquals(LmsEvent.Eliminated(3, 3, 1), first.single())
        assertEquals(1, match.killsOf(1))
        val last = match.eliminate(2, 1, 60)
        assertIs<LmsEvent.Eliminated>(last[0])
        val ended = assertIs<LmsEvent.GameEnded>(last[1])
        assertEquals(1, ended.result.winner)
        assertEquals(mapOf(3 to 3, 2 to 2, 1 to 1), ended.result.placements)
        assertEquals(2, ended.result.kills[1])
        assertEquals(LmsPhase.Idle, match.phase)
    }

    @Test
    fun `a dead killer is not credited`() {
        val match = started(4)
        match.eliminate(2, null, 30)
        val event = match.eliminate(3, 2, 40).single()
        assertEquals(LmsEvent.Eliminated(3, 3, null), event)
    }

    @Test
    fun `leaving mid game counts as elimination`() {
        val match = started(2)
        val events = match.leave(2, 30)
        assertEquals(1, assertIs<LmsEvent.GameEnded>(events[1]).result.winner)
    }

    @Test
    fun `participation stops when a player goes out`() {
        val match = started(2)
        val events = match.eliminate(2, 1, 150)
        val ended = assertIs<LmsEvent.GameEnded>(events[1])
        assertEquals(140, ended.result.participation[2])
        assertEquals(140, ended.result.participation[1])
    }

    @Test
    fun `timeout goes to the top killer or nobody on a tie`() {
        val match = started(4)
        match.eliminate(4, 1, 30)
        val ended = assertIs<LmsEvent.GameEnded>(match.update(210).single())
        assertEquals(1, ended.result.winner)

        val tied = started(3)
        assertNull(assertIs<LmsEvent.GameEnded>(tied.update(210).single()).result.winner)
    }

    private fun withBots(wait: Int = 10, game: Int = 200): LmsMatch =
        LmsMatch(wait, game, botPlan = { humans -> (6 - humans).coerceAtLeast(0) })

    @Test
    fun `a lone player starts against bots`() {
        val match = withBots()
        match.join(1, 0)
        val started = assertIs<LmsEvent.GameStarted>(match.update(10).single())
        assertEquals(setOf(1), started.players)
        assertEquals(5, started.bots.size)
        assertTrue(started.bots.all { it < 0 })
    }

    @Test
    fun `bot eliminations push the human placement up and credit the kill`() {
        val match = withBots()
        match.join(1, 0)
        match.update(10)
        val events = match.eliminate(-1, 1, 20)
        assertEquals(6, assertIs<LmsEvent.Eliminated>(events.single()).placement)
        assertEquals(1, match.killsOf(1))
    }

    @Test
    fun `the human wins by outlasting every bot and results exclude bots`() {
        val match = withBots()
        match.join(1, 0)
        match.update(10)
        var last = emptyList<LmsEvent>()
        for (bot in 1..5) last = match.eliminate(-bot, 1, 20 + bot)
        val ended = last.filterIsInstance<LmsEvent.GameEnded>().single().result
        assertEquals(1, ended.winner)
        assertEquals(mapOf(1 to 1), ended.placements)
        assertEquals(5.0 / 6.0, ended.botShare, 1e-9)
    }

    @Test
    fun `the game ends when the last human dies while bots remain`() {
        val match = withBots()
        match.join(1, 0)
        match.update(10)
        val ended = match.eliminate(1, null, 30).filterIsInstance<LmsEvent.GameEnded>().single().result
        assertNull(ended.winner)
        assertEquals(mapOf(1 to 6), ended.placements)
    }

    @Test
    fun `competitive games never use bots`() {
        val match = LmsMatch(waitTicks = 10, botPlan = { 5 })
        match.join(1, 0, LmsMode.Competitive)
        assertIs<LmsEvent.WaitExtended>(match.update(10).single())
    }
}
