package org.rsmod.content.raids.cox.room

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid

/** A room holding a party-size dependent group of the same monster, spread round its middle. */
abstract class GroupRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    protected abstract val types: List<String>
    protected abstract val stats: CoxNpcStats
    protected abstract val ranged: Boolean

    protected abstract fun count(partySize: Int): Int

    override fun spawn() {
        val count = count(raid.scaling.partySize)
        repeat(count) { index ->
            val angle = 2 * Math.PI * index / count
            val x = CENTRE + (RING_RADIUS * cos(angle)).roundToInt()
            val z = CENTRE + (RING_RADIUS * sin(angle)).roundToInt()
            spawnNpc(types[index % types.size], x, z, stats, ranged = ranged)
        }
    }

    private companion object {
        const val CENTRE = 15
        const val RING_RADIUS = 5.0
    }
}

class ShamansRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    GroupRoom(raid, room, services) {
    override val types = TYPES
    override val stats = STATS
    override val ranged = false

    override fun count(partySize: Int): Int = countFor(partySize)

    companion object {
        fun countFor(partySize: Int): Int = (2 + partySize / 5).coerceAtMost(5)

        val TYPES = listOf("npc.raids_lizardshaman_a", "npc.raids_lizardshaman_b")
        val STATS =
            CoxNpcStats(
                hitpoints = 190,
                attack = 130,
                strength = 130,
                defence = 210,
                ranged = 130,
                magic = 130,
            )
    }
}

class MysticsRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    GroupRoom(raid, room, services) {
    override val types = TYPES
    override val stats = STATS
    override val ranged = true

    override fun count(partySize: Int): Int = countFor(partySize)

    companion object {
        fun countFor(partySize: Int): Int = (3 + partySize / 3).coerceAtMost(12)

        val TYPES =
            listOf(
                "npc.raids_skeletonmystic_a",
                "npc.raids_skeletonmystic_b",
                "npc.raids_skeletonmystic_c",
            )
        val STATS =
            CoxNpcStats(hitpoints = 160, attack = 140, strength = 140, defence = 187, magic = 140)
    }
}
