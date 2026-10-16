package org.rsmod.content.minigames.pestcontrol

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

internal object PcPoints {
    const val VARP = "varp.pc_points"

    fun get(player: Player): Int = player.vars[VARP]

    fun add(player: Player, amount: Int) {
        val total = (get(player).toLong() + amount).coerceAtMost(PcShop.POINT_CAP.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, VARP, total)
    }

    fun spend(player: Player, amount: Int): Boolean {
        if (amount <= 0 || get(player) < amount) return false
        VarPlayerIntMapSetter.set(player, VARP, get(player) - amount)
        return true
    }
}
