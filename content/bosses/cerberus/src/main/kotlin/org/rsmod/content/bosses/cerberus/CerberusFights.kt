package org.rsmod.content.bosses.cerberus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.clearOwnedNpcs
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal class CerberusFight(val arena: CerberusArena) {
    var attacks: Int = 0
    var sinceTriple: Int = 0
    var over: Boolean = false
}

@Singleton
internal class CerberusFights
@Inject
constructor(private val deps: BossDeps, private val aiPlayerInteractions: AiPlayerInteractions) {
    private val fights = IdentityHashMap<Npc, CerberusFight>()

    fun enterArena(player: Player, arena: CerberusArena) {
        val cerberus = cerberusIn(arena) ?: return
        if (cerberus.visType.internalName != CERBERUS_SITTING || cerberus in fights) return
        val type = ServerCacheManager.getNpc(CERBERUS.asRSCM(RSCMType.NPC)) ?: return
        val fight = CerberusFight(arena)
        fights[cerberus] = fight
        cerberus.transmog(type, Int.MAX_VALUE)
        cerberus.apRangeOverride = ATTACK_RANGE
        cerberus.apRequiresLineOfSight = false
        cerberus.anim(STAND_SEQ)
        deps.worldQueues.add(STAND_TICKS) {
            if (fight.over || !cerberus.isSlotAssigned) return@add
            if (arena.contains(player.coords) && player.hitpoints > 0) cerberus.apPlayer2(player, aiPlayerInteractions)
            watch(cerberus, fight)
        }
    }

    fun end(cerberus: Npc): CerberusFight? {
        val fight = fights.remove(cerberus) ?: return null
        fight.over = true
        deps.clearOwnedNpcs(cerberus)
        return fight
    }

    fun playersIn(arena: CerberusArena): List<Player> =
        deps.playerList.filter { arena.contains(it.coords) && it.hitpoints > 0 }

    fun nextAction(cerberus: Npc, target: Player) {
        val fight = fights[cerberus] ?: return
        if (fight.over) return
        fight.attacks++
        val hp = cerberus.hitpoints
        when {
            fight.attacks == 1 || fight.sinceTriple >= TRIPLE_EVERY -> triple(cerberus, fight, target)
            hp < SOULS_BELOW && fight.attacks % SOULS_EVERY == 0 && specialRolls() -> souls(cerberus, fight, target)
            hp < LAVA_BELOW && fight.attacks % LAVA_EVERY == 0 && specialRolls() -> lava(cerberus, fight, target)
            else -> standard(cerberus, fight, target)
        }
    }

    private fun specialRolls(): Boolean = deps.random.of(SPECIAL_SKIP_ONE_IN) != 0

    private fun standard(cerberus: Npc, fight: CerberusFight, target: Player) {
        fight.sinceTriple++
        val styles = if (adjacent(cerberus, target)) 3 else 2
        when (deps.random.of(styles)) {
            0 -> deps.runAbility(cerberus, target, RANGED_ABILITY)
            1 -> deps.runAbility(cerberus, target, MAGIC_ABILITY)
            else -> deps.runAbility(cerberus, target, MELEE_ABILITY)
        }
    }

    private fun triple(cerberus: Npc, fight: CerberusFight, target: Player) {
        fight.sinceTriple = 0
        deps.suppressAttacks(cerberus, TRIPLE_GAP * 2 + CERBERUS_ATTACK_RATE)
        deps.runAbility(cerberus, target, MAGIC_ABILITY)
        later(cerberus, fight, TRIPLE_GAP) { deps.runAbility(cerberus, target, RANGED_ABILITY) }
        later(cerberus, fight, TRIPLE_GAP * 2) { deps.runAbility(cerberus, target, MELEE_ABILITY) }
    }

    private fun souls(cerberus: Npc, fight: CerberusFight, target: Player) {
        cerberus.say(SOULS_SHOUT)
        cerberus.anim(HOWL_SEQ)
        val types = listOf(SOUL_RANGED, SOUL_MAGIC, SOUL_MELEE).shuffled()
        val souls = fight.arena.soulTiles.zip(types).mapNotNull { (tile, type) -> deps.spawnOwnedNpc(cerberus, type, tile) }
        souls.forEachIndexed { index, soul ->
            soul.respawns = false
            soul.movementLocked = true
            later(cerberus, fight, SOUL_FIRST_ATTACK + index * SOUL_GAP) {
                if (soul.isSlotAssigned && target.hitpoints > 0 && fight.arena.contains(target.coords)) {
                    soul.facePlayer(target)
                    deps.runAbility(soul, target, SOUL_ABILITY)
                }
            }
        }
        later(cerberus, fight, SOUL_FIRST_ATTACK + souls.size * SOUL_GAP + SOUL_LINGER) {
            souls.filter { it.isSlotAssigned }.forEach { deps.npcRepo.del(it, Int.MAX_VALUE) }
        }
    }

    private fun lava(cerberus: Npc, fight: CerberusFight, target: Player) {
        cerberus.say(LAVA_SHOUT)
        cerberus.anim(LAVA_SEQ)
        val open = arenaTiles(fight.arena).filter { it != target.coords }.shuffled().take(LAVA_POOLS - 1)
        for (tile in open + target.coords) {
            deps.bossProjectile(
                spotanim = LAVA_ORB.asRSCM(RSCMType.SPOTANIM),
                src = cerberus.coords.translate(CERBERUS_SIZE / 2, CERBERUS_SIZE / 2),
                target = tile,
                startHeight = ORB_START_HEIGHT,
                endHeight = 0,
                delay = ORB_DELAY,
                travel = LAVA_LAND_TICKS * CLIENT_CYCLES_PER_TICK - ORB_DELAY,
                curve = ORB_CURVE,
            )
            later(cerberus, fight, LAVA_LAND_TICKS) { burnPool(cerberus, fight, tile, LAVA_LAND_ABILITY) }
            for (pulse in 1..LAVA_PULSES) {
                later(cerberus, fight, LAVA_LAND_TICKS + pulse * LAVA_PULSE_GAP) {
                    burnPool(cerberus, fight, tile, LAVA_BURN_ABILITY)
                }
            }
        }
    }

    private fun burnPool(cerberus: Npc, fight: CerberusFight, tile: CoordGrid, ability: String) {
        deps.worldRepo.spotanimMap(SpotanimType(LAVA_POOL.asRSCM(RSCMType.SPOTANIM)), tile)
        for (player in playersIn(fight.arena)) {
            if (player.coords == tile) deps.runAbility(cerberus, player, ability)
        }
    }

    private fun arenaTiles(arena: CerberusArena): List<CoordGrid> {
        val tiles = ArrayList<CoordGrid>()
        for (x in CerberusLair.MIN_X..CerberusLair.MAX_X) {
            for (z in ARENA_FLOOR_MIN_Z..CerberusLair.MAX_Z) tiles += arena.shift(CoordGrid(x, z, 0))
        }
        return tiles
    }

    private fun watch(cerberus: Npc, fight: CerberusFight) {
        deps.worldQueues.add(WATCH_TICKS) {
            if (fight.over || !cerberus.isSlotAssigned) return@add
            if (playersIn(fight.arena).isEmpty()) reset(cerberus) else watch(cerberus, fight)
        }
    }

    fun resetIfEmpty(arena: CerberusArena) {
        if (playersIn(arena).isNotEmpty()) return
        fights.entries.firstOrNull { it.value.arena == arena }?.key?.let(::reset)
    }

    private fun reset(cerberus: Npc) {
        if (cerberus.hitpoints <= 0) return
        end(cerberus)
        cerberus.resetMode()
        cerberus.resetTransmog()
        cerberus.hitpoints = cerberus.baseHitpointsLvl
        cerberus.anim(SIT_SEQ)
    }

    private fun cerberusIn(arena: CerberusArena): Npc? =
        deps.npcRepo.findAll(ZoneKey.from(arena.spawn), ZONE_RADIUS).firstOrNull {
            it.type.internalName == CERBERUS_SITTING && arena.contains(it.coords)
        }

    private fun later(cerberus: Npc, fight: CerberusFight, ticks: Int, action: () -> Unit) {
        deps.worldQueues.add(ticks) { if (!fight.over && cerberus.isSlotAssigned) action() }
    }

    private fun adjacent(cerberus: Npc, target: Player): Boolean {
        val dx = target.coords.x - cerberus.coords.x
        val dz = target.coords.z - cerberus.coords.z
        val inside = dx in 0 until CERBERUS_SIZE && dz in 0 until CERBERUS_SIZE
        val edge = dx in -1..CERBERUS_SIZE && dz in -1..CERBERUS_SIZE
        val corner = (dx == -1 || dx == CERBERUS_SIZE) && (dz == -1 || dz == CERBERUS_SIZE)
        return edge && !inside && !corner
    }

    private companion object {
        const val STAND_SEQ = "seq.cerberus_idle_to_stand"
        const val SIT_SEQ = "seq.cerberus_stand_to_sit"
        const val HOWL_SEQ = "seq.cerberus_howl"
        const val LAVA_SEQ = "seq.cerberus_special_attack_flame"
        const val LAVA_ORB = "spotanim.cerberus_special_attack_flame"
        const val LAVA_POOL = "spotanim.fire_liquid"
        const val SOULS_SHOUT = "Aaarrrooooooo"
        const val LAVA_SHOUT = "Grrrrrrrrrrrrrr"

        const val CERBERUS_SIZE = 5
        const val ATTACK_RANGE = 15
        const val ZONE_RADIUS = 2
        const val STAND_TICKS = 2
        const val WATCH_TICKS = 5
        const val ARENA_FLOOR_MIN_Z = 1243

        const val TRIPLE_EVERY = 10
        const val TRIPLE_GAP = 2
        const val SOULS_BELOW = 400
        const val SOULS_EVERY = 7
        const val LAVA_BELOW = 200
        const val LAVA_EVERY = 5
        const val SPECIAL_SKIP_ONE_IN = 10

        const val SOUL_FIRST_ATTACK = 3
        const val SOUL_GAP = 2
        const val SOUL_LINGER = 2

        const val LAVA_POOLS = 3
        const val LAVA_LAND_TICKS = 3
        const val LAVA_PULSES = 4
        const val LAVA_PULSE_GAP = 2

        const val ORB_START_HEIGHT = 80
        const val ORB_DELAY = 20
        const val ORB_CURVE = 30
        const val CLIENT_CYCLES_PER_TICK = 30
    }
}
