package org.rsmod.content.bosses.vetion

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.content.areas.wilderness.checkWildernessBossFee
import org.rsmod.content.areas.wilderness.tryPayWildernessBossFee
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VetionRest @Inject constructor(private val playerList: PlayerList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (lair in VETION_LAIRS) {
            onOpLoc1(lair.entrance) { enter(lair) }
            onOpLoc2(lair.entrance) { peek(lair) }
            onOpLoc3(lair.entrance) { checkWildernessBossFee(ENTRY_FEE) }
        }
        onOpLoc1(EXIT_LOC) { leave() }
    }

    private suspend fun ProtectedAccess.enter(lair: VetionLair) {
        arriveDelay()
        tryPayWildernessBossFee(ENTRY_FEE) { source ->
            mes("<col=ef1020>You enter the lair and $FEE_TEXT is taken from $source to pay the entry fee.")
            telejump(lair.arrival, TeleportType.Exempt)
        }
    }

    private fun ProtectedAccess.peek(lair: VetionLair) {
        val count = playerList.count { lair.contains(it.coords) }
        when (count) {
            0 -> mes("The lair is currently empty.")
            1 -> mes("There is 1 player currently in the lair.")
            else -> mes("There are $count players currently in the lair.")
        }
    }

    private suspend fun ProtectedAccess.leave() {
        arriveDelay()
        val lair = VETION_LAIRS.firstOrNull { it.contains(player.coords) || it.arrival.chebyshevDistance(player.coords) <= EXIT_REACH } ?: return
        telejump(lair.surface, TeleportType.Exempt)
    }

    private companion object {
        const val EXIT_LOC = "loc.wild_vetion_exit01"
        const val EXIT_REACH = 8
        const val ENTRY_FEE = 50_000
        const val FEE_TEXT = "50,000 coins"
    }
}
