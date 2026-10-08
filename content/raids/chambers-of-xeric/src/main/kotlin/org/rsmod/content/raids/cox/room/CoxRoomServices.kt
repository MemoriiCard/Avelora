package org.rsmod.content.raids.cox.room

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

@Singleton
class CoxRoomServices
@Inject
constructor(
    val boss: BossDeps,
    val routes: RouteFactory,
    val ai: AiPlayerInteractions,
    val npcHitModifier: NpcHitModifier,
    val objRepo: ObjRepository,
) {
    val npcRepo
        get() = boss.npcRepo

    val collision
        get() = boss.collision

    val random
        get() = boss.random

    val players
        get() = boss.playerList

    val worldRepo
        get() = boss.worldRepo

    val cycle: Int
        get() = boss.mapClock.cycle

    /** Rolls [npc]'s accuracy and party-scaled max hit against [target] and queues the hit. */
    fun strike(
        npc: Npc,
        target: Player,
        style: HitType,
        delay: Int = 1,
        meleeType: MeleeAttackType? = null,
        scale: Double = 1.0,
    ) {
        val landed =
            when (style) {
                HitType.Melee -> boss.accuracy.rollMeleeAccuracy(npc, target, meleeType, random)
                HitType.Ranged -> boss.accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> boss.accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        val max =
            when (style) {
                HitType.Ranged -> boss.maxHit.getRangedMaxHit(npc, target)
                HitType.Magic -> boss.maxHit.getMagicMaxHit(npc, target)
                else -> boss.maxHit.getMeleeMaxHit(npc, target, meleeType)
            }
        val damage = if (landed) random.of(0, (max * scale).toInt().coerceAtLeast(0)) else 0
        target.queueHit(npc, delay, style, damage, boss.playerHitModifier)
    }

    fun spotanimAt(spotanim: String, tile: CoordGrid, height: Int = 0, delay: Int = 0) {
        worldRepo.spotanimMap(SpotanimType(spotanim.asRSCM(RSCMType.SPOTANIM)), tile, height, delay)
    }

    /** Lobs a projectile from [from] to [tile] and runs [onLand] once it lands. */
    fun lob(spotanim: String, from: CoordGrid, tile: CoordGrid, onLand: () -> Unit) {
        val distance = from.chebyshevDistance(tile)
        val travel = LOB_BASE_CYCLES + distance * LOB_CYCLES_PER_TILE
        boss.bossProjectile(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            src = from,
            target = tile,
            startHeight = 80,
            endHeight = 0,
            delay = 0,
            travel = travel,
            curve = 30,
        )
        boss.worldQueues.add((travel + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK) { onLand() }
    }

    private companion object {
        const val LOB_BASE_CYCLES = 30
        const val LOB_CYCLES_PER_TILE = 5
        const val CYCLES_PER_TICK = 30
    }
}
