package org.rsmod.content.bosses.hydra

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.map.CoordGrid

internal class HydraFight(val player: Player, val session: InstanceSession) {
    var phase: HydraPhase = HydraPhase.Serpentine
    var weakened: Boolean = false
    var empowered: Boolean = false
    var transitioning: Boolean = false
    var untilSpecial: Int = FIRST_SPECIAL
    var styleRanged: Boolean = true
    var styleCount: Int = 0
    var over: Boolean = false

    companion object {
        const val FIRST_SPECIAL = 3
    }
}

@Singleton
internal class HydraFights
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val fights = IdentityHashMap<Npc, HydraFight>()

    fun fightFor(npc: Npc): HydraFight? = fights[npc]

    fun spawnHydra(session: InstanceSession, player: Player) {
        val coords = instances.resolveCoord(session, HydraLair.SPAWN) ?: return
        val type = ServerCacheManager.getNpc(HydraPhase.Serpentine.npc.asRSCM(RSCMType.NPC)) ?: return
        val hydra = Npc(type, coords)
        deps.npcRepo.add(hydra, Int.MAX_VALUE)
        hydra.respawns = false
        hydra.apRangeOverride = ATTACK_RANGE
        instances.attachNpc(session.id, hydra)
        val fight = HydraFight(player, session)
        fights[hydra] = fight
        later(hydra, fight, WAKE_TICKS) { if (player.hitpoints > 0) hydra.apPlayer2(player, aiPlayerInteractions) }
        scheduleVents(hydra, fight)
    }

    fun end(npc: Npc): HydraFight? {
        val fight = fights.remove(npc) ?: return null
        fight.over = true
        return fight
    }

    fun endFor(player: Player) {
        fights.entries.filter { it.value.player === player }.map { it.key }.forEach(::end)
    }

    fun respawnLater(fight: HydraFight) {
        deps.worldQueues.add(RESPAWN_TICKS) {
            if (instances.sessionForPlayer(fight.player) === fight.session) spawnHydra(fight.session, fight.player)
        }
    }

    fun onIncomingHit(hydra: Npc, hit: HitBuilder) {
        val fight = fights[hydra] ?: return
        if (fight.transitioning) {
            hit.damage = 0
            return
        }
        if (fight.phase != HydraPhase.Enraged && !fight.weakened) hit.damage = hit.damage * UNWEAKENED_PERCENT / 100
        val next = fight.phase.next ?: return
        val floor = hydra.baseHitpointsLvl * (HydraPhase.entries.size - next.ordinal) / HydraPhase.entries.size
        if (hydra.hitpoints - hit.damage <= floor) {
            hit.damage = (hydra.hitpoints - floor).coerceAtLeast(0)
            transition(hydra, fight, next)
        }
    }

    fun nextAction(hydra: Npc, target: Player) {
        val fight = fights[hydra] ?: return
        if (fight.over || fight.transitioning) return
        if (fight.untilSpecial <= 0) {
            fight.untilSpecial = SPECIAL_EVERY
            special(hydra, fight, target)
            return
        }
        fight.untilSpecial--
        val ability = standardAbility(fight.empowered, fight.styleRanged, fight.phase)
        deps.runAbility(hydra, target, ability)
        fight.styleCount++
        val switchAfter = if (fight.phase == HydraPhase.Enraged) 1 else STYLE_SWITCH
        if (fight.styleCount >= switchAfter) {
            fight.styleCount = 0
            fight.styleRanged = !fight.styleRanged
        }
        if (fight.phase == HydraPhase.Enraged) {
            val ready = deps.mapClock.cycle + ENRAGED_ATTACK_RATE
            deps.worldQueues.add(1) { deps.encounter(hydra).busyUntil = ready }
        }
    }

    private fun transition(hydra: Npc, fight: HydraFight, next: HydraPhase) {
        fight.transitioning = true
        val current = fight.phase
        deps.suppressAttacks(hydra, TRANSITION_TICKS + 1)
        current.transition?.let { morph(hydra, it) }
        hydra.anim(current.transitionSeq)
        later(hydra, fight, TRANSITION_TICKS) {
            fight.phase = next
            fight.weakened = false
            fight.empowered = false
            fight.transitioning = false
            fight.styleCount = 0
            fight.untilSpecial = if (next == HydraPhase.Enraged) 0 else HydraFight.FIRST_SPECIAL
            morph(hydra, next.npc)
            hydra.anim(next.spawnSeq)
            if (next == HydraPhase.Enraged) fight.player.mes("The Alchemical Hydra becomes enraged!")
        }
    }

    private fun special(hydra: Npc, fight: HydraFight, target: Player) {
        hydra.anim(fight.phase.specialSeq)
        when (fight.phase) {
            HydraPhase.Serpentine,
            HydraPhase.Enraged -> poison(hydra, fight, target)
            HydraPhase.Electric -> lightning(hydra, fight)
            HydraPhase.Flame -> flames(hydra, fight)
        }
    }

    private fun poison(hydra: Npc, fight: HydraFight, target: Player) {
        val centre = target.coords
        val tiles = (listOf(centre) + around(centre).shuffled().take(POISON_BLOBS - 1)).filter { inLair(fight, it) }
        for (tile in tiles) {
            lob(hydra, tile, POISON_ORB, POISON_LAND_TICKS)
            later(hydra, fight, POISON_LAND_TICKS) {
                spotanim(POISON_SPLASH, tile)
                if (fight.player.coords.chebyshevDistance(tile) <= 1) deps.runAbility(hydra, fight.player, POISON_ABILITY)
            }
            for (tick in 1..POOL_TICKS) {
                later(hydra, fight, POISON_LAND_TICKS + tick) {
                    if (tick % 2 == 1) spotanim(POOL, tile)
                    if (fight.player.coords == tile) deps.runAbility(hydra, fight.player, POOL_ABILITY)
                }
            }
        }
    }

    private fun lightning(hydra: Npc, fight: HydraFight) {
        spotanim(LIGHTNING_CASTER, hydra.coords.translate(HYDRA_SIZE / 2, HYDRA_SIZE / 2))
        for (corner in HydraLair.CORNERS) {
            val start = instances.resolveCoord(fight.session, corner) ?: continue
            chase(hydra, fight, start, LIGHTNING_TRAVEL, LIGHTNING_TICKS, LIGHTNING_ABILITY)
        }
    }

    private fun flames(hydra: Npc, fight: HydraFight) {
        val start = hydra.coords.translate(HYDRA_SIZE / 2, HYDRA_SIZE / 2)
        chase(hydra, fight, start, FIRE, FLAME_TICKS, FLAME_ABILITY)
    }

    private fun chase(hydra: Npc, fight: HydraFight, start: CoordGrid, spot: String, ticks: Int, ability: String) {
        var current = start
        var spent = false
        for (tick in 1..ticks) {
            later(hydra, fight, tick) {
                if (spent) return@later
                current = step(current, fight.player.coords)
                spotanim(spot, current)
                if (fight.player.coords == current && fight.player.hitpoints > 0) {
                    spent = true
                    deps.runAbility(hydra, fight.player, ability)
                }
            }
        }
    }

    private fun scheduleVents(hydra: Npc, fight: HydraFight) {
        later(hydra, fight, VENT_PERIOD) {
            if (!fight.transitioning) sprayVents(hydra, fight)
            scheduleVents(hydra, fight)
        }
    }

    private fun sprayVents(hydra: Npc, fight: HydraFight) {
        if (fight.phase == HydraPhase.Enraged) return
        for (vent in HydraVent.entries) {
            val tile = instances.resolveCoord(fight.session, vent.tile) ?: continue
            if (!overVent(hydra, tile)) continue
            if (vent == fight.phase.weakness) {
                if (!fight.weakened) fight.player.mes("The chemicals neutralise the Alchemical Hydra's defences!")
                fight.weakened = true
            } else if (!fight.empowered) {
                fight.empowered = true
                fight.player.mes("The chemicals are absorbed by the Alchemical Hydra; empowering it further!")
            }
        }
    }

    private fun overVent(hydra: Npc, vent: CoordGrid): Boolean {
        val x = vent.x.coerceIn(hydra.coords.x, hydra.coords.x + HYDRA_SIZE - 1)
        val z = vent.z.coerceIn(hydra.coords.z, hydra.coords.z + HYDRA_SIZE - 1)
        return vent.chebyshevDistance(CoordGrid(x, z, vent.level)) <= VENT_RADIUS
    }

    private fun inLair(fight: HydraFight, tile: CoordGrid): Boolean {
        val base = instances.resolveCoord(fight.session, HydraLair.CENTRE) ?: return false
        val x = tile.x - base.x + HydraLair.CENTRE.x
        val z = tile.z - base.z + HydraLair.CENTRE.z
        return x in HydraLair.MIN_X..HydraLair.MAX_X && z in HydraLair.MIN_Z..HydraLair.MAX_Z
    }

    private fun around(tile: CoordGrid): List<CoordGrid> =
        (-POISON_SPREAD..POISON_SPREAD).flatMap { dx ->
            (-POISON_SPREAD..POISON_SPREAD).mapNotNull { dz -> if (dx == 0 && dz == 0) null else tile.translate(dx, dz) }
        }

    private fun step(from: CoordGrid, to: CoordGrid): CoordGrid =
        from.translate((to.x - from.x).coerceIn(-1, 1), (to.z - from.z).coerceIn(-1, 1))

    private fun spotanim(name: String, tile: CoordGrid) {
        deps.worldRepo.spotanimMap(SpotanimType(name.asRSCM(RSCMType.SPOTANIM)), tile)
    }

    private fun lob(hydra: Npc, tile: CoordGrid, spotanim: String, landTicks: Int) {
        deps.bossProjectile(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            src = hydra.coords.translate(HYDRA_SIZE / 2, HYDRA_SIZE / 2),
            target = tile,
            startHeight = ORB_START_HEIGHT,
            endHeight = 0,
            delay = ORB_DELAY,
            travel = landTicks * CLIENT_CYCLES_PER_TICK - ORB_DELAY,
            curve = ORB_CURVE,
        )
    }

    private fun morph(npc: Npc, into: String) {
        val type = ServerCacheManager.getNpc(into.asRSCM(RSCMType.NPC)) ?: return
        if (type.id == npc.type.id) npc.resetTransmog() else npc.transmog(type, Int.MAX_VALUE)
    }

    private fun later(hydra: Npc, fight: HydraFight, ticks: Int, action: () -> Unit) {
        deps.worldQueues.add(ticks) { if (!fight.over && hydra.isSlotAssigned) action() }
    }

    private companion object {
        const val POISON_ORB = "spotanim.hydraboss_pools_proj"
        const val POISON_SPLASH = "spotanim.hydraboss_pools_splash"
        const val POOL = "spotanim.hydraboss_pools_landed_0"
        const val LIGHTNING_CASTER = "spotanim.hydraboss_lightning_caster"
        const val LIGHTNING_TRAVEL = "spotanim.hydraboss_shockwave"
        const val FIRE = "spotanim.hydraboss_fire"

        const val HYDRA_SIZE = 6
        const val ATTACK_RANGE = 10
        const val RESPAWN_TICKS = 43
        const val WAKE_TICKS = 2
        const val UNWEAKENED_PERCENT = 25
        const val STYLE_SWITCH = 3
        const val SPECIAL_EVERY = 9
        const val TRANSITION_TICKS = 4

        const val VENT_PERIOD = 8
        const val VENT_RADIUS = 1

        const val POISON_BLOBS = 5
        const val POISON_SPREAD = 2
        const val POISON_LAND_TICKS = 3
        const val POOL_TICKS = 10
        const val LIGHTNING_TICKS = 12
        const val FLAME_TICKS = 10

        const val ORB_START_HEIGHT = 90
        const val ORB_DELAY = 20
        const val ORB_CURVE = 30
        const val CLIENT_CYCLES_PER_TICK = 30
    }
}
