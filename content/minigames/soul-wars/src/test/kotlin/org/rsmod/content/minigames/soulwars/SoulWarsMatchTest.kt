package org.rsmod.content.minigames.soulwars

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private inline fun <reified T> assertIs(value: Any?): T = assertInstanceOf(T::class.java, value)

class SoulWarsMatchTest {
    private val even = mapOf(SwTeam.Blue to 1.0, SwTeam.Red to 1.0)

    private fun started(wait: Int = 10, game: Int = 100): SoulWarsMatch {
        val match = SoulWarsMatch(wait, game)
        match.join(1, 0)
        match.join(2, 0)
        assertIs<SwEvent.GameStarted>(match.update(wait, even).single())
        return match
    }

    @Test
    fun `joiners alternate between teams`() {
        val match = SoulWarsMatch()
        assertEquals(SwTeam.Blue, match.join(1, 0))
        assertEquals(SwTeam.Red, match.join(2, 0))
        assertEquals(SwTeam.Blue, match.join(3, 0))
        assertEquals(SwTeam.Blue, match.join(3, 0))
    }

    @Test
    fun `wait restarts while a team is empty`() {
        val match = SoulWarsMatch(waitTicks = 10)
        match.join(1, 0)
        assertIs<SwEvent.WaitExtended>(match.update(10, even).single())
        assertEquals(10, match.ticksLeft(10))
        assertEquals(SwPhase.Waiting, match.phase)
    }

    @Test
    fun `game starts with both teams and moves players out of the lobby`() {
        val match = started()
        assertEquals(SwPhase.Playing, match.phase)
        assertEquals(mapOf(1 to SwTeam.Blue, 2 to SwTeam.Red), match.playing)
        assertTrue(match.waiting.isEmpty())
        assertNull(match.join(1, 20))
    }

    @Test
    fun `killing an avatar wins for the other team`() {
        val match = started()
        val ended = assertIs<SwEvent.GameEnded>(match.avatarKilled(SwTeam.Red, 40).single())
        assertEquals(SwTeam.Blue, ended.result.winner)
        assertEquals(SwEnding.AvatarKilled, ended.result.ending)
        assertEquals(SwPhase.Idle, match.phase)
    }

    @Test
    fun `timeout goes to the team whose avatar has more health`() {
        val match = started()
        val ended = assertIs<SwEvent.GameEnded>(
            match.update(110, mapOf(SwTeam.Blue to 0.4, SwTeam.Red to 0.9)).single(),
        )
        assertEquals(SwTeam.Red, ended.result.winner)
        assertEquals(SwEnding.Timeout, ended.result.ending)
    }

    @Test
    fun `equal avatars fall back to fragments then draw`() {
        val match = started()
        match.sacrifice(2, 7)
        val ended = assertIs<SwEvent.GameEnded>(match.update(110, even).single())
        assertEquals(SwTeam.Red, ended.result.winner)
        assertEquals(mapOf(2 to 7), ended.result.sacrificed)

        val drawn = started()
        val draw = assertIs<SwEvent.GameEnded>(drawn.update(110, even).single())
        assertNull(draw.result.winner)
    }

    @Test
    fun `sacrifice only counts during a game`() {
        val match = SoulWarsMatch()
        match.join(1, 0)
        assertNull(match.sacrifice(1, 3))
        val started = started()
        assertEquals(SwEvent.Sacrificed(1, 3, SwTeam.Blue), started.sacrifice(1, 3))
        assertEquals(3, started.fragmentsFor(SwTeam.Blue))
        assertNull(started.sacrifice(1, 0))
    }

    @Test
    fun `a team that empties forfeits`() {
        val match = started()
        val ended = assertIs<SwEvent.GameEnded>(match.leave(2, 30).single())
        assertEquals(SwTeam.Blue, ended.result.winner)
        assertEquals(SwEnding.Forfeit, ended.result.ending)
    }

    @Test
    fun `participation is measured from game start`() {
        val match = started()
        val ended = assertIs<SwEvent.GameEnded>(match.update(110, even).single())
        assertEquals(mapOf(1 to 100, 2 to 100), ended.result.participation)
    }

    @Test
    fun `players joining mid game queue for the next one`() {
        val match = started()
        match.join(9, 20)
        assertEquals(SwPhase.Playing, match.phase)
        match.update(110, even)
        assertEquals(SwPhase.Waiting, match.phase)
        assertTrue(9 in match.waiting)
    }

    private fun withBots(): SoulWarsMatch =
        SoulWarsMatch(10, 100) { blue, red -> mapOf(SwTeam.Blue to 5 - blue, SwTeam.Red to 5 - red) }

    @Test
    fun `a lone player starts against bots`() {
        val match = withBots()
        match.join(1, 0)
        val started = assertIs<SwEvent.GameStarted>(match.update(10, even).single())
        assertEquals(mapOf(SwTeam.Blue to 4, SwTeam.Red to 5), started.bots)
    }

    @Test
    fun `bot share reaches the result`() {
        val match = withBots()
        match.join(1, 0)
        match.update(10, even)
        val ended = match.update(110, even).filterIsInstance<SwEvent.GameEnded>().single()
        assertEquals(9.0 / 10.0, ended.result.botShare, 1e-9)
    }

    @Test
    fun `the game ends when the last human leaves a bot game`() {
        val match = withBots()
        match.join(1, 0)
        match.update(10, even)
        val ended = match.leave(1, 40).filterIsInstance<SwEvent.GameEnded>().single()
        assertNull(ended.result.winner)
    }

    @Test
    fun `soul rolls respect the weights`() {
        assertEquals(SoulKind.WeakA, SoulKind.roll(0))
        assertEquals(SoulKind.Wolf, SoulKind.roll(SoulKind.TOTAL_WEIGHT - 1))
        assertTrue(SoulKind.entries.all { it.fragments in 1..2 })
    }
}
