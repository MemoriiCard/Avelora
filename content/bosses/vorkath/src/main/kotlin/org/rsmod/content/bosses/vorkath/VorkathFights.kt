package org.rsmod.content.bosses.vorkath

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal enum class VorkathSpecial {
    Acid,
    Spawn,
}

internal class VorkathFight(val player: Player, val session: InstanceSession) {
    var attacks: Int = 0
    var nextSpecial: VorkathSpecial = VorkathSpecial.Acid
    var acidPhase: Boolean = false
    var spawn: Npc? = null
    var over: Boolean = false
    val acid: MutableMap<CoordGrid, LocInfo> = HashMap()
}

@Singleton
internal class VorkathFights
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val fights = IdentityHashMap<Npc, VorkathFight>()
    private val sessions = IdentityHashMap<Npc, InstanceSession>()

    fun fightFor(npc: Npc): VorkathFight? = fights[npc]

    fun spawnSleeping(session: InstanceSession) {
        val coords = instances.resolveCoord(session, VorkathArena.LAIR) ?: return
        val sleeping = Npc(SLEEPING, coords)
        deps.npcRepo.add(sleeping, Int.MAX_VALUE)
        sleeping.respawns = false
        instances.attachNpc(session.id, sleeping)
        sessions[sleeping] = session
    }

    fun wake(sleeping: Npc, player: Player) {
        val session = sessions.remove(sleeping) ?: return
        val coords = sleeping.coords
        deps.npcRepo.del(sleeping, Int.MAX_VALUE)
        val vorkath = Npc(AWAKE, coords)
        deps.npcRepo.add(vorkath, Int.MAX_VALUE)
        vorkath.respawns = false
        vorkath.movementLocked = true
        vorkath.apRangeOverride = ATTACK_RANGE
        vorkath.apRequiresLineOfSight = false
        instances.attachNpc(session.id, vorkath)
        val fight = VorkathFight(player, session)
        fight.nextSpecial = VorkathSpecial.entries[deps.random.of(VorkathSpecial.entries.size)]
        fights[vorkath] = fight
        vorkath.anim(WAKE_SEQ)
        deps.worldQueues.add(WAKE_TICKS) {
            if (fight.over || !vorkath.isSlotAssigned) return@add
            vorkath.apPlayer2(player, aiPlayerInteractions)
            scheduleAcidTick(vorkath, fight)
        }
    }

    fun end(npc: Npc): VorkathFight? {
        val fight = fights.remove(npc) ?: return null
        fight.over = true
        clearAcid(fight)
        fight.spawn?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        fight.spawn = null
        CombatEffects.unfreeze(fight.player)
        return fight
    }

    fun endFor(player: Player) {
        fights.entries.filter { it.value.player === player }.forEach { end(it.key) }
    }

    fun respawnSleeping(fight: VorkathFight) {
        deps.worldQueues.add(RESPAWN_TICKS) {
            if (instances.sessionForPlayer(fight.player) === fight.session) spawnSleeping(fight.session)
        }
    }

    fun nextAction(vorkath: Npc, target: Player) {
        val fight = fights[vorkath] ?: return
        if (fight.over) return
        if (fight.attacks >= STANDARD_ATTACKS) {
            fight.attacks = 0
            val special = fight.nextSpecial
            fight.nextSpecial = if (special == VorkathSpecial.Acid) VorkathSpecial.Spawn else VorkathSpecial.Acid
            when (special) {
                VorkathSpecial.Acid -> acidPhase(vorkath, fight, target)
                VorkathSpecial.Spawn -> zombifiedSpawn(vorkath, fight, target)
            }
            return
        }
        fight.attacks++
        val adjacent = adjacent(vorkath, target)
        when (val roll = deps.random.of(if (adjacent) STANDARD_WEIGHT + MELEE_WEIGHT else STANDARD_WEIGHT)) {
            in 0 until RANGED_WEIGHT -> deps.runAbility(vorkath, target, RANGED_ABILITY)
            in RANGED_WEIGHT until MAGIC_END -> deps.runAbility(vorkath, target, MAGIC_ABILITY)
            in MAGIC_END until FIRE_END -> deps.runAbility(vorkath, target, DRAGONFIRE_ABILITY)
            in FIRE_END until VENOM_END -> deps.runAbility(vorkath, target, VENOM_FIRE_ABILITY)
            in VENOM_END until PRAYER_END -> deps.runAbility(vorkath, target, PRAYER_FIRE_ABILITY)
            in PRAYER_END until STANDARD_WEIGHT -> deadlyFireball(vorkath, fight, target)
            else -> if (roll >= STANDARD_WEIGHT) deps.runAbility(vorkath, target, MELEE_ABILITY)
        }
    }

    private fun deadlyFireball(vorkath: Npc, fight: VorkathFight, target: Player) {
        vorkath.anim(FIREBALL_SEQ)
        val tile = target.coords
        lob(vorkath, tile, FIREBALL, FIREBALL_LAND_TICKS) {
            val player = fight.player
            if (player.hitpoints <= 0) return@lob
            when (player.coords.chebyshevDistance(tile)) {
                0 -> deps.runAbility(vorkath, player, FIREBALL_DIRECT_ABILITY)
                1 -> deps.runAbility(vorkath, player, FIREBALL_SPLASH_ABILITY)
            }
        }
    }

    private fun acidPhase(vorkath: Npc, fight: VorkathFight, target: Player) {
        fight.acidPhase = true
        deps.suppressAttacks(vorkath, ACID_PHASE_TICKS)
        vorkath.anim(ACID_SEQ)
        val pools = VorkathArena.TILES.shuffled().take(ACID_POOLS).toMutableList()
        val playerTile = target.coords
        for (static in pools) {
            val tile = instances.resolveCoord(fight.session, static) ?: continue
            lob(vorkath, tile, ACID_ORB, ACID_LAND_TICKS) { placeAcid(fight, tile) }
        }
        lob(vorkath, playerTile, ACID_ORB, ACID_LAND_TICKS) { placeAcid(fight, playerTile) }
        for (shot in 0 until RAPID_FIRE_SHOTS) {
            deps.worldQueues.add(RAPID_FIRE_START + shot) {
                if (fight.over || !vorkath.isSlotAssigned) return@add
                val player = fight.player
                val tile = player.coords
                vorkath.faceSquare(tile)
                lob(vorkath, tile, RAPID_FIRE, RAPID_FIRE_LAND_TICKS) {
                    if (player.coords == tile && player.hitpoints > 0) {
                        deps.runAbility(vorkath, player, RAPID_FIRE_ABILITY)
                    }
                }
            }
        }
        deps.worldQueues.add(ACID_PHASE_TICKS) {
            fight.acidPhase = false
            if (!fight.over) clearAcid(fight)
        }
    }

    private fun placeAcid(fight: VorkathFight, tile: CoordGrid) {
        if (!fight.acidPhase || tile in fight.acid) return
        val loc = deps.locRepo.add(tile, ACID_LOC, ACID_PHASE_TICKS, LocAngle.North, LocShape.GroundDecor) ?: return
        fight.acid[tile] = loc
    }

    private fun clearAcid(fight: VorkathFight) {
        for (loc in fight.acid.values) deps.locRepo.del(loc, Int.MAX_VALUE)
        fight.acid.clear()
    }

    private fun scheduleAcidTick(vorkath: Npc, fight: VorkathFight) {
        deps.worldQueues.add(1) {
            if (fight.over || !vorkath.isSlotAssigned) return@add
            val player = fight.player
            if (fight.acid.isNotEmpty() && player.coords in fight.acid && player.hitpoints > 0) {
                val before = player.hitpoints
                deps.runAbility(vorkath, player, ACID_ABILITY)
                deps.worldQueues.add(1) {
                    val dealt = before - player.hitpoints
                    if (dealt > 0 && vorkath.isSlotAssigned) vorkath.heal(dealt, showHitsplat = false)
                }
            }
            scheduleAcidTick(vorkath, fight)
        }
    }

    private fun zombifiedSpawn(vorkath: Npc, fight: VorkathFight, target: Player) {
        deps.encounter(vorkath).invulnerable = true
        deps.suppressAttacks(vorkath, SPAWN_PHASE_TICKS)
        deps.runAbility(vorkath, target, ICE_BREATH_ABILITY)
        deps.worldQueues.add(ICE_BREATH_LAND_TICKS) {
            if (fight.over) return@add
            CombatEffects.freeze(fight.player, SPAWN_PHASE_TICKS)
        }
        val landing = spawnLanding(fight, target) ?: return
        deps.worldQueues.add(SPAWN_THROW_DELAY) {
            if (fight.over || !vorkath.isSlotAssigned) return@add
            vorkath.anim(FIREBALL_SEQ)
            lob(vorkath, landing, SPAWN_ORB, SPAWN_LAND_TICKS) {
                val spawn = deps.spawnOwnedNpc(vorkath, SPAWN, landing) ?: return@lob
                spawn.respawns = false
                instances.attachNpc(fight.session.id, spawn)
                fight.spawn = spawn
                spawnByNpc[spawn] = vorkath
                spawn.opPlayer2(fight.player, aiPlayerInteractions)
            }
        }
    }

    private val spawnByNpc = IdentityHashMap<Npc, Npc>()

    private fun spawnLanding(fight: VorkathFight, target: Player): CoordGrid? {
        val base = instances.resolveCoord(fight.session, VorkathArena.LAIR) ?: return null
        val player = target.coords.translate(VorkathArena.LAIR.x - base.x, VorkathArena.LAIR.z - base.z)
        val tiles = VorkathArena.TILES.filter { it.chebyshevDistance(player) == SPAWN_DISTANCE }
        val choice = tiles.randomOrNull() ?: VorkathArena.TILES.maxBy { it.chebyshevDistance(player) }
        return instances.resolveCoord(fight.session, choice)
    }

    fun explodeSpawn(spawn: Npc, target: Player) {
        val vorkath = spawnByNpc.remove(spawn)
        deps.runAbility(spawn, target, BLAST_ABILITY)
        deps.npcRepo.del(spawn, Int.MAX_VALUE)
        vorkath?.let { releaseSpawnPhase(it) }
    }

    fun onSpawnDeath(spawn: Npc) {
        spawnByNpc.remove(spawn)?.let { releaseSpawnPhase(it) }
    }

    private fun releaseSpawnPhase(vorkath: Npc) {
        val fight = fights[vorkath] ?: return
        fight.spawn = null
        deps.encounter(vorkath).invulnerable = false
        CombatEffects.unfreeze(fight.player)
        deps.encounter(vorkath).busyUntil = deps.mapClock.cycle + SPAWN_RECOVERY_TICKS
    }

    private fun lob(vorkath: Npc, tile: CoordGrid, spotanim: String, landTicks: Int, onLand: () -> Unit) {
        val fight = fights[vorkath] ?: return
        val centre = vorkath.coords.translate(VorkathArena.VORKATH_SIZE / 2, VorkathArena.VORKATH_SIZE / 2)
        deps.bossProjectile(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            src = centre,
            target = tile,
            startHeight = ORB_START_HEIGHT,
            endHeight = 0,
            delay = ORB_DELAY,
            travel = landTicks * CLIENT_CYCLES_PER_TICK - ORB_DELAY,
            curve = ORB_CURVE,
        )
        deps.worldQueues.add(landTicks) {
            if (!fight.over && vorkath.isSlotAssigned) onLand()
        }
    }

    private fun adjacent(vorkath: Npc, target: Player): Boolean {
        val dx = target.coords.x - vorkath.coords.x
        val dz = target.coords.z - vorkath.coords.z
        val size = VorkathArena.VORKATH_SIZE
        return dx in -1..size && dz in -1..size
    }

    private companion object {
        const val SLEEPING = "npc.vorkath_sleeping"
        const val AWAKE = "npc.vorkath"
        const val SPAWN = "npc.vorkath_spawn"
        const val ACID_LOC = "loc.vorkath_acid"
        const val WAKE_SEQ = "seq.ds2_vorkath_spawn"
        const val FIREBALL_SEQ = "seq.ds2_vorkath_ranged_up"
        const val ACID_SEQ = "seq.ds2_vorkath_acid"
        const val FIREBALL = "spotanim.vorkath_area_travel"
        const val RAPID_FIRE = "spotanim.vorkath_area_small_travel"
        const val ACID_ORB = "spotanim.vorkath_acid_travel"
        const val SPAWN_ORB = "spotanim.vorkath_spawn_travel"

        const val ATTACK_RANGE = 20
        const val WAKE_TICKS = 5
        const val RESPAWN_TICKS = 5
        const val STANDARD_ATTACKS = 6

        const val RANGED_WEIGHT = 4
        const val MAGIC_END = RANGED_WEIGHT + 3
        const val FIRE_END = MAGIC_END + 2
        const val VENOM_END = FIRE_END + 1
        const val PRAYER_END = VENOM_END + 1
        const val STANDARD_WEIGHT = PRAYER_END + 1
        const val MELEE_WEIGHT = 4

        const val FIREBALL_LAND_TICKS = 4
        const val ACID_POOLS = 40
        const val ACID_LAND_TICKS = 3
        const val RAPID_FIRE_START = 4
        const val RAPID_FIRE_SHOTS = 25
        const val RAPID_FIRE_LAND_TICKS = 2
        const val ACID_PHASE_TICKS = RAPID_FIRE_START + RAPID_FIRE_SHOTS + RAPID_FIRE_LAND_TICKS + 1

        const val ICE_BREATH_LAND_TICKS = 2
        const val SPAWN_THROW_DELAY = 3
        const val SPAWN_LAND_TICKS = 3
        const val SPAWN_DISTANCE = 10
        const val SPAWN_PHASE_TICKS = 40
        const val SPAWN_RECOVERY_TICKS = 2

        const val ORB_START_HEIGHT = 100
        const val ORB_DELAY = 20
        const val ORB_CURVE = 30
        const val CLIENT_CYCLES_PER_TICK = 30
    }
}
