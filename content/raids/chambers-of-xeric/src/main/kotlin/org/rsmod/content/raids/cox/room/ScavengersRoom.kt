package org.rsmod.content.raids.cox.room

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid

/**
 * Scavenger beasts roam the resource rooms between combat rooms. They never block the way on, and
 * each kill rolls their table twice for tools, lockpicks, cave worms, planks and potion
 * secondaries.
 */
class ScavengersRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    GroupRoom(raid, room, services) {
    override val types = TYPES
    override val stats = STATS
    override val ranged = false
    override val blocksExit: Boolean = false

    override fun count(partySize: Int): Int = countFor(partySize)

    override fun onKilled(npc: Npc, hero: Player, dropCoords: CoordGrid) {
        repeat(ROLLS) {
            for ((obj, count) in ScavengerDrops.roll(services.random)) {
                services.objRepo.add(Obj.fromOwner(hero, dropCoords, obj, count), DROP_DURATION)
                val name = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj
                for (member in raid.insiders) member.mes("${hero.displayName} received a drop: $count x $name")
            }
        }
    }

    companion object {
        fun countFor(partySize: Int): Int = (3 + partySize / 4).coerceAtMost(8)

        val TYPES = listOf("npc.raids_scavenger_beast_a", "npc.raids_scavenger_beast_b")
        val STATS =
            CoxNpcStats(hitpoints = 30, attack = 120, strength = 120, defence = 45, single = true)

        private const val ROLLS = 2
        private const val DROP_DURATION = 200
    }
}
