package org.rsmod.content.raids.cox.room

import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

/**
 * A corrupted scavenger blocks the way on and won't move until it's been fed. Players crack the
 * room's chests for cavern grubs and tip them into the trough. A few chests hold poison gas instead,
 * and one holds a nest of psykk bats.
 */
class ThievingRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private val chests = mutableMapOf<CoordGrid, Contents>()
    private val batsTaken = mutableSetOf<Player>()
    private var trough: LocInfo? = null
    private lateinit var scavenger: Npc
    var fed = 0
        private set

    var grubsNeeded = 0
        private set

    override fun spawn() {
        grubsNeeded = grubsNeeded(raid.scaling.partySize)
        val chestLocs = findLocs(CHEST_CLOSED).shuffled()
        for ((index, loc) in chestLocs.withIndex()) {
            chests[loc.coords] =
                when {
                    index < POISON_CHESTS -> Contents.Poison
                    index == POISON_CHESTS -> Contents.Bats
                    else -> Contents.Grubs
                }
        }
        trough = findLocs(TROUGH_EMPTY).firstOrNull()
        val near = trough?.coords?.translate(0, 2) ?: local(CENTRE, CENTRE)
        scavenger = spawnAt(SCAVENGER, standTile(near, SCAVENGER_SIZE), stats = null, required = false)
        scavenger.movementLocked = true
        scavenger.ignoreCombatInteractions = true
    }

    override fun onCleared() {
        if (scavenger.isSlotAssigned) {
            transmog(scavenger, SLEEPING)
            scavenger.anim(SLEEP_SEQ)
        }
        val n = raid.scaling.partySize
        rewardAll("obj.raids_stinkhorn_mushroom", 2 * n + 3)
        rewardAll("obj.raids_endarkened_juice", 2 * n + 3)
        rewardAll("obj.raids_cicely", n + 2)
    }

    fun isChest(coords: CoordGrid): Boolean = coords in chests

    fun isTrough(coords: CoordGrid): Boolean = trough?.coords == coords

    /** What [player] finds in the chest at [coords] after picking it open. */
    fun loot(player: Player, coords: CoordGrid): Loot =
        when (chests[coords]) {
            Contents.Poison -> {
                for (target in playersInRoom()) {
                    if (target.coords.chebyshevDistance(coords) > 1) continue
                    val damage = services.random.of(POISON_MIN, POISON_MAX)
                    target.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
                }
                Loot.Poison
            }
            Contents.Bats ->
                if (batsTaken.add(player)) Loot.Bat else Loot.Empty
            Contents.Grubs -> Loot.Grubs
            null -> Loot.Empty
        }

    /** Feeds [grubs] to the scavenger; returns how many it actually ate. */
    fun feed(player: Player, grubs: Int): Int {
        if (cleared) return 0
        val eaten = minOf(grubs, grubsNeeded - fed)
        if (eaten <= 0) return 0
        fed += eaten
        award(player, eaten * POINTS_PER_GRUB)
        scavenger.anim(EAT_SEQ)
        trough?.let { services.boss.locRepo.add(it.coords, TROUGH_FULL, Int.MAX_VALUE, it.angle, it.shape) }
        for (member in playersInRoom()) member.mes("The corrupted scavenger has eaten $fed of $grubsNeeded grubs.")
        if (fed >= grubsNeeded) clear()
        return eaten
    }

    private enum class Contents {
        Grubs,
        Poison,
        Bats,
    }

    enum class Loot {
        Grubs,
        Poison,
        Bat,
        Empty,
    }

    companion object {
        const val CHEST_CLOSED = "loc.raids_thievingchest_closed"
        const val CHEST_OPEN = "loc.raids_thievingchest_open"
        const val TROUGH_EMPTY = "loc.raids_thievingchest_foodtrough_empty"
        const val TROUGH_FULL = "loc.raids_thievingchest_foodtrough_full"
        const val GRUBS = "obj.raids_thievingchest_grubs"
        const val BAT = "obj.raids_bat6_raw"
        private const val SCAVENGER = "npc.raids_thievingchest_beast_active"
        private const val SLEEPING = "npc.raids_thievingchest_beast_sleeping"
        private const val EAT_SEQ = "seq.raids_thievingchests_eat"
        private const val SLEEP_SEQ = "seq.raids_thievingchests_sleeping"

        fun grubsNeeded(partySize: Int): Int = if (partySize <= 1) SOLO_GRUBS else GRUBS_PER_PLAYER * partySize

        private const val SOLO_GRUBS = 30
        private const val GRUBS_PER_PLAYER = 16
        private const val POINTS_PER_GRUB = 115
        private const val POISON_CHESTS = 3
        private const val POISON_MIN = 1
        private const val POISON_MAX = 3
        private const val SCAVENGER_SIZE = 2
        private const val CENTRE = 15
    }
}
