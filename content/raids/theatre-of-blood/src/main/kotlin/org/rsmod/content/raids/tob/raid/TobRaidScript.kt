package org.rsmod.content.raids.tob.raid

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TobRaidScript @Inject constructor(private val raids: TobRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.tob_arena_barrier") { passBarrier(it.loc) }
        onOpLoc1("loc.tob_dungeon_walkway_exit_clickbox") { advance() }
        onOpLoc2("loc.tob_dungeon_walkway_exit_clickbox") { advance() }
        onOpLoc1("loc.tob_dungeon_xarpus_arena_door_exit") { advance() }
        onOpLoc2("loc.tob_dungeon_xarpus_arena_door_exit") { advance() }
        onOpLoc1("loc.tob_treasureroom_teleportout") { leaveRaid() }
    }

    private fun ProtectedAccess.passBarrier(loc: BoundLocInfo) {
        val raid = raids.containing(player) ?: return
        val room = raid.roomAt(player.coords) ?: return
        if (room != raid.room) return
        val arena = raid.coords(room, room.arena)
        val from = player.coords
        val destination = across(loc, from)
        val entering = distance(destination, arena) < distance(from, arena)
        if (entering && raid.roomCleared) {
            mes("There is nothing left to fight in here.")
            return
        }
        if (!entering && raid.engaged && !raid.roomCleared) {
            mes("You can't leave the arena while the fight is still going on.")
            return
        }
        telejump(destination)
        if (entering) raids.engage(raid)
    }

    private fun ProtectedAccess.advance() {
        val raid = raids.containing(player) ?: return
        if (!raid.roomCleared) {
            mes("You can't move on until this room has been cleared.")
            return
        }
        if (raid.room.exit == null) return
        raids.advance(raid)
    }

    private fun ProtectedAccess.leaveRaid() {
        val raid = raids.containing(player) ?: return
        raids.leave(player, raid, "You leave the Theatre of Blood.")
    }

    private fun across(loc: BoundLocInfo, from: CoordGrid): CoordGrid {
        val sw = loc.coords
        val dx = abs(from.x - sw.x)
        val dz = abs(from.z - sw.z)
        return if (dz > dx) {
            val z = if (from.z < sw.z) sw.z + loc.adjustedLength else sw.z - 1
            CoordGrid(from.x, z, sw.level)
        } else {
            val x = if (from.x < sw.x) sw.x + loc.adjustedWidth else sw.x - 1
            CoordGrid(x, from.z, sw.level)
        }
    }

    private fun distance(a: CoordGrid, b: CoordGrid): Int = abs(a.x - b.x) + abs(a.z - b.z)
}
