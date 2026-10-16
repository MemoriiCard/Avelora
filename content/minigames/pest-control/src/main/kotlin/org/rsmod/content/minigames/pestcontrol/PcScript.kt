package org.rsmod.content.minigames.pestcontrol

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PcScript @Inject constructor(private val service: PcService) : PluginScript() {
    override fun ScriptContext.startup() {
        for (gangplank in GANGPLANKS) onOpLoc1(gangplank) { service.toggleQueue(player) }
        onPlayerSoftTimer(PcService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toOutpost = false) }
        onPlayerLogin {
            if (!service.isPlaying(player) && PcMap.inArena(player.coords)) service.leave(player)
        }
    }

    private companion object {
        private val GANGPLANKS =
            listOf(
                "loc.pest_lander_gangplank",
                "loc.pest_lander_gangplank_2",
                "loc.pest_lander_gangplank_3",
            )
    }
}
