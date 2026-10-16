package org.rsmod.content.minigames.soulwars

import org.rsmod.content.minigames.framework.Payout

data class SwReward(val zeal: Int, val tokens: Int) {
    companion object {
        val NONE = SwReward(0, 0)
    }
}

object SoulWarsRewards {
    const val MIN_PARTICIPATION_TICKS = 100
    const val BASE_ZEAL = 6
    const val BASE_TOKENS = 2
    const val ZEAL_PER_FRAGMENT = 2

    fun reward(
        participationTicks: Int,
        fragmentsSacrificed: Int,
        won: Boolean,
        botShare: Double,
        firstWinToday: Boolean,
    ): SwReward {
        if (participationTicks < MIN_PARTICIPATION_TICKS) return SwReward.NONE
        val zeal = Payout.reward(BASE_ZEAL, botShare, won, firstWinToday) + fragmentsSacrificed * ZEAL_PER_FRAGMENT
        return SwReward(zeal, Payout.reward(BASE_TOKENS, botShare, won, firstWinToday))
    }
}
