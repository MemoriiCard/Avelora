package org.rsmod.content.bosses.corp

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid

@Singleton
internal class CorpFights @Inject constructor(private val deps: BossDeps) {
    private val cores = IdentityHashMap<Npc, Npc>()
    private val nextStomp = IdentityHashMap<Npc, Int>()

    fun rollCoreOnAttack(corp: Npc) {
        if (corp.hitpoints < CORE_ATTACK_HP) rollCore(corp)
    }

    fun rollCore(corp: Npc) {
        if (deps.random.of(CORE_CHANCE) != 0) return
        val existing = cores[corp]
        if (existing != null && existing.isSlotAssigned && existing.hitpoints > 0) return
        val tile = corp.coords.translate(-1, CORP_SIZE / 2)
        val type = ServerCacheManager.getNpc(DARK_CORE.asRSCM(RSCMType.NPC)) ?: return
        val core = Npc(type, tile)
        deps.npcRepo.add(core, Int.MAX_VALUE)
        core.respawns = false
        cores[corp] = core
        val encounter = deps.encounterRegistry.of(corp)
        encounter.busyUntil = deps.mapClock.cycle + CORE_ATTACK_RESET
        scheduleCore(corp, core, CORE_TICK)
    }

    private fun scheduleCore(corp: Npc, core: Npc, delay: Int) {
        deps.worldQueues.add(delay) {
            if (!core.isSlotAssigned || core.hitpoints <= 0) {
                cores.remove(corp, core)
                return@add
            }
            if (!corp.isSlotAssigned || corp.hitpoints <= 0) {
                removeCore(corp)
                return@add
            }
            val adjacent = playersInRoom(corp).filter { it.coords.chebyshevDistance(core.coords) <= 1 }
            if (adjacent.isNotEmpty()) {
                for (player in adjacent) deps.runAbility(corp, player, CORE_LEECH_ABILITY)
                scheduleCore(corp, core, CORE_TICK)
                return@add
            }
            val target = playersInRoom(corp).minByOrNull { it.coords.chebyshevDistance(core.coords) }
            if (target == null) {
                scheduleCore(corp, core, CORE_TICK)
                return@add
            }
            jump(core, target.coords)
            scheduleCore(corp, core, CORE_JUMP_TICKS + CORE_TICK)
        }
    }

    private fun jump(core: Npc, dest: CoordGrid) {
        deps.bossProjectile(
            spotanim = CORE_JUMP.asRSCM(RSCMType.SPOTANIM),
            src = core.coords,
            target = dest,
            startHeight = 0,
            endHeight = 0,
            delay = 0,
            travel = CORE_JUMP_TICKS * CLIENT_CYCLES_PER_TICK,
            curve = CORE_JUMP_CURVE,
        )
        deps.worldQueues.add(CORE_JUMP_TICKS) {
            if (!core.isSlotAssigned) return@add
            PathingEntityCommon.telejump(core, deps.collision, dest)
        }
    }

    fun removeCore(corp: Npc) {
        val core = cores.remove(corp) ?: return
        if (core.isSlotAssigned) deps.npcRepo.del(core, Int.MAX_VALUE)
    }

    fun stompCheck(corp: Npc) {
        val now = deps.mapClock.cycle
        val due = nextStomp[corp] ?: (now + STOMP_INTERVAL).also { nextStomp[corp] = it }
        if (now < due) return
        nextStomp[corp] = now + STOMP_INTERVAL
        val under = playersInRoom(corp).filter { underneath(corp, it.coords) }
        if (under.isEmpty()) return
        corp.anim(STOMP_SEQ)
        deps.worldRepo.spotanimMap(SpotanimType(STOMP_SPOTANIM.asRSCM(RSCMType.SPOTANIM)), corp.coords.translate(CORP_SIZE / 2, CORP_SIZE / 2))
        for (player in under) deps.runAbility(corp, player, STOMP_ABILITY)
    }

