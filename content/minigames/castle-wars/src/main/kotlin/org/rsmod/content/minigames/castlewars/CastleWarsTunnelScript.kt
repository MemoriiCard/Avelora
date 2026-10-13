package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CastleWarsTunnelScript
@Inject
constructor(
    private val service: CastleWarsService,
    private val tunnels: CastleWarsTunnels,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.castlewars_cavewall_rockslide") { collapse(it.loc.coords) }
        onOpLocU("loc.castlewars_cavewall_rockslide", POTION) { collapse(it.loc.coords, usePotion = true) }
        for (blocked in listOf("loc.castlewars_blocked_tunnel_1", "loc.castlewars_blocked_tunnel_2")) {
            onOpLoc1(blocked) { clear(it.loc.coords) }
            onOpLocU(blocked, POTION) { clear(it.loc.coords, usePotion = true) }
        }
    }

    private suspend fun ProtectedAccess.collapse(at: CoordGrid, usePotion: Boolean = false) {
        if (!playing()) return
        if (!tool(usePotion)) return
        if (usePotion) invDel(inv, POTION)
        val index = TunnelRules.nearest(at)
        if (tunnels.collapse(index)) {
            mes("The cave wall collapses, blocking the tunnel!")
        } else {
            mes("That tunnel is already blocked.")
        }
    }

    private suspend fun ProtectedAccess.clear(at: CoordGrid, usePotion: Boolean = false) {
        if (!playing()) return
        if (!tool(usePotion)) return
        if (usePotion) invDel(inv, POTION)
        if (!tunnels.clear(TunnelRules.nearest(at))) return
        mes("You clear some of the rocks.")
    }

    private fun ProtectedAccess.playing(): Boolean {
        if (service.isPlaying(player)) return true
        mes("You can only do that during a game.")
        return false
    }

    private fun ProtectedAccess.tool(usePotion: Boolean): Boolean {
        if (usePotion || hasPickaxe(player) || inv.contains(POTION)) return true
        mes("You need a pickaxe or an explosive potion to do that.")
        return false
    }

    private fun hasPickaxe(player: Player): Boolean {
        val worn = player.worn[Wearpos.RightHand.slot]
        return (player.inv.filterNotNull() + listOfNotNull(worn)).any { obj ->
            ServerCacheManager.getItem(obj.id)?.name?.contains("pickaxe", ignoreCase = true) == true
        }
    }

    private companion object {
        const val POTION = "obj.castlewars_explosives_potion"
    }
}
