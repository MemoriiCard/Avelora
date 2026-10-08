package org.rsmod.content.raids.cox.room

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Deathly rangers and mages watch over a tightrope. They ignore anyone who stays put, but the
 * moment a player crosses, all of them turn on the party. A keystone crystal on the far side
 * dispels the barrier at the exit and kills every deathly creature that is still standing.
 */
class TightropeRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private val ends = mutableListOf<CoordGrid>()
    private val deathly = mutableListOf<Npc>()
    private var aroused = false

    var agilityRequired = 1
        private set

    override fun spawn() {
        ends += findLocs(ROPE_END).map { it.coords }
        val average = raid.party.members.map { it.agilityLvl }.average().toInt()
        agilityRequired = (average * services.random.of(MIN_PERCENT, MAX_PERCENT) / 100).coerceAtLeast(1)

        val middle =
            if (ends.size >= 2) CoordGrid((ends[0].x + ends[1].x) / 2, (ends[0].z + ends[1].z) / 2, ends[0].level)
            else local(CENTRE, CENTRE)
        val count = deathlyCount(raid.scaling.partySize)
        repeat(count) {
            deathly += watcher(RANGER, RANGER_STATS, middle)
            deathly += watcher(MAGE, MAGE_STATS, middle)
        }
    }

    override fun tryUnblock(player: Player): Boolean {
        if (!player.hasKeystone()) return false
        player.consumeKeystone()
        for (npc in deathly) {
            if (npc.isSlotAssigned) services.npcRepo.del(npc, Int.MAX_VALUE)
        }
        deathly.clear()
        award(player, KEYSTONE_POINTS)
        for (member in playersInRoom()) member.mes("The keystone dispels the barrier and the deathly creatures crumble.")
        clear()
        return true
    }

    /** Where crossing from [from] lands, or null when [from] isn't a rope end of this room. */
    fun crossingFrom(from: CoordGrid): CoordGrid? {
        val start = ends.firstOrNull { it == from } ?: return null
        val far = ends.filter { it != start }.minByOrNull { it.chebyshevDistance(start) } ?: return null
        val dx = (far.x - start.x).coerceIn(-1, 1)
        val dz = (far.z - start.z).coerceIn(-1, 1)
        return far.translate(dx, dz)
    }

    fun distance(from: CoordGrid, to: CoordGrid): Int = from.chebyshevDistance(to)

    fun arouse(target: Player) {
        if (cleared) return
        aroused = true
        for (npc in deathly) {
            if (!npc.isSlotAssigned) continue
            npc.ignoreCombatInteractions = false
            engage(npc, target)
        }
    }

    private fun watcher(type: String, stats: CoxNpcStats, middle: CoordGrid): Npc {
        val dx = services.random.of(-SPREAD, SPREAD)
        val dz = services.random.of(-SPREAD, SPREAD)
        val npc = spawnAt(type, standTile(middle.translate(dx, dz), 1), stats, required = false, ranged = true)
        npc.ignoreCombatInteractions = true
        return npc
    }

    private fun Player.hasKeystone(): Boolean = inv.any { it != null && it.id == KEYSTONE_ID }

    private fun Player.consumeKeystone() {
        val slot = inv.indexOfFirst { it != null && it.id == KEYSTONE_ID }
        if (slot >= 0) inv[slot] = null
    }

    companion object {
        const val ROPE_END = "loc.raids_tightrope_end"
        const val KEYSTONE_LOC = "loc.raids_tightrope_keystone_loc"
        const val KEYSTONE = "obj.raids_tightrope_keystone"
        const val RANGER = "npc.raids_tightrope_ranger"
        const val MAGE = "npc.raids_tightrope_mage"

        val RANGER_STATS =
            CoxNpcStats(
                hitpoints = 120,
                ranged = 210,
                defence = 155,
                magic = 155,
                defensiveMagic = true,
            )
        val MAGE_STATS =
            CoxNpcStats(hitpoints = 120, magic = 210, defence = 155)

        fun deathlyCount(partySize: Int): Int = (2 + partySize / 8).coerceAtMost(4)

        private val KEYSTONE_ID: Int by lazy { KEYSTONE.asRSCM(RSCMType.OBJ) }
        private const val KEYSTONE_POINTS = 150
        private const val MIN_PERCENT = 80
        private const val MAX_PERCENT = 100
        private const val SPREAD = 5
        private const val CENTRE = 15
    }
}
