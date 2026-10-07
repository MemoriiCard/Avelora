package org.rsmod.content.bosses.abyssalsire

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.EnumMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.clearOwnedNpcs
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitBuilder
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal enum class SirePhase {
    Asleep,
    Stasis,
    Stunned,
    Combat,
    Centre,
}

internal class SireFight(val chamber: SireChamber, val sire: Npc) {
    var phase: SirePhase = SirePhase.Asleep
    var stunDamage: Int = 0
    var stunId: Int = 0
    var exploded: Boolean = false
    var over: Boolean = false
}

@Singleton
internal class SireFights
@Inject
constructor(private val deps: BossDeps, private val aiPlayerInteractions: AiPlayerInteractions) {
    private val fights = EnumMap<SireChamber, SireFight>(SireChamber::class.java)

    fun fightFor(sire: Npc): SireFight? = fights.values.firstOrNull { it.sire === sire }

    fun onSireHit(sire: Npc, hit: HitBuilder) {
        val chamber = SireChamber.containing(sire.coords) ?: return
        val attacker = attacker(hit) ?: return
        val fight = fights[chamber] ?: start(chamber, sire) ?: return
        when (fight.phase) {
            SirePhase.Asleep -> {
                wake(fight, attacker)
                addStunDamage(fight, hit.damage)
                keepAlive(sire, hit)
            }
            SirePhase.Stasis -> {
                addStunDamage(fight, hit.damage)
                keepAlive(sire, hit)
            }
            SirePhase.Stunned -> keepAlive(sire, hit)
            SirePhase.Combat ->
                if (sire.hitpoints - hit.damage <= sire.baseHitpointsLvl * CENTRE_PERCENT / 100) moveToCentre(fight)
            SirePhase.Centre ->
                if (!fight.exploded && sire.hitpoints - hit.damage < sire.baseHitpointsLvl * EXPLODE_PERCENT / 100) {
                    explode(fight, attacker)
                }
        }
    }

    fun onLungDeath(lung: Npc) {
        val chamber = SireChamber.containing(lung.coords) ?: return
        val fight = fights[chamber] ?: sireIn(chamber)?.let { start(chamber, it) } ?: return
        val alive = npcsIn(chamber, LUNG).count { it.visType.internalName == LUNG && it.hitpoints > 0 }
        if (alive == 0 && fight.phase < SirePhase.Combat) beginCombat(fight)
    }

    fun nextAction(sire: Npc, target: Player) {
        val fight = fightFor(sire) ?: return
        if (fight.over || !fight.chamber.contains(target.coords)) return
        when (fight.phase) {
            SirePhase.Asleep,
            SirePhase.Stunned -> Unit
            SirePhase.Stasis -> if (deps.random.of(2) == 0) miasma(fight, target) else launchSpawn(fight, target)
            SirePhase.Combat ->
                when {
                    distanceTo(sire, target) <= SIRE_MELEE_RANGE -> deps.runAbility(sire, target, DOUBLE_HOOK_ABILITY)
                    deps.random.of(2) == 0 -> miasma(fight, target)
                    else -> launchSpawn(fight, target)
                }
            SirePhase.Centre -> {
                miasma(fight, target)
                launchSpawn(fight, target)
            }
        }
    }

    fun end(sire: Npc) {
        val fight = fightFor(sire) ?: return
        finish(fight)
    }

    fun playersIn(chamber: SireChamber): List<Player> =
        deps.playerList.filter { chamber.contains(it.coords) && it.hitpoints > 0 }

    private fun start(chamber: SireChamber, sire: Npc): SireFight? {
        if (sire.visType.internalName != SIRE_SLEEPING) return null
        val fight = SireFight(chamber, sire)
        fights[chamber] = fight
        watch(fight)
        return fight
    }

    private fun wake(fight: SireFight, attacker: Player) {
        fight.phase = SirePhase.Stasis
        morph(fight.sire, SIRE_AWAKE)
        fight.sire.anim(WAKING_SEQ)
        fight.sire.movementLocked = true
        fight.sire.apRangeOverride = STASIS_RANGE
        fight.sire.apRequiresLineOfSight = false
        fight.sire.apPlayer2(attacker, aiPlayerInteractions)
        wakeTentacles(fight, attacker)
    }

    private fun addStunDamage(fight: SireFight, damage: Int) {
        if (fight.phase != SirePhase.Stasis) return
        fight.stunDamage += damage
        if (fight.stunDamage >= STUN_DAMAGE) stun(fight)
    }

    private fun stun(fight: SireFight) {
        fight.phase = SirePhase.Stunned
        fight.stunDamage = 0
        val stunId = ++fight.stunId
        morph(fight.sire, SIRE_STUNNED)
        for (tentacle in tentacles(fight.chamber)) {
            tentacle.resetMode()
            morph(tentacle, TENTACLE_STUNNED)
            tentacle.anim(TENTACLE_STUN_SEQ)
        }
        later(fight, STUN_TICKS) {
            if (fight.phase != SirePhase.Stunned || fight.stunId != stunId) return@later
            fight.phase = SirePhase.Stasis
            morph(fight.sire, SIRE_AWAKE)
            playersIn(fight.chamber).firstOrNull()?.let { wakeTentacles(fight, it) }
        }
    }

    private fun wakeTentacles(fight: SireFight, target: Player) {
        for (tentacle in tentacles(fight.chamber)) {
            morph(tentacle, TENTACLE_ACTIVE)
            tentacle.anim(TENTACLE_WAKE_SEQ)
            tentacle.movementLocked = true
            tentacle.apPlayer2(target, aiPlayerInteractions)
        }
    }

    private fun beginCombat(fight: SireFight) {
        fight.phase = SirePhase.Combat
        sleepTentacles(fight.chamber)
        val sire = fight.sire
        morph(sire, SIRE_WANDERING)
        sire.anim(MOBILISE_SEQ)
        sire.hitpoints = sire.baseHitpointsLvl
        sire.movementLocked = false
        sire.apRangeOverride = SIRE_MELEE_RANGE
        sire.apRequiresLineOfSight = true
        deps.suppressAttacks(sire, MOBILISE_TICKS)
        later(fight, MOBILISE_TICKS) { playersIn(fight.chamber).firstOrNull()?.let { sire.apPlayer2(it, aiPlayerInteractions) } }
    }

    private fun moveToCentre(fight: SireFight) {
        fight.phase = SirePhase.Centre
        val sire = fight.sire
        morph(sire, SIRE_PANICKING)
        sire.anim(PANIC_SEQ)
        PathingEntityCommon.telejump(sire, deps.collision, fight.chamber.centre)
        sire.movementLocked = true
        sire.apRangeOverride = CENTRE_RANGE
        sire.apRequiresLineOfSight = false
    }

    private fun explode(fight: SireFight, target: Player) {
        fight.exploded = true
        val sire = fight.sire
        morph(sire, SIRE_APOCALYPSE)
        sire.anim(APOCALYPSE_SEQ)
        deps.suppressAttacks(sire, EXPLOSION_TICKS + 2)
        PathingEntityCommon.telejump(target, deps.collision, sire.coords.translate(SIRE_SIZE / 2, -1))
        target.mes("The Sire drags you towards it!")
        later(fight, EXPLOSION_TICKS) {
            val centre = sire.coords.translate(SIRE_SIZE / 2, SIRE_SIZE / 2)
            deps.worldRepo.spotanimMap(SpotanimType(PORTAL.asRSCM(RSCMType.SPOTANIM)), centre)
            for (player in playersIn(fight.chamber)) {
                if (distanceTo(sire, player) <= EXPLOSION_REACH) deps.runAbility(sire, player, EXPLOSION_ABILITY)
            }
            morph(sire, SIRE_PANICKING)
        }
    }

    private fun miasma(fight: SireFight, target: Player) {
        val sire = fight.sire
        sire.anim(MIASMA_SEQ)
        val tile = target.coords
        lob(fight, tile, MIASMA_ORB, MIASMA_LAND_TICKS)
        for (pulse in 0 until MIASMA_PULSES) {
            later(fight, MIASMA_LAND_TICKS + pulse) {
                if (pulse % 2 == 0) deps.worldRepo.spotanimMap(SpotanimType(MIASMA_POOL.asRSCM(RSCMType.SPOTANIM)), tile)
                for (player in playersIn(fight.chamber)) {
                    if (player.coords.chebyshevDistance(tile) <= 1) deps.runAbility(sire, player, MIASMA_ABILITY)
                }
            }
        }
    }

    private fun launchSpawn(fight: SireFight, target: Player) {
        val sire = fight.sire
        sire.anim(SPAWN_SEQ)
        val tile = target.coords
        lob(fight, tile, SPAWN_ORB, SPAWN_LAND_TICKS)
        later(fight, SPAWN_LAND_TICKS) {
            val spawn = deps.spawnOwnedNpc(sire, SPAWN, tile, SPAWN_LIFETIME) ?: return@later
            spawn.respawns = false
            if (target.hitpoints > 0) spawn.apPlayer2(target, aiPlayerInteractions)
            deps.worldQueues.add(SCION_TICKS) { matureSpawn(spawn) }
        }
    }

    private fun matureSpawn(spawn: Npc) {
        if (!spawn.isSlotAssigned || spawn.hitpoints <= 0 || spawn.visType.internalName != SPAWN) return
        val target = playersIn(SireChamber.containing(spawn.coords) ?: return).minByOrNull {
            it.coords.chebyshevDistance(spawn.coords)
        }
        morph(spawn, SCION)
        spawn.hitpoints = spawn.visType.hitpoints
        spawn.anim(SCION_SPAWN_SEQ)
        target?.let { spawn.apPlayer2(it, aiPlayerInteractions) }
    }

    private fun watch(fight: SireFight) {
        later(fight, WATCH_TICKS) {
            if (playersIn(fight.chamber).isEmpty()) {
                reset(fight)
            } else {
                watch(fight)
            }
        }
    }

    private fun reset(fight: SireFight) {
        val sire = fight.sire
        finish(fight)
        if (sire.hitpoints <= 0 || !sire.isSlotAssigned) return
        sire.resetMode()
        sire.resetTransmog()
        sire.hitpoints = sire.baseHitpointsLvl
        sire.movementLocked = false
        sire.apRangeOverride = null
        PathingEntityCommon.telejump(sire, deps.collision, sire.spawnCoords)
    }

    private fun finish(fight: SireFight) {
        if (fight.over) return
        fight.over = true
        fights.remove(fight.chamber)
        deps.clearOwnedNpcs(fight.sire)
        sleepTentacles(fight.chamber)
        for (lung in npcsIn(fight.chamber, LUNG)) restoreLung(lung)
    }

    private fun sleepTentacles(chamber: SireChamber) {
        for (tentacle in tentacles(chamber)) {
            tentacle.resetMode()
            tentacle.resetTransmog()
        }
    }

    private fun restoreLung(lung: Npc) {
        if (lung.visType.internalName == LUNG && lung.hitpoints > 0) return
        lung.resetTransmog()
        lung.resetMode()
        lung.hitpoints = lung.baseHitpointsLvl
        lung.showAllOps()
    }

    private fun tentacles(chamber: SireChamber): List<Npc> =
        chamber.tentacles.flatMap { tile -> deps.npcRepo.findAll(ZoneKey.from(tile), 1).filter { it.coords == tile } }
            .filter { it.type.internalName in TENTACLE_SLEEPING }

    private fun npcsIn(chamber: SireChamber, baseType: String): List<Npc> =
        deps.npcRepo.findAll(ZoneKey.from(chamber.centre), CHAMBER_ZONE_RADIUS)
            .filter { it.type.internalName == baseType && chamber.contains(it.coords) }
            .toList()

    private fun sireIn(chamber: SireChamber): Npc? = npcsIn(chamber, SIRE_SLEEPING).firstOrNull()

    private fun morph(npc: Npc, into: String) {
        val type = ServerCacheManager.getNpc(into.asRSCM(RSCMType.NPC)) ?: return
        if (type.id == npc.type.id) npc.resetTransmog() else npc.transmog(type, Int.MAX_VALUE)
    }

    private fun keepAlive(sire: Npc, hit: HitBuilder) {
        if (hit.damage >= sire.hitpoints) hit.damage = sire.hitpoints - 1
    }

    private fun attacker(hit: HitBuilder): Player? {
        if (!hit.isFromPlayer) return null
        val slot = hit.sourceSlot ?: return null
        return deps.playerList[slot]
    }

    private fun lob(fight: SireFight, tile: CoordGrid, spotanim: String, landTicks: Int) {
        val sire = fight.sire
        deps.bossProjectile(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            src = sire.coords.translate(SIRE_SIZE / 2, SIRE_SIZE / 2),
            target = tile,
            startHeight = ORB_START_HEIGHT,
            endHeight = 0,
            delay = ORB_DELAY,
            travel = landTicks * CLIENT_CYCLES_PER_TICK - ORB_DELAY,
            curve = ORB_CURVE,
        )
    }

    private fun later(fight: SireFight, ticks: Int, action: () -> Unit) {
        deps.worldQueues.add(ticks) { if (!fight.over && fight.sire.isSlotAssigned) action() }
    }

    private fun distanceTo(sire: Npc, player: Player): Int {
        val x = player.coords.x.coerceIn(sire.coords.x, sire.coords.x + SIRE_SIZE - 1)
        val z = player.coords.z.coerceIn(sire.coords.z, sire.coords.z + SIRE_SIZE - 1)
        return player.coords.chebyshevDistance(CoordGrid(x, z, sire.coords.level))
    }

    private companion object {
        const val WAKING_SEQ = "seq.sire_waking"
        const val MOBILISE_SEQ = "seq.sire_mobilising"
        const val PANIC_SEQ = "seq.sire_panic_mode"
        const val APOCALYPSE_SEQ = "seq.sire_apocalypse"
        const val MIASMA_SEQ = "seq.sire_attack_miasma"
        const val SPAWN_SEQ = "seq.sire_attack_spawns"
        const val TENTACLE_WAKE_SEQ = "seq.abyssal_tentacle_waking"
        const val TENTACLE_STUN_SEQ = "seq.abyssal_tentacle_stunned"
        const val SCION_SPAWN_SEQ = "seq.abyssal_scion_spawn"
        const val MIASMA_ORB = "spotanim.abyssal_miasma_spotanim"
        const val MIASMA_POOL = "spotanim.abyssal_miasma_spotanim"
        const val SPAWN_ORB = "spotanim.abyssal_spawn_projanim"
        const val PORTAL = "spotanim.abyssal_portal"

        const val SIRE_SIZE = 6
        const val SIRE_MELEE_RANGE = 3
        const val STASIS_RANGE = 30
        const val CENTRE_RANGE = 30
        const val CHAMBER_ZONE_RADIUS = 4
        const val WATCH_TICKS = 5

        const val STUN_DAMAGE = 75
        const val STUN_TICKS = 45
        const val MOBILISE_TICKS = 4
        const val CENTRE_PERCENT = 50
        const val EXPLODE_PERCENT = 33
        const val EXPLOSION_TICKS = 3
        const val EXPLOSION_REACH = 1

        const val MIASMA_LAND_TICKS = 2
        const val MIASMA_PULSES = 6
        const val SPAWN_LAND_TICKS = 2
        const val SCION_TICKS = 25
        const val SPAWN_LIFETIME = 100

        const val ORB_START_HEIGHT = 90
        const val ORB_DELAY = 20
        const val ORB_CURVE = 30
        const val CLIENT_CYCLES_PER_TICK = 30
    }
}
