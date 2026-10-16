package org.rsmod.content.minigames.framework

import kotlin.math.roundToInt

object Payout {
    const val BOT_PENALTY = 0.6
    const val FLOOR = 0.5
    const val WIN_BONUS = 1.5
    const val LOSS_SHARE = 0.6
    const val FIRST_WIN_MULTIPLIER = 2.0

    /** Share of the full reward paid when [botShare] of the game was bots. */
    fun botScale(botShare: Double): Double = (1.0 - botShare * BOT_PENALTY).coerceAtLeast(FLOOR)

    /**
     * Reward for one finished game. [base] is the loss-free participation amount before scaling; the
     * bot scale applies first, then the win and first-win bonuses stack on top.
     */
    fun reward(base: Int, botShare: Double, won: Boolean, firstWinToday: Boolean): Int {
        val scaled = base * botScale(botShare)
        val amount =
            if (won) {
                scaled * WIN_BONUS * (if (firstWinToday) FIRST_WIN_MULTIPLIER else 1.0)
            } else {
                scaled * WIN_BONUS * LOSS_SHARE
            }
        return amount.roundToInt()
    }
}
