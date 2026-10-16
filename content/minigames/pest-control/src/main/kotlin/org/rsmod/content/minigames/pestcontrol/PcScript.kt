package org.rsmod.content.minigames.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PcScript
@Inject
constructor(private val service: PcService, private val players: PlayerList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (lander in PcLander.entries) {
            onOpLoc1(lander.gangplank) { service.toggleQueue(player, lander) }
            onOpNpc1(lander.squire) { talkToSquire(it.npc) }
        }
        onPlayerLogout { service.leave(player, toOutpost = false) }
        onPlayerLogin {
            if (!service.isPlaying(player) && service.inArena(player.coords)) service.leave(player)
        }
        bindEngagement()
        bindSeals()
    }

    private fun ScriptContext.bindEngagement() {
        val targets =
            PcNpcs.PESTS.map { it to PcMatch.ENGAGE_PEST } +
                (0 until PcMatch.PORTALS).map { PcNpcs.portal(it) to PcMatch.ENGAGE_PORTAL }
        for ((symbol, amount) in targets) {
            val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: continue
            val boost = if (symbol.contains("spinner")) PcMatch.ENGAGE_PORTAL else amount
            onNpcHit(type) {
                if (!hit.isFromPlayer) return@onNpcHit
                val source = hit.resolvePlayerSource(players) ?: return@onNpcHit
                service.engage(source, boost)
            }
        }
    }

    private fun ScriptContext.bindSeals() {
        for (charges in PcSeal.CHARGES) {
            onOpHeld4(PcSeal.symbol(charges)) { rub(it.slot, charges) }
        }
    }

    private fun ProtectedAccess.rub(slot: Int, charges: Int) {
        val struck = service.sealBlast(player)
        if (struck == null) {
            mes("The seal only holds its power inside Pest Control.")
            return
        }
        if (charges == 1) {
            invDel(inv, PcSeal.symbol(1), 1, slot = slot)
            mes("The last of the seal's power flares out and it dissolves.")
        } else {
            val next = ServerCacheManager.getItem(PcSeal.symbol(charges - 1).asRSCM(RSCMType.OBJ)) ?: return
            invReplaceSlot(inv, slot, 1, next)
            mes("The seal pulses and damages $struck nearby pests. It has ${charges - 1} charges left.")
        }
    }

    private suspend fun ProtectedAccess.talkToSquire(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "Do you want to leave the battle early? You won't earn a reward.")
            if (choice2("Yes, take me back.", true, "No, I'll keep fighting.", false)) {
                service.leave(player)
            }
        }
    }
}
