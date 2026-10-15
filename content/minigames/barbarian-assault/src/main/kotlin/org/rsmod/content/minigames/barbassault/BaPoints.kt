package org.rsmod.content.minigames.barbassault

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

internal object BaPoints {
    const val QUEEN_KILLS = "varp.ba_queen_kills"
    const val PROGRESS = "varp.ba_gamble_progress"

    fun get(player: Player, role: BaRole): Int = player.vars[role.varp]

    fun level(player: Player, role: BaRole): Int = BaLevels.level(get(player, role))

    fun add(player: Player, role: BaRole, amount: Int) {
        val total = (get(player, role).toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, role.varp, total)
    }

    fun spend(player: Player, role: BaRole, amount: Int): Boolean {
        if (amount <= 0 || get(player, role) < amount) return false
        VarPlayerIntMapSetter.set(player, role.varp, get(player, role) - amount)
        return true
    }

    fun queenKills(player: Player): Int = player.vars[QUEEN_KILLS]

    fun gambleProgress(player: Player): Int = player.vars[PROGRESS]

    fun addQueenKill(player: Player) {
        VarPlayerIntMapSetter.set(player, QUEEN_KILLS, queenKills(player) + 1)
    }

    fun recordProgress(player: Player, wavesCleared: Int) {
        VarPlayerIntMapSetter.set(player, PROGRESS, maxOf(gambleProgress(player), wavesCleared))
    }

    fun consumeGamble(player: Player, tier: GambleTier) {
        VarPlayerIntMapSetter.set(player, PROGRESS, 0)
        if (tier.needsQueen) VarPlayerIntMapSetter.set(player, QUEEN_KILLS, queenKills(player) - 1)
    }
}
