package org.rsmod.content.minigames.barbassault

import org.rsmod.content.minigames.framework.Payout

object BaRewards {
    const val MIN_PARTICIPATION_TICKS = 100
    const val BASE_TOKENS = 2
    const val TOKENS_PER_WAVE = 1

    fun tokens(
        participationTicks: Int,
        wavesCleared: Int,
        cleared: Boolean,
        botShare: Double,
        firstWinToday: Boolean,
    ): Int {
        if (participationTicks < MIN_PARTICIPATION_TICKS || wavesCleared == 0) return 0
        return Payout.reward(BASE_TOKENS + wavesCleared * TOKENS_PER_WAVE, botShare, cleared, firstWinToday)
    }

    fun eligible(participationTicks: Int, wavesCleared: Int): Boolean =
        participationTicks >= MIN_PARTICIPATION_TICKS && wavesCleared > 0
}
