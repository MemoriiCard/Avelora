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
}
