package org.rsmod.content.minigames.castlewars

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CastleWarsRewardsTest {
    @Test
    fun `too little participation pays nothing`() {
        assertEquals(CwReward.NONE, CastleWarsRewards.reward(99, won = true, botShare = 0.0, firstWinToday = true))
    }

    @Test
    fun `win pays three times the old loss rate and first win doubles it`() {
        assertEquals(CwReward(6, 3), CastleWarsRewards.reward(500, won = true, botShare = 0.0, firstWinToday = false))
        assertEquals(CwReward(12, 6), CastleWarsRewards.reward(500, won = true, botShare = 0.0, firstWinToday = true))
    }

    @Test
    fun `a loss still pays about sixty percent of a win`() {
        assertEquals(CwReward(4, 2), CastleWarsRewards.reward(500, won = false, botShare = 0.0, firstWinToday = true))
    }

    @Test
    fun `bot share scales the payout down`() {
        assertEquals(CwReward(3, 2), CastleWarsRewards.reward(500, won = true, botShare = 1.0, firstWinToday = false))
    }
}
