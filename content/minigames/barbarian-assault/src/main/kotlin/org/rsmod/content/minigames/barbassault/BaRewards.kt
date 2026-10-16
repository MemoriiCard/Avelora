package org.rsmod.content.minigames.barbassault

import org.rsmod.content.minigames.framework.Payout

data class BaReward(val honour: Int, val tokens: Int) {
    companion object {
        val NONE = BaReward(0, 0)
    }
}

object BaRewards {
    const val MIN_PARTICIPATION_TICKS = 100
    const val HONOUR_PER_WAVE = 8
    const val HONOUR_PER_KILL = 1
    const val BASE_TOKENS = 2
    const val TOKENS_PER_WAVE = 1

    fun reward(
        participationTicks: Int,
        wavesCleared: Int,
        kills: Int,
        cleared: Boolean,
        botShare: Double,
        firstWinToday: Boolean,
    ): BaReward {
        if (participationTicks < MIN_PARTICIPATION_TICKS || wavesCleared == 0) return BaReward.NONE
        val honour = Payout.reward(wavesCleared * HONOUR_PER_WAVE, botShare, cleared, firstWinToday) + kills * HONOUR_PER_KILL
        val tokens = Payout.reward(BASE_TOKENS + wavesCleared * TOKENS_PER_WAVE, botShare, cleared, firstWinToday)
        return BaReward(honour, tokens)
    }
}
