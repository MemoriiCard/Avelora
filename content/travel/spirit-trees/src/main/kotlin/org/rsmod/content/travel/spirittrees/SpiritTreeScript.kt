package org.rsmod.content.travel.spirittrees

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpiritTreeScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (loc in TRAVEL_LOCS) {
            onOpLoc1(loc) { travel() }
        }
    }

    private suspend fun ProtectedAccess.travel() {
        val labels = SpiritTrees.destinations.map { it.name } + CANCEL
        val choice = menu(TITLE, *labels.toTypedArray())
        val destination = SpiritTrees.destinations.getOrNull(choice) ?: return
        telejump(destination.coords)
    }

    private companion object {
        const val TITLE = "Where would you like to go?"
        const val CANCEL = "Stay here"

        val TRAVEL_LOCS =
            listOf(
                "loc.spirittree_small_2ops",
                "loc.spirittree_prif_2ops",
                "loc.spirittree_big_2ops",
                "loc.spirittree_big_2ops_orbs",
            )
    }
}
