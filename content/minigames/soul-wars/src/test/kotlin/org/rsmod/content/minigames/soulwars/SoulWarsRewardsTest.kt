package org.rsmod.content.minigames.soulwars

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SoulWarsRewardsTest {
    @Test
    fun `too little participation pays nothing`() {
        assertEquals(SwReward.NONE, SoulWarsRewards.reward(99, 10, true, 0.0, true))
    }

    @Test
    fun `win zeal is base times one and a half and first win doubles it`() {
        assertEquals(SwReward(9, 3), SoulWarsRewards.reward(500, 0, true, 0.0, false))
        assertEquals(SwReward(18, 6), SoulWarsRewards.reward(500, 0, true, 0.0, true))
    }

    @Test
    fun `fragments add flat zeal on top of the bonus-scaled base`() {
        assertEquals(SwReward(9 + 10, 3), SoulWarsRewards.reward(500, 5, true, 0.0, false))
    }

    @Test
    fun `a loss still pays about sixty percent of a win`() {
        assertEquals(SwReward(5, 2), SoulWarsRewards.reward(500, 0, false, 0.0, false))
    }
}
