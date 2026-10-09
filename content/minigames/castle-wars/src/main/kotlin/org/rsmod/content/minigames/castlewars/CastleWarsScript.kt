package org.rsmod.content.minigames.castlewars

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CastleWarsScript @Inject constructor(private val service: CastleWarsService) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.castlewars_saradomin_tele") { service.enter(player, CwTeam.Saradomin) }
        onOpLoc1("loc.castlewars_zamorak_tele") { service.enter(player, CwTeam.Zamorak) }
        onOpLoc1("loc.castlewars_random_tele") { service.enter(player, null) }
        onOpLoc1("loc.castlewars_saradomin_exit") { leaveWaitingRoom() }
        onOpLoc1("loc.castlewars_zamorak_exit") { leaveWaitingRoom() }
        onOpLoc1("loc.castlewars_saradomin_quit") { service.leave(player) }
        onOpLoc1("loc.castlewars_zamorak_quit") { service.leave(player) }
        onOpLoc1("loc.castlewars_saradomin_banner+stand") { service.takeFlag(player, CwTeam.Saradomin) }
        onOpLoc1("loc.castlewars_zamorak_banner+stand") { service.takeFlag(player, CwTeam.Zamorak) }
        onPlayerSoftTimer(CastleWarsService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toLobby = false) }
        onPlayerLogin {
            if (service.teamOf(player) == null && CastleWarsMap.inside(player.coords)) {
                service.leave(player)
            }
        }
    }

    private fun ProtectedAccess.leaveWaitingRoom() {
        if (service.isPlaying(player)) {
            mes("You can't leave through here during the game.")
            return
        }
        service.leave(player)
    }
}
