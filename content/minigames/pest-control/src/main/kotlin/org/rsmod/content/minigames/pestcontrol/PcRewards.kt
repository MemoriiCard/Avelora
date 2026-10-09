package org.rsmod.content.minigames.pestcontrol

import org.rsmod.content.minigames.framework.Payout

data class PcReward(val points: Int, val tokens: Int) {
    companion object {
        val NONE = PcReward(0, 0)
    }
}

object PcRewards {
    const val MIN_KILLS = 3
    const val BASE_POINTS = 4
    const val BASE_TOKENS = 2
    const val POINTS_PER_PORTAL = 2

    fun reward(
        kills: Int,
        portalKills: Int,
        won: Boolean,
        botShare: Double,
        firstWinToday: Boolean,
    ): PcReward {
        if (kills < MIN_KILLS && portalKills == 0) return PcReward.NONE
        val points = Payout.reward(BASE_POINTS, botShare, won, firstWinToday) + portalKills * POINTS_PER_PORTAL
        return PcReward(points, Payout.reward(BASE_TOKENS, botShare, won, firstWinToday))
    }
}
