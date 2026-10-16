package org.rsmod.content.minigames.framework

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object MinigameProfile {
    private const val TOKENS = "varp.minigame_tokens"
    private const val FIRST_WIN_DAY = "varp.minigame_first_win_day"

    fun tokens(player: Player): Int = player.vars[TOKENS]

    fun addTokens(player: Player, amount: Int) {
        VarPlayerIntMapSetter.set(player, TOKENS, (tokens(player).toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    fun spendTokens(player: Player, amount: Int): Boolean {
        if (tokens(player) < amount) return false
        VarPlayerIntMapSetter.set(player, TOKENS, tokens(player) - amount)
        return true
    }

    /** True once per [epochDay]; the call that returns true also records the win. */
    fun claimFirstWin(player: Player, epochDay: Long): Boolean {
        val stored = epochDay.toInt() + 1
        if (player.vars[FIRST_WIN_DAY] == stored) return false
        VarPlayerIntMapSetter.set(player, FIRST_WIN_DAY, stored)
        return true
    }
}
