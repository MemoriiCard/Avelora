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
 * Tekton fights until he has swung 10 to 14 times (or nobody stays in melee reach), walks back to
 * his anvil to heal while sparks rain on the party, then returns enraged for a few attacks.
 */
class TektonRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private lateinit var tekton: Npc
    private var anvilTile: CoordGrid = CoordGrid.NULL
    private var state = State.Waiting
    private var stateTicks = 0
    private var attacks = 0
    private var attackLimit = 0
    private var lastAttackTick = 0
    private var outOfReachTicks = 0
    private var sparkSets = 0

    override fun spawn() {
        val (ax, az) = pick(14 to 22, 7 to 13, 12 to 22)
        tekton = spawnNpc(TEKTON, ax, az, STATS)
        anvilTile = tekton.coords
        transmog(tekton, WAITING)
        tekton.ignoreCombatInteractions = true
    }

    override fun onEngage(first: Player) {
        tekton.ignoreCombatInteractions = false
        startFighting(enraged = false)
    }

    override fun onTick() {
        if (!tekton.isSlotAssigned || tekton.hitpoints <= 0) return
        stateTicks++
        when (state) {
            State.Waiting -> Unit
            State.Fighting,
            State.Enraged -> fightTick()
            State.Returning -> stompUnderneath()
            State.Anvil -> anvilTick()
        }
    }

    private fun fightTick() {
        val encounter = services.boss.encounter(tekton)
        if (encounter.lastAbilityTick != lastAttackTick && encounter.lastAbilityTick > 0) {
            lastAttackTick = encounter.lastAbilityTick
            attacks++
        }
        val anyoneInReach = playersInRoom().any { inMeleeReach(it) }
        outOfReachTicks = if (anyoneInReach) 0 else outOfReachTicks + 1
        if (state == State.Enraged && attacks >= ENRAGED_ATTACKS) {
            startFighting(enraged = false)
            return
        }
        if (state == State.Fighting && (attacks >= attackLimit || outOfReachTicks >= SCAN_TICKS)) {
            returnToAnvil()
        }
    }

    private fun startFighting(enraged: Boolean) {
        state = if (enraged) State.Enraged else State.Fighting
        stateTicks = 0
        attacks = 0
        outOfReachTicks = 0
        attackLimit = 10 + services.random.of(0, 4)
        transmog(tekton, if (enraged) FIGHTING_ENRAGED else FIGHTING)
        services.boss.encounter(tekton).transitionTo(if (enraged) "enraged" else "standard", services.cycle)
        tekton.ignoreCombatInteractions = false
        nearestPlayer(tekton)?.let { engage(tekton, it) }
    }

    private fun returnToAnvil() {
        state = State.Returning
        stateTicks = 0
        tekton.ignoreCombatInteractions = true
        tekton.clearInteractionRoute()
        transmog(tekton, WALKING)
        tekton.walkTo(services.routes, anvilTile) { arriveAtAnvil() }
    }

    private fun arriveAtAnvil() {
        if (state != State.Returning) return
        state = State.Anvil
        stateTicks = 0
        sparkSets = 0
        transmog(tekton, HAMMERING)
    }

    private fun anvilTick() {
        if (stateTicks % ANVIL_PULSE != 0) return
        val heal = maxOf(1, tekton.baseHitpointsLvl * ANVIL_HEAL_PERMILLE / 1000)
        tekton.heal(heal, showHitsplat = true)
        if (sparkSets < MAX_SPARK_SETS) {
            sparkSets++
            for (player in playersInRoom()) {
                repeat(SPARKS_PER_PLAYER) { throwSpark(player.coords) }
            }
            return
        }
        startFighting(enraged = true)
    }

    private fun throwSpark(near: CoordGrid) {
        val tile = near.translate(services.random.of(-1, 1), services.random.of(-1, 1))
        services.lob(SPARK_TRAVEL, tekton.coords.translate(1, 1), tile) {
            services.spotanimAt(SPARK_IMPACT, tile)
            for (player in playersInRoom()) {
                if (player.coords.chebyshevDistance(tile) <= 1) {
                    val damage = services.random.of(SPARK_MIN, SPARK_MAX)
                    player.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
                }
            }
        }
    }

    private fun stompUnderneath() {
        for (player in playersInRoom()) {
            if (underneath(player)) {
                val damage = services.random.of(STOMP_MIN, STOMP_MAX)
                player.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
            }
        }
    }

    private fun inMeleeReach(player: Player): Boolean {
        val size = tekton.type.size
        val dx = player.coords.x - tekton.coords.x
        val dz = player.coords.z - tekton.coords.z
        return player.coords.level == tekton.coords.level &&
            dx in -1..size &&
            dz in -1..size &&
            !(dx in 0 until size && dz in 0 until size)
    }

    private fun underneath(player: Player): Boolean {
        val size = tekton.type.size
        return player.coords.level == tekton.coords.level &&
            player.coords.x - tekton.coords.x in 0 until size &&
            player.coords.z - tekton.coords.z in 0 until size
    }

    private enum class State {
        Waiting,
        Fighting,
        Returning,
        Anvil,
        Enraged,
    }

    companion object {
        const val TEKTON = "npc.raids_tekton_fighting_standard"
        const val FIGHTING = "npc.raids_tekton_fighting_standard"
        const val FIGHTING_ENRAGED = "npc.raids_tekton_fighting_enraged"
        const val WALKING = "npc.raids_tekton_walking_standard"
        const val WAITING = "npc.raids_tekton_waiting"
        const val HAMMERING = "npc.raids_tekton_hammering"

        val TYPES =
            listOf(
                TEKTON,
                "npc.raids_tekton_waiting",
                "npc.raids_tekton_walking_standard",
                "npc.raids_tekton_walking_enraged",
                "npc.raids_tekton_fighting_enraged",
                "npc.raids_tekton_hammering",
            )

        val STATS =
            CoxNpcStats(
                hitpoints = 300,
                attack = 390,
                strength = 390,
                defence = 205,
                magic = 205,
                defensiveMagic = true,
                tekton = true,
            )

        private const val SPARK_TRAVEL = "spotanim.wild_falloff_meteor_flying"
        private const val SPARK_IMPACT = "spotanim.wild_falloff_meteor_blast"
        private const val SPARK_MIN = 10
        private const val SPARK_MAX = 20
        private const val SPARKS_PER_PLAYER = 2
        private const val MAX_SPARK_SETS = 5
        private const val ANVIL_PULSE = 4
        private const val ANVIL_HEAL_PERMILLE = 13
        private const val SCAN_TICKS = 8
        private const val ENRAGED_ATTACKS = 5
        private const val STOMP_MIN = 2
        private const val STOMP_MAX = 8
    }
}
