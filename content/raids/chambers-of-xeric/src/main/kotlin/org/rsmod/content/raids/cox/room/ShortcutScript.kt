package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.strengthLvl
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The large scavenger ruins hide a shortcut: a boulder, a pile of rocks or a rotten sapling. It
 * takes the party's average level in the matching skill, give or take two, to clear it.
 */
class ShortcutScript
@Inject
constructor(private val raids: CoxRaids, private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(BOULDER) { push(it.loc) }
        onOpLoc1(ROCKS) { mine(it.loc) }
        onOpLoc1(ROOTS) { chop(it.loc) }
        onOpLoc1(ROCKS_CLEARED) { cross(it.loc) }
        onOpLoc1(ROOTS_CLEARED) { cross(it.loc) }
    }

    private suspend fun ProtectedAccess.push(boulder: BoundLocInfo) {
        val (room, shortcut) = shortcutAt(boulder, ScavengersRoom.Shortcut.Kind.Boulder) ?: return
        if (!meets(shortcut, player.strengthLvl)) return
        anim(PUSH_SEQ)
        delay(PUSH_DELAY)
        resetAnim()
        locRepo.del(boulder, Int.MAX_VALUE)
        room.openShortcut(player, shortcut)
        mes("You heave the boulder out of the way.")
    }

    private suspend fun ProtectedAccess.mine(rocks: BoundLocInfo) {
        val (room, shortcut) = shortcutAt(rocks, ScavengersRoom.Shortcut.Kind.Rocks) ?: return
        if (!meets(shortcut, player.miningLvl)) return
        if (!carries(PICKAXE)) {
            mes("You need a pickaxe to clear these rocks.")
            return
        }
        anim(MINE_SEQ)
        delay(MINE_DELAY)
        resetAnim()
        locRepo.change(rocks, ROCKS_CLEARED, Int.MAX_VALUE)
        room.openShortcut(player, shortcut)
        mes("You clear a path through the rocks.")
    }

    private suspend fun ProtectedAccess.chop(sapling: BoundLocInfo) {
        val (room, shortcut) = shortcutAt(sapling, ScavengersRoom.Shortcut.Kind.Roots) ?: return
        if (!meets(shortcut, player.woodcuttingLvl)) return
        if (!carries(AXE)) {
            mes("You need an axe to chop down this sapling.")
            return
        }
        anim(CHOP_SEQ)
        delay(CHOP_DELAY)
        resetAnim()
        locRepo.change(sapling, ROOTS_CLEARED, Int.MAX_VALUE)
        room.openShortcut(player, shortcut)
        mes("You chop the rotten sapling down.")
    }

    private suspend fun ProtectedAccess.cross(obstacle: BoundLocInfo) {
        val dx = (obstacle.coords.x - player.coords.x).coerceIn(-1, 1)
        val dz = (obstacle.coords.z - player.coords.z).coerceIn(-1, 1)
        anim(CROSS_SEQ)
        delay(CROSS_DELAY)
        resetAnim()
        telejump(obstacle.coords.translate(dx, dz))
    }

    private fun ProtectedAccess.shortcutAt(
        loc: BoundLocInfo,
        kind: ScavengersRoom.Shortcut.Kind,
    ): Pair<ScavengersRoom, ScavengersRoom.Shortcut>? {
        val raid = raids.containing(player) ?: return null
        val room = raid.roomAt(loc.coords)?.let(raid::controllerOf) as? ScavengersRoom ?: return null
        val shortcut = room.shortcut?.takeIf { it.kind == kind } ?: return null
        return room to shortcut
    }

    private fun ProtectedAccess.meets(shortcut: ScavengersRoom.Shortcut, level: Int): Boolean {
        if (level >= shortcut.required) return true
        mes("You need a ${shortcut.kind.skill} level of ${shortcut.required} to clear this.")
        return false
    }

    private fun ProtectedAccess.carries(content: String): Boolean {
        val worn = player.righthand?.let { getInvObj(it) }
        if (worn != null && worn.isContentType(content)) return true
        return inv.any { it != null && getInvObj(it).isContentType(content) }
    }

    private companion object {
        const val BOULDER = "loc.raids_corridor_boulder"
        const val ROCKS = "loc.raids_corridor_rocks"
        const val ROCKS_CLEARED = "loc.raids_corridor_rocks_cleared"
        const val ROOTS = "loc.raids_corridor_roots"
        const val ROOTS_CLEARED = "loc.raids_corridor_roots_cleared"
        const val PICKAXE = "content.mining_pickaxe"
        const val AXE = "content.woodcutting_axe"
        const val PUSH_SEQ = "seq.human_push"
        const val MINE_SEQ = "seq.human_mining_bronze_pickaxe"
        const val CHOP_SEQ = "seq.human_woodcutting_bronze_axe"
        const val CROSS_SEQ = "seq.human_pickupfloor"
        const val PUSH_DELAY = 3
        const val MINE_DELAY = 5
        const val CHOP_DELAY = 5
        const val CROSS_DELAY = 2
    }
}
