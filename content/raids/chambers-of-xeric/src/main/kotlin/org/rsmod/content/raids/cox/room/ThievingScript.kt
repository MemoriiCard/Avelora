package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ThievingScript
@Inject
constructor(private val raids: CoxRaids, private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ThievingRoom.CHEST_CLOSED) { open(it.loc) }
        onOpLoc1(ThievingRoom.TROUGH_EMPTY) { deposit(it.loc) }
        onOpLoc1(ThievingRoom.TROUGH_FULL) { deposit(it.loc) }
    }

    private suspend fun ProtectedAccess.open(chest: BoundLocInfo) {
        val room = roomFor(chest) ?: return
        if (!room.isChest(chest.coords) || room.cleared) return
        anim(PICK_SEQ)
        delay(PICK_DELAY)
        val lockpick = inv.count(LOCKPICK) > 0
        if (random.of(1, 1000) > openChance(player.thievingLvl, lockpick)) {
            mes("You fail to pick the lock.")
            return
        }
        locRepo.change(chest, ThievingRoom.CHEST_OPEN, RELOCK_TICKS)
        when (room.loot(player, chest.coords)) {
            ThievingRoom.Loot.Grubs -> {
                val grubs = random.of(1, maxGrubs(player.thievingLvl))
                invAdd(inv, ThievingRoom.GRUBS, count = grubs, strict = false)
                mes("You find some cavern grubs in the chest.")
            }
            ThievingRoom.Loot.Poison -> mes("A cloud of poison gas bursts out of the chest!")
            ThievingRoom.Loot.Bat -> {
                invAdd(inv, ThievingRoom.BAT, strict = false)
                mes("A psykk bat flutters out of the chest and into your hands.")
            }
            ThievingRoom.Loot.Empty -> mes("The chest is empty.")
        }
    }

    private fun ProtectedAccess.deposit(trough: BoundLocInfo) {
        val room = roomFor(trough) ?: return
        if (!room.isTrough(trough.coords)) return
        if (room.cleared) {
            mes("The corrupted scavenger has eaten its fill.")
            return
        }
        val grubs = inv.count(ThievingRoom.GRUBS)
        if (grubs == 0) {
            mes("You have no grubs to put in the trough.")
            return
        }
        val eaten = room.feed(player, grubs)
        if (eaten > 0) invDel(inv, ThievingRoom.GRUBS, count = eaten, strict = false)
    }

    private fun ProtectedAccess.roomFor(loc: BoundLocInfo): ThievingRoom? {
        val raid = raids.containing(player) ?: return null
        return raid.roomAt(loc.coords)?.let(raid::controllerOf) as? ThievingRoom
    }

    companion object {
        private const val LOCKPICK = "obj.lockpick"
        private const val PICK_SEQ = "seq.human_pickpocket"
        private const val PICK_DELAY = 2
        private const val RELOCK_TICKS = 10

        /** Chance out of 1000, from 39.2% (level 1, no lockpick) to 82.3% (level 99, lockpick). */
        fun openChance(level: Int, lockpick: Boolean): Int {
            val low = if (lockpick) 607 else 392
            val high = if (lockpick) 823 else 607
            return low + (level.coerceIn(1, 99) - 1) * (high - low) / 98
        }

        fun maxGrubs(level: Int): Int = 1 + listOf(50, 75, 100).count { level >= it }
    }
}
