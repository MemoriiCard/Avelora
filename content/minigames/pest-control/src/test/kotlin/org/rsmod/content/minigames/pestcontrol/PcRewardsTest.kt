package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PcRewardsTest {
    @Test
    fun `idle players earn nothing`() {
        assertEquals(PcReward.NONE, PcRewards.reward(2, 0, true, 0.0, false))
    }

    @Test
    fun `three kills or a portal qualifies`() {
        assertTrue(PcRewards.reward(3, 0, false, 0.0, false).points > 0)
        assertTrue(PcRewards.reward(0, 1, false, 0.0, false).points > 0)
    }

    @Test
    fun `winning pays more`() {
        val win = PcRewards.reward(5, 0, true, 0.0, false)
        val loss = PcRewards.reward(5, 0, false, 0.0, false)
        assertTrue(win.points > loss.points)
        assertTrue(win.tokens > loss.tokens)
    }

    @Test
    fun `portal kills add flat points`() {
        val a = PcRewards.reward(5, 0, false, 0.0, false)
        val b = PcRewards.reward(5, 3, false, 0.0, false)
        assertEquals(3 * PcRewards.POINTS_PER_PORTAL, b.points - a.points)
    }

    @Test
    fun `first win of the day doubles the win share`() {
        val first = PcRewards.reward(5, 0, true, 0.0, true)
        val later = PcRewards.reward(5, 0, true, 0.0, false)
        assertEquals(later.points * 2, first.points)
    }
}
