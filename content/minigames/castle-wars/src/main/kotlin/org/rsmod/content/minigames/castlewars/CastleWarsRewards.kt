package org.rsmod.content.minigames.castlewars

import org.rsmod.content.minigames.framework.Payout

data class CwReward(val tickets: Int, val tokens: Int) {
    companion object {
        val NONE = CwReward(0, 0)
    }
}

object CastleWarsRewards {
    const val MIN_PARTICIPATION_TICKS = 100
    const val BASE_TICKETS = 4
    const val BASE_TOKENS = 2

    fun reward(participationTicks: Int, won: Boolean, botShare: Double, firstWinToday: Boolean): CwReward {
        if (participationTicks < MIN_PARTICIPATION_TICKS) return CwReward.NONE
        return CwReward(
            tickets = Payout.reward(BASE_TICKETS, botShare, won, firstWinToday),
            tokens = Payout.reward(BASE_TOKENS, botShare, won, firstWinToday),
        )
    }
}
