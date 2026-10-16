package org.rsmod.content.minigames.lms

import org.rsmod.content.minigames.framework.Payout

data class LmsReward(val points: Int, val tokens: Int) {
    companion object {
        val NONE = LmsReward(0, 0)
    }
}

object LmsCompetitive {
    const val START_RATING = 500
    const val MAX_POINTS = 9

    fun points(placement: Int, kills: Int): Int {
        if (kills < 1) return 0
        val base =
            when (placement) {
                1 -> 6
                2 -> 5
                in 3..4 -> 4
                in 5..9 -> 3
                in 10..19 -> 1
                else -> 0
            }
        val bonus =
            when {
                kills >= 5 -> 3
                kills >= 3 -> 2
                else -> 0
            }
        return (base + bonus).coerceAtMost(MAX_POINTS)
    }

    fun ratingChange(placement: Int): Int =
        when (placement) {
            in 1..9 -> 10 - placement
            in 10..16 -> 0
            else -> -(placement - 16)
        }

    fun title(rating: Int): String =
        when {
            rating >= 1000 -> "Legend"
            rating >= 750 -> "Master"
            rating >= 600 -> "Veteran"
            else -> "Novice"
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
