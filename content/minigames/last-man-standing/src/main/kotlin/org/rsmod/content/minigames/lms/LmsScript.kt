package org.rsmod.content.minigames.lms

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LmsScript @Inject constructor(private val service: LmsService) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(WIZARD) { service.toggleQueue(player) }
        onOpNpc3(WIZARD) { service.toggleQueue(player) }
        for (chest in CHESTS) onOpLoc1(chest) { service.openChest(player, it.loc.coords) }
        onPlayerSoftTimer(LmsService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toLobby = false) }
        onPlayerLogin {
            if (!service.isPlaying(player) && LmsMap.inArena(player.coords)) service.leave(player)
        }
    }

    companion object {
        const val WIZARD = "npc.br_wizard_casual"
        val CHESTS =
            listOf(
                "loc.br_loot_chest_closed",
                "loc.br_loot_drawer_closed",
                "loc.br_loot_cupboard_closed",
            )
    }
}