    fun splitShot(corp: Npc, target: Player) {
        val centre = target.coords
        lob(corp, centre, SPLIT_MAIN) {
            splash(corp, centre, SPLIT_DIRECT_ABILITY, SPLIT_NEAR_ABILITY)
            repeat(SPLINTERS) {
                val dx = deps.random.of(-SPLINTER_RADIUS, SPLINTER_RADIUS)
                val dz = deps.random.of(-SPLINTER_RADIUS, SPLINTER_RADIUS)
                val tile = centre.translate(dx, dz)
                splinter(corp, centre, tile)
            }
        }
    }

    private fun splinter(corp: Npc, from: CoordGrid, tile: CoordGrid) {
        deps.bossProjectile(
            spotanim = SPLINTER.asRSCM(RSCMType.SPOTANIM),
            src = from,
            target = tile,
            startHeight = 0,
            endHeight = 0,
            delay = 0,
            travel = SPLINTER_TICKS * CLIENT_CYCLES_PER_TICK,
            curve = SPLINTER_CURVE,
        )
        deps.worldQueues.add(SPLINTER_TICKS) {
            if (corp.isSlotAssigned && corp.hitpoints > 0) {
                splash(corp, tile, SPLINTER_DIRECT_ABILITY, SPLINTER_NEAR_ABILITY)
            }
        }
    }

    private fun splash(corp: Npc, tile: CoordGrid, direct: String, near: String) {
        for (player in playersInRoom(corp)) {
            if (player.hitpoints <= 0) continue
            when (player.coords.chebyshevDistance(tile)) {
                0 -> deps.runAbility(corp, player, direct)
                1 -> deps.runAbility(corp, player, near)
            }
        }
    }

    private fun lob(corp: Npc, tile: CoordGrid, spotanim: String, onLand: () -> Unit) {
        val centre = corp.coords.translate(CORP_SIZE / 2, CORP_SIZE / 2)
        deps.bossProjectile(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            src = centre,
            target = tile,
            startHeight = SPLIT_START_HEIGHT,
            endHeight = 0,
            delay = SPLIT_DELAY,
            travel = SPLIT_LAND_TICKS * CLIENT_CYCLES_PER_TICK - SPLIT_DELAY,
            curve = SPLIT_CURVE,
        )
        deps.worldQueues.add(SPLIT_LAND_TICKS) {
            if (corp.isSlotAssigned && corp.hitpoints > 0) onLand()
        }
    }

    fun clear(corp: Npc) {
        removeCore(corp)
        nextStomp.remove(corp)
    }

    private fun playersInRoom(corp: Npc): List<Player> =
        deps.playerList.filter {
            it.coords.level == corp.coords.level && it.coords.chebyshevDistance(corp.coords) <= ROOM_REACH && CorpLairMap.inRoom(it.coords)
        }

    private fun underneath(corp: Npc, tile: CoordGrid): Boolean =
        tile.x - corp.coords.x in 0 until CORP_SIZE && tile.z - corp.coords.z in 0 until CORP_SIZE

    private companion object {
        const val CORP_SIZE = 5
        const val ROOM_REACH = 32
        const val CORE_CHANCE = 8
        const val CORE_ATTACK_HP = 1000
        const val CORE_ATTACK_RESET = 4
        const val CORE_TICK = 2
        const val CORE_JUMP_TICKS = 2
        const val CORE_JUMP_CURVE = 40
        const val STOMP_INTERVAL = 7
        const val SPLINTERS = 6
        const val SPLINTER_RADIUS = 3
        const val SPLINTER_TICKS = 2
        const val SPLINTER_CURVE = 20
        const val SPLIT_START_HEIGHT = 80
        const val SPLIT_DELAY = 20
        const val SPLIT_LAND_TICKS = 3
        const val SPLIT_CURVE = 30
        const val CLIENT_CYCLES_PER_TICK = 30

        const val CORE_JUMP = "spotanim.dark_core_jump"
        const val SPLIT_MAIN = "spotanim.corp_spirit_beast_weak_proj"
        const val SPLINTER = "spotanim.corp_spirit_beast_weak_proj"
        const val STOMP_SEQ = "seq.corpbeast_stomp_attack"
        const val STOMP_SPOTANIM = "spotanim.corp_ground_stomp"
    }
}
