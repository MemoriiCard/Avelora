package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PcRewardsTest {
    private fun reward(
        lander: PcLander = PcLander.Novice,
        portalKills: Int = 0,
        won: Boolean = true,
        firstWin: Boolean = false,
    ) = PcRewards.reward(lander, 80, portalKills, won, 0.0, firstWin)

    @Test
    fun `winning pays more`() {
        val win = reward()
        val loss = reward(won = false)
        assertTrue(win.points > loss.points)
        assertTrue(win.tokens > loss.tokens)
    }

    @Test
    fun `harder landers pay more`() {
        val points = PcLander.entries.map { reward(it).points }
        assertEquals(points.sorted(), points)
        assertTrue(points.first() < points.last())
    }

    @Test
    fun `coins are ten times combat level and only for a win`() {
        assertEquals(800, reward().coins)
        assertEquals(0, reward(won = false).coins)
    }

    @Test
    fun `portal kills add flat points`() {
        val a = reward(won = false)
        val b = reward(portalKills = 3, won = false)
        assertEquals(3 * PcRewards.POINTS_PER_PORTAL, b.points - a.points)
    }

    @Test
    fun `first win of the day pays extra`() {
        val first = reward(firstWin = true)
        val later = reward()
        assertTrue(first.points > later.points)
        assertTrue(first.tokens > later.tokens)
    }
}
