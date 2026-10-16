package org.rsmod.content.bosses.vorkath

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Torfinn : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (player.vars[TRAVEL_VARBIT] < TRAVEL_UNLOCKED && QuestRequirements.hasCompleted(player, DS2)) {
                VarPlayerIntMapSetter.set(player, TRAVEL_VARBIT, TRAVEL_UNLOCKED)
            }
        }
        onOpNpc1(AT_RELLEKKA) { talk(it.npc, "Ungael", VorkathArena.UNGAEL_DOCK) }
        onOpNpc3(AT_RELLEKKA) { sail(VorkathArena.UNGAEL_DOCK) }
        onOpNpc1(AT_UNGAEL) { talk(it.npc, "Rellekka", VorkathArena.RELLEKKA_DOCK) }
        onOpNpc3(AT_UNGAEL) { sail(VorkathArena.RELLEKKA_DOCK) }
        onOpLoc1(UNGAEL_BOAT) { sail(VorkathArena.RELLEKKA_DOCK) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, place: String, dest: CoordGrid) {
        startDialogue(npc) { torfinnDialogue(place, dest) }
    }

    private suspend fun Dialogue.torfinnDialogue(place: String, dest: CoordGrid) {
        chatNpc(neutral, "Ready to sail to $place?")
        val go = choice2("Yes, let's go.", true, "Not right now.", false)
        if (!go) return
        chatPlayer(neutral, "Yes, let's go.")
        access.sail(dest)
    }

    private fun ProtectedAccess.sail(dest: CoordGrid) {
        telejump(dest)
    }

    private companion object {
        const val AT_RELLEKKA = "npc.torfinn_travel_rellekka"
        const val AT_UNGAEL = "npc.torfinn_travel_ungael"
        const val UNGAEL_BOAT = "loc.ungael_boat"
        const val TRAVEL_VARBIT = "varbit.ds2_frem"
        const val TRAVEL_UNLOCKED = 20
        const val DS2 = "quest_dragonslayer2"
    }
}
