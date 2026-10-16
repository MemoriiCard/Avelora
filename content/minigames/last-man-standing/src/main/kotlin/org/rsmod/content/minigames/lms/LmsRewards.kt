package org.rsmod.content.minigames.lms

import org.rsmod.content.minigames.framework.Payout

data class LmsReward(val points: Int, val tokens: Int) {
    companion object {
        val NONE = LmsReward(0, 0)
    }
}

object LmsRewards {
    const val MIN_PARTICIPATION_TICKS = 100
    const val BASE_POINTS = 6
    const val BASE_TOKENS = 2
    const val POINTS_PER_KILL = 3

    fun reward(
        participationTicks: Int,
        kills: Int,
        won: Boolean,
        botShare: Double,
        firstWinToday: Boolean,
    ): LmsReward {
        if (participationTicks < MIN_PARTICIPATION_TICKS) return LmsReward.NONE
        val points = Payout.reward(BASE_POINTS, botShare, won, firstWinToday) + kills * POINTS_PER_KILL
        return LmsReward(points, Payout.reward(BASE_TOKENS, botShare, won, firstWinToday))
    }
}
