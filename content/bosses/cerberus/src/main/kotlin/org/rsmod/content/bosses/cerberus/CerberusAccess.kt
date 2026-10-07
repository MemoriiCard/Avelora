package org.rsmod.content.bosses.cerberus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CerberusAccess @Inject internal constructor(private val fights: CerberusFights) : PluginScript() {
    override fun ScriptContext.startup() {
        for (entrance in TAVERLEY_ENTRANCES) onOpLoc1(entrance) { telejump(CerberusLair.HUB) }
        for (exit in HUB_EXITS) onOpLoc1(exit) { telejump(CerberusLair.TAVERLEY_RETURN) }
        onOpLoc1(WINCH) { turnWinch(CerberusArena.byWinch(it.loc.coords)) }
        onOpLoc2(WINCH) { peek(CerberusArena.byWinch(it.loc.coords)) }
        onOpLoc1(GATE) { leaveArena(CerberusArena.containing(it.loc.coords.translate(0, 1))) }
    }

    private suspend fun ProtectedAccess.turnWinch(arena: CerberusArena?) {
        if (arena == null || !canEnter()) return
        anim(WINCH_SEQ)
        delay(WINCH_TICKS)
        telejump(arena.landing)
        fights.enterArena(player, arena)
    }

    private fun ProtectedAccess.peek(arena: CerberusArena?) {
        if (arena == null) return
        val inside = fights.playersIn(arena).size
        mes(if (inside == 1) "You peek down and see 1 adventurer." else "You peek down and see $inside adventurers.")
    }

    private fun ProtectedAccess.leaveArena(arena: CerberusArena?) {
        if (arena == null) return
        telejump(arena.hubReturn)
        fights.resetIfEmpty(arena)
    }

    private fun ProtectedAccess.canEnter(): Boolean {
        if (player.statBase("stat.slayer") < CerberusLair.SLAYER_LEVEL) {
            mes("You need a Slayer level of ${CerberusLair.SLAYER_LEVEL} to use this winch.")
            return false
        }
        val task = SlayerTaskManager.getCurrentSlayerTask(this)
        val onTask = task != null && TASK_TYPES.any { SlayerTaskManager.isTaskNpcType(it, task.id) }
        if (!onTask) {
            mes("You need to be on a Hellhound or Cerberus Slayer task to use this winch.")
            return false
        }
        return true
    }

    private companion object {
        const val WINCH = "loc.hellhound_winch"
        const val GATE = "loc.hellhound_iron_gate_active"
        const val WINCH_SEQ = "seq.human_turn_iron_winch"
        const val WINCH_TICKS = 2

        val TAVERLEY_ENTRANCES =
            listOf("loc.hellhound_cave_entrance_a_01", "loc.hellhound_cave_entrance_b_02", "loc.hellhound_cave_entrance_c_03")
        val HUB_EXITS =
            listOf("loc.hellhound_cave_entrance_01", "loc.hellhound_cave_entrance_02", "loc.hellhound_cave_entrance_03")

        val TASK_TYPES by lazy {
            listOf("npc.hellhound", CERBERUS).mapNotNull { ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC)) }
        }
    }
}
