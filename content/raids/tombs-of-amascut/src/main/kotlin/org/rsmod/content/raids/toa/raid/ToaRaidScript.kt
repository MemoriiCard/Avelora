package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.raids.toa.layout.ToaPath
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.puzzle.ApmekenPuzzleRoom
import org.rsmod.content.raids.toa.puzzle.CrondisPuzzleRoom
import org.rsmod.content.raids.toa.puzzle.HetPuzzleRoom
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRaidScript @Inject constructor(private val raids: ToaRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        for (path in ToaPath.entries) {
            onOpLoc1(path.door) { enterPath(path) }
            onOpLoc2(path.door) { enterPath(path) }
        }
        onOpLoc1("loc.toa_nexus_wardens_door") { wardens() }
        for (continueLoc in CONTINUES) {
            onOpLoc1(continueLoc) { continuePath() }
            onOpLoc2(continueLoc) { continuePath() }
        }
        for (exit in EXITS) onOpLoc1(exit) { leaveToNexus() }
        onOpLoc1("loc.toa_crondis_water_source") { fillContainer() }
        onOpLoc1("loc.toa_het_statue_parent") { (raids.containing(player)?.controller as? HetPuzzleRoom)?.takePickaxe(player) }
        onOpLoc2("loc.toa_het_statue_parent") { (raids.containing(player)?.controller as? HetPuzzleRoom)?.depositPickaxe(player) }
        onOpNpc1("npc.toa_het_goal") { (raids.containing(player)?.controller as? HetPuzzleRoom)?.destroy(player) }
        onOpNpc1("npc.toa_het_goal_vulnerable") { (raids.containing(player)?.controller as? HetPuzzleRoom)?.destroy(player) }
        for (hammer in listOf("loc.toa_path_apmeken_hammers", "loc.toa_path_apmeken_potions")) {
            onOpLoc1(hammer) { (raids.containing(player)?.controller as? ApmekenPuzzleRoom)?.takeHammer(player) }
        }
        onOpLoc1("loc.toa_path_apmeken_pillar") {
            (raids.containing(player)?.controller as? ApmekenPuzzleRoom)?.repair(player, it.loc.coords)
        }
        for (index in 0 until 4) {
            onOpNpc1("npc.toa_crondis_tree_${index + 1}") { waterTree(it.npc) }
        }
    }

    private fun ProtectedAccess.enterPath(path: ToaPath) {
        val raid = raids.containing(player) ?: return
        if (raid.room != ToaRoom.Nexus) return
        raids.enterPath(raid, path)
    }

    private fun ProtectedAccess.wardens() {
        if (raids.containing(player) == null) return
        mes("The way to the Wardens is sealed until every path is cleared.")
    }

    private fun ProtectedAccess.continuePath() {
        val raid = raids.containing(player) ?: return
        if (!raid.roomCleared) {
            mes("The way forward is still sealed.")
            return
        }
        raids.continuePath(raid)
    }

    private fun ProtectedAccess.leaveToNexus() {
        val raid = raids.containing(player) ?: return
        val path = raid.path ?: return
        if (raid.room == path.boss && raid.engaged && !raid.roomCleared) {
            mes("You can't leave while the fight is still going on.")
            return
        }
        raids.returnToNexus(raid)
    }

    private fun ProtectedAccess.fillContainer() {
        val raid = raids.containing(player) ?: return
        (raid.controller as? CrondisPuzzleRoom)?.fill(player)
    }

    private fun ProtectedAccess.waterTree(npc: Npc) {
        val raid = raids.containing(player) ?: return
        (raid.controller as? CrondisPuzzleRoom)?.water(player, npc)
    }

    private companion object {
        val CONTINUES = listOf("loc.toa_path_crondis_continue", "loc.toa_scabaras_continue", "loc.toa_door_continue", "loc.toa_path_apmeken_continue")
        val EXITS = listOf("loc.toa_zebak_exit", "loc.toa_entrance_kephri_main", "loc.toa_crondis_exit", "loc.toa_door_exit", "loc.toa_entrance_akkha01", "loc.toa_entrance_baba02")
    }
}
