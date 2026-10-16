package org.rsmod.content.minigames.soulwars

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SoulWarsScript @Inject constructor(private val service: SoulWarsService) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.soul_wars_leave_soulwars_portal") { service.toggleQueue(player) }
        onOpLoc1("loc.soul_wars_blue_exit_portal") { service.leave(player) }
        onOpLoc1("loc.soul_wars_red_exit_portal") { service.leave(player) }
        for (obelisk in OBELISKS) onOpLoc1(obelisk) { service.sacrifice(player) }
        onPlayerSoftTimer(SoulWarsService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toHub = false) }
        onPlayerLogin {
            if (service.teamOf(player) == null && SoulWarsMap.inArena(player.coords)) {
                service.leave(player)
            }
        }
    }

    private companion object {
        private val OBELISKS =
            listOf(
                "loc.soul_wars_central_obelisk_neutral",
                "loc.soul_wars_central_obelisk_blue",
                "loc.soul_wars_central_obelisk_red",
            )
    }
}
