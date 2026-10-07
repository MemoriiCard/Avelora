package org.rsmod.content.raids.cox.room

import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.clearInteractionRoute
import org.rsmod.api.npc.heal
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.route.walkTo
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

/**
 * Three vanguards rise together. If one falls too far behind the others in health they all heal
 * back to full, and every 20 to 36 ticks they shuffle round their triangle, untargetable while
 * they move.
 */
class VanguardsRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private val slots = mutableListOf<CoordGrid>()
    private val vanguards = mutableListOf<Npc>()
    private var nextShuffle = 0
    private var shuffleEnds = -1

    override fun spawn() {
        for ((index, type) in TYPES.withIndex()) {
            val (x, z) = SLOTS[index]
            val npc = spawnNpc(type, x, z, STATS, ranged = type != MELEE)
            slots += npc.coords
            vanguards += npc
            dormant(npc)
        }
    }

    override fun onEngage(first: Player) {
        for (npc in vanguards) {
            npc.resetTransmog()
            npc.showAllOps()
            npc.ignoreCombatInteractions = false
            npc.anim("seq.vanguard_spawn")
        }
        nextShuffle = services.cycle + shuffleDelay()
    }

    override fun onTick() {
        val alive = vanguards.filter { it.isSlotAssigned && it.hitpoints > 0 }
        if (alive.isEmpty()) return
        if (shuffleEnds >= 0) {
            if (services.cycle >= shuffleEnds) finishShuffle(alive)
            return
        }
        checkHealthSync(alive)
        if (services.cycle >= nextShuffle && alive.size > 1) shuffle(alive)
    }

    private fun checkHealthSync(alive: List<Npc>) {
        if (alive.size < 2) return
        val percents = alive.map { it.hitpoints * 100 / it.baseHitpointsLvl.coerceAtLeast(1) }
        val gap = percents.max() - percents.min()
        val threshold = if (raid.scaling.partySize >= LARGE_PARTY) LARGE_PARTY_GAP else SMALL_PARTY_GAP
        if (gap <= threshold) return
        for (npc in alive) {
            npc.heal(npc.baseHitpointsLvl, showHitsplat = true)
            npc.anim("seq.vanguard_heal")
        }
    }

    private fun shuffle(alive: List<Npc>) {
        val clockwise = services.random.of(0, 1) == 0
        shuffleEnds = services.cycle + SHUFFLE_TICKS
        for (npc in alive) {
            val index = vanguards.indexOf(npc)
            val next = slots[Math.floorMod(index + if (clockwise) 1 else -1, slots.size)]
            services.boss.encounter(npc).invulnerable = true
            npc.clearInteractionRoute()
            npc.ignoreCombatInteractions = true
            npc.hideAllOps()
            npc.walkTo(services.routes, next)
        }
        for (player in playersInRoom()) {
            if (alive.any { it.coords.chebyshevDistance(player.coords) <= 2 }) {
                val damage = services.random.of(TRAMPLE_MIN, TRAMPLE_MAX)
                player.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
            }
        }
        val rotated = if (clockwise) listOf(vanguards.last()) + vanguards.dropLast(1)
            else vanguards.drop(1) + vanguards.first()
        vanguards.clear()
        vanguards += rotated
    }

    private fun finishShuffle(alive: List<Npc>) {
        shuffleEnds = -1
        nextShuffle = services.cycle + shuffleDelay()
        for (npc in alive) {
            services.boss.encounter(npc).invulnerable = false
            npc.ignoreCombatInteractions = false
            npc.showAllOps()
            npc.attackLvl = npc.baseAttackLvl
            npc.strengthLvl = npc.baseStrengthLvl
            npc.defenceLvl = npc.baseDefenceLvl
            npc.rangedLvl = npc.baseRangedLvl
            npc.magicLvl = npc.baseMagicLvl
            nearestPlayer(npc)?.let { engage(npc, it) }
        }
    }

    private fun dormant(npc: Npc) {
        transmog(npc, DORMANT)
        npc.hideAllOps()
        npc.ignoreCombatInteractions = true
    }

    private fun shuffleDelay(): Int = services.random.of(SHUFFLE_MIN, SHUFFLE_MAX)

    companion object {
        const val MELEE = "npc.raids_vanguard_melee"
        const val RANGED = "npc.raids_vanguard_ranged"
        const val MAGIC = "npc.raids_vanguard_magic"
        private const val DORMANT = "npc.raids_vanguard_dormant"
        val TYPES = listOf(MELEE, RANGED, MAGIC)

        private val SLOTS = listOf(15 to 20, 10 to 12, 20 to 12)

        val STATS =
            CoxNpcStats(
                hitpoints = 180,
                attack = 150,
                strength = 150,
                defence = 160,
                ranged = 150,
                magic = 150,
            )

        private const val SHUFFLE_MIN = 20
        private const val SHUFFLE_MAX = 36
        private const val SHUFFLE_TICKS = 4
        private const val TRAMPLE_MIN = 3
        private const val TRAMPLE_MAX = 6
        private const val LARGE_PARTY = 5
        private const val SMALL_PARTY_GAP = 40
        private const val LARGE_PARTY_GAP = 33
    }
}
