package org.rsmod.content.minigames.barbassault

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaScript @Inject constructor(private val service: BaService) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.barbassault_recruitment_entrance") { enter() }
        onOpLoc1("loc.barbassault_game_exit") { service.leave(player) }
        onPlayerLogout { service.leave(player, toOutpost = false) }
        onPlayerLogin {
            if (!service.isPlaying(player) && service.inArena(player.coords)) service.leave(player)
        }
    }

    private suspend fun ProtectedAccess.enter() {
        if (service.isQueued(player)) {
            service.leaveQueue(player)
            return
        }
        startDialogue {
            val role =
                choice4(
                    BaRole.Attacker.label,
                    BaRole.Attacker,
                    BaRole.Defender.label,
                    BaRole.Defender,
                    BaRole.Healer.label,
                    BaRole.Healer,
                    BaRole.Collector.label,
                    BaRole.Collector,
                    title = "Choose your role",
                )
            service.joinQueue(player, role)
        }
    }
}
