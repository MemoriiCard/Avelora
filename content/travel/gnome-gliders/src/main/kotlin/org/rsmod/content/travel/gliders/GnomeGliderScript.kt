package org.rsmod.content.travel.gliders

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.previousDestination by intVarBit("varbit.pilot_previous_destination")

class GnomeGliderScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in pilotNpcs()) {
            onOpNpc1(npc) { openMap() }
            onOpNpc4(npc) { flyToPrevious() }
        }
        for (destination in GliderDestination.entries) {
            onIfModalButton("component.glidermap:${destination.button}") { fly(destination) }
        }
    }

    private fun pilotNpcs(): List<String> {
        val names = linkedSetOf<String>()
        for (parent in PILOTS) {
            names += parent
            val type = ServerCacheManager.getNpc(parent.asRSCM(RSCMType.NPC)) ?: continue
            for (child in type.transforms.orEmpty().filter { it >= 0 }) {
                names += RSCM.getReverseMapping(RSCMType.NPC, child)
            }
        }
        return names.toList()
    }

    private fun ProtectedAccess.openMap() {
        ifOpenMainModal(INTERFACE)
    }

    private fun ProtectedAccess.flyToPrevious() {
        val destination = GliderDestination.byCode(player.previousDestination) ?: return openMap()
        fly(destination)
    }

    private fun ProtectedAccess.fly(destination: GliderDestination) {
        if (player.distanceTo(destination.coords) <= HERE_DISTANCE) {
            mes("You are already at ${destination.label}.")
            return
        }
        ifClose()
        player.previousDestination = destination.code
        telejump(destination.coords)
    }

    private companion object {
        const val INTERFACE = "interface.glidermap"
        const val HERE_DISTANCE = 20

        val PILOTS =
            listOf(
                "npc.pilot_grand_tree",
                "npc.pilot_karamja",
                "npc.pilot_al_kharid",
                "npc.pilot_white_wolf",
                "npc.pilot_apeatoll",
                "npc.pilot_digsite",
            )
    }
}
