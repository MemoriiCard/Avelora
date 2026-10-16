package org.rsmod.content.minigames.pestcontrol

import org.rsmod.content.minigames.framework.Payout

data class PcReward(val points: Int, val tokens: Int, val coins: Int) {
    companion object {
        val NONE = PcReward(0, 0, 0)
    }
}

object PcRewards {
    const val BASE_TOKENS = 2
    const val POINTS_PER_PORTAL = 2
    const val COINS_PER_COMBAT_LEVEL = 10

    fun reward(
        lander: PcLander,
        combatLevel: Int,
        portalKills: Int,
        won: Boolean,
        botShare: Double,
        firstWinToday: Boolean,
    ): PcReward {
        val points =
            Payout.reward(lander.basePoints, botShare, won, firstWinToday) +
                portalKills * POINTS_PER_PORTAL
        val tokens = Payout.reward(BASE_TOKENS, botShare, won, firstWinToday)
        val coins = if (won) combatLevel * COINS_PER_COMBAT_LEVEL else 0
        return PcReward(points, tokens, coins)
    }
}
