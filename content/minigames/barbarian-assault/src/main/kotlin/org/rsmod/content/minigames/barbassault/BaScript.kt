package org.rsmod.content.minigames.barbassault

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaScript @Inject constructor(private val service: BaService) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.barbassault_recruitment_entrance") { service.toggleQueue(player) }
        onOpLoc1("loc.barbassault_game_exit") { service.leave(player) }
        onPlayerSoftTimer(BaService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toOutpost = false) }
        onPlayerLogin {
            if (!service.isPlaying(player) && BaMap.inArena(player.coords)) service.leave(player)
        }
    }
}
