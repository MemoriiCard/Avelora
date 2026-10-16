package org.rsmod.content.minigames.barbassault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaRewardsTest {
    @Test
    fun `too short or no waves pays nothing`() {
        assertEquals(BaReward.NONE, BaRewards.reward(50, 3, 4, false, 0.0, false))
        assertEquals(BaReward.NONE, BaRewards.reward(500, 0, 0, false, 0.0, false))
    }

    @Test
    fun `clearing pays more than failing`() {
        val win = BaRewards.reward(900, 5, 20, true, 0.0, false)
        val loss = BaRewards.reward(900, 5, 20, false, 0.0, false)
        assertTrue(win.honour > loss.honour)
        assertTrue(win.tokens > loss.tokens)
    }

    @Test
    fun `first clear of the day doubles the clear bonus`() {
        val first = BaRewards.reward(900, 5, 0, true, 0.0, true)
        val later = BaRewards.reward(900, 5, 0, true, 0.0, false)
        assertEquals(later.honour * 2, first.honour)
    }

    @Test
    fun `kills add flat honour`() {
        val a = BaRewards.reward(900, 3, 0, false, 0.0, false)
        val b = BaRewards.reward(900, 3, 10, false, 0.0, false)
        assertEquals(10, b.honour - a.honour)
    }
}
