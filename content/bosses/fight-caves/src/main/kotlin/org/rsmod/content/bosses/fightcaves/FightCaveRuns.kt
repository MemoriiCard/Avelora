package org.rsmod.content.bosses.fightcaves

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

private var Player.fightCaveWave by intVarp("varp.fight_cave_wave")
private var Player.fightCaveRotation by intVarp("varp.fight_cave_rotation")

internal class FightCaveRun(val player: Player, val session: InstanceSession, val rotation: Int) {
    var wave: Int = 0
    var finished: Boolean = false
    var healersSummoned: Boolean = false
    var jad: Npc? = null
    val monsters: MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap())
    val healers: MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap())
    val distractedHealers: MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap())
    val nextHealCycle: MutableMap<Npc, Int> = IdentityHashMap()
}

@Singleton
internal class FightCaveRuns
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val objRepo: ObjRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val runs = HashMap<InstanceId, FightCaveRun>()
    private val runByNpc = IdentityHashMap<Npc, FightCaveRun>()

    fun runFor(npc: Npc): FightCaveRun? = runByNpc[npc]

    fun runFor(session: InstanceSession): FightCaveRun? = runs[session.id]

    fun start(player: Player, session: InstanceSession) {
        val saved = player.fightCaveWave
        val resuming = saved in 1..FightCaveWaves.FINAL_WAVE
        if (!resuming) player.fightCaveRotation = deps.random.of(FightCaveArena.ROTATION.size)
        val run = FightCaveRun(player, session, player.fightCaveRotation)
        runs[session.id] = run
        if (resuming && saved > 1) player.mes("Your Fight Cave run resumes at wave $saved.")
        scheduleWave(run, if (resuming) saved else 1, START_DELAY)
    }

    fun end(id: InstanceId) {
        val run = runs.remove(id) ?: return
        run.finished = true
        (run.monsters + run.healers).forEach { runByNpc.remove(it) }
    }

    fun forfeit(player: Player, run: FightCaveRun, rewardCoords: CoordGrid) {
        val completed = (run.wave - 1).coerceAtLeast(0)
        player.fightCaveWave = 0
        end(run.session.id)
        payTokkul(player, completed, rewardCoords)
    }

    private fun scheduleWave(run: FightCaveRun, wave: Int, delay: Int) {
        deps.worldQueues.add(delay) {
            if (runs[run.session.id] !== run || run.finished) return@add
            spawnWave(run, wave)
        }
    }

    private fun spawnWave(run: FightCaveRun, wave: Int) {
        run.wave = wave
        run.player.fightCaveWave = wave
        run.player.mes("<col=ef1020>Wave: $wave</col>")
        val monsters = FightCaveWaves.wave(wave)
        val points = FightCaveArena.spawnPoints(wave, run.rotation, monsters.size)
        for ((index, monster) in monsters.withIndex()) {
            val type = if (wave == ORANGE_KETZEK_WAVE && index == 1) ORANGE_KETZEK else monster.npc
            val coords = instances.resolveCoord(run.session, points[index]) ?: continue
            val npc = spawn(run, type, coords, monster.ranged)
            run.monsters += npc
            if (monster == FightCaveMonster.TzTokJad) run.jad = npc
        }
    }

    private fun spawn(run: FightCaveRun, type: String, coords: CoordGrid, ranged: Boolean): Npc {
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        instances.attachNpc(run.session.id, npc)
        runByNpc[npc] = run
        if (ranged) {
            npc.apRangeOverride = RANGED_ATTACK_RANGE
            npc.apPlayer2(run.player, aiPlayerInteractions)
        } else {
            npc.opPlayer2(run.player, aiPlayerInteractions)
        }
        return npc
    }

    suspend fun onDeath(access: StandardNpcAccess, deathSeq: String, deathTicks: Int) {
        val npc = access.npc
        access.noneMode()
        access.hideAllOps()
        access.anim(deathSeq)
        access.delay(deathTicks)
        val coords = npc.coords
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        val run = runByNpc.remove(npc) ?: return
        if (run.finished) return
        run.healers.remove(npc)
        run.distractedHealers.remove(npc)
        run.nextHealCycle.remove(npc)
        if (!run.monsters.remove(npc)) return
        when (npc.type.internalName) {
            FightCaveMonster.TzKek.npc, TZKEK_B -> splitKek(run, coords)
            FightCaveMonster.TzTokJad.npc -> finish(run)
        }
        if (run.monsters.isEmpty() && !run.finished) scheduleWave(run, run.wave + 1, WAVE_DELAY)
    }

    private fun splitKek(run: FightCaveRun, coords: CoordGrid) {
        val second = SPLIT_OFFSETS.map { coords.translate(it.first, it.second) }.firstOrNull { !deps.collision.isWalkBlocked(it) } ?: coords
        for (tile in listOf(coords, second)) {
            run.monsters += spawn(run, SMALL_TZKEK, tile, ranged = false)
        }
    }

    private fun finish(run: FightCaveRun) {
        run.finished = true
        val player = run.player
        for (healer in run.healers.toList()) {
            runByNpc.remove(healer)
            if (healer.isSlotAssigned) deps.npcRepo.del(healer, Int.MAX_VALUE)
        }
        run.healers.clear()
        player.fightCaveWave = 0
        player.invAddOrDrop(objRepo, FIRE_CAPE, coords = FightCaveArena.OUTSIDE)
        payTokkul(player, FightCaveWaves.FINAL_WAVE, FightCaveArena.OUTSIDE)
        player.mes("You were victorious!")
        deps.worldQueues.add(FINISH_DELAY) {
            if (instances.sessionForPlayer(player)?.id != run.session.id) return@add
            val exit = instances.leave(player, run.session, deps.mapClock.cycle)
            PathingEntityCommon.telejump(player, deps.collision, exit)
            player.mes("TzHaar-Mej-Jal: You even defeated TzTok-Jad, I am most impressed! Please accept this gift as a reward.")
        }
    }

    private fun payTokkul(player: Player, wavesCompleted: Int, coords: CoordGrid) {
        val tokkul = FightCaveWaves.tokkulFor(wavesCompleted)
        if (tokkul <= 0) return
        player.invAddOrDrop(objRepo, TOKKUL, tokkul, coords = coords)
        if (wavesCompleted < FightCaveWaves.FINAL_WAVE) player.mes("TzHaar-Mej-Jal: Well done in the cave, here, take TokKul as reward.")
    }

    fun summonHealers(run: FightCaveRun, jad: Npc) {
        run.healersSummoned = true
        val needed = JAD_HEALER_COUNT - run.healers.size
        val tiles = healerTiles(jad).shuffled().take(needed)
        for (tile in tiles) {
            val healer = Npc(HEALER, tile)
            deps.npcRepo.add(healer, Int.MAX_VALUE)
            healer.respawns = false
            instances.attachNpc(run.session.id, healer)
            runByNpc[healer] = run
            run.healers += healer
            scheduleHealerTick(run, healer, jad)
        }
    }

    fun distractHealer(run: FightCaveRun, healer: Npc) {
        if (healer in run.healers) run.distractedHealers += healer
    }

    private fun healerTiles(jad: Npc): List<CoordGrid> {
        val size = jad.size
        val ring = buildList {
            for (d in -1..size) {
                add(jad.coords.translate(d, -1))
                add(jad.coords.translate(d, size))
                add(jad.coords.translate(-1, d))
                add(jad.coords.translate(size, d))
            }
        }
        return ring.distinct().filter { !deps.collision.isWalkBlocked(it) }
    }

    private fun scheduleHealerTick(run: FightCaveRun, healer: Npc, jad: Npc) {
        deps.worldQueues.add(1) { tickHealer(run, healer, jad) }
    }

    private fun tickHealer(run: FightCaveRun, healer: Npc, jad: Npc) {
        if (run.finished || !healer.isSlotAssigned || healer.hitpoints <= 0) return
        if (!jad.isSlotAssigned || jad.hitpoints <= 0) return
        if (healer in run.distractedHealers) {
            if (healer.isWithinDistance(run.player, HEALER_LEASH)) {
                scheduleHealerTick(run, healer, jad)
                return
            }
            run.distractedHealers -= healer
            healer.resetMode()
        }
        if (healer.isWithinDistance(jad, 1)) {
            healJad(run, healer, jad)
        } else if (healer.routeDestination.isEmpty()) {
            healer.walk(jad.coords)
        }
        scheduleHealerTick(run, healer, jad)
    }

    private fun healJad(run: FightCaveRun, healer: Npc, jad: Npc) {
        val now = deps.mapClock.cycle
        if (now < (run.nextHealCycle[healer] ?: 0)) return
        run.nextHealCycle[healer] = now + HEALER_HEAL_INTERVAL
        healer.faceNpc(jad)
        healer.anim(HEAL_SEQ)
        jad.spotanim(HEAL_SPOTANIM)
        jad.hitpoints = (jad.hitpoints + HEALER_HEAL_AMOUNT).coerceAtMost(jad.baseHitpointsLvl)
        if (jad.hitpoints >= jad.baseHitpointsLvl) run.healersSummoned = false
    }

    fun woundedNear(mejKot: Npc): Npc? {
        val run = runByNpc[mejKot] ?: return null
        return run.monsters.firstOrNull {
            it.isSlotAssigned && it.hitpoints > 0 && it.hitpoints * 2 < it.baseHitpointsLvl &&
                (it === mejKot || mejKot.isWithinDistance(it, 1))
        }
    }

    fun mejKotHeal(mejKot: Npc) {
        val target = woundedNear(mejKot) ?: return
        mejKot.anim(HEAL_SEQ)
        if (target !== mejKot) mejKot.faceNpc(target)
        target.spotanim(HEAL_SPOTANIM)
        target.hitpoints = (target.hitpoints + MEJKOT_HEAL_AMOUNT).coerceAtMost(target.baseHitpointsLvl)
    }

    private companion object {
        const val START_DELAY = 4
        const val WAVE_DELAY = 8
        const val FINISH_DELAY = 6
        const val RANGED_ATTACK_RANGE = 15
        const val ORANGE_KETZEK_WAVE = 62
        const val ORANGE_KETZEK = "npc.tzhaar_fightcave_swarm_5b"
        const val TZKEK_B = "npc.tzhaar_fightcave_swarm_2b"
        const val SMALL_TZKEK = "npc.tzhaar_fightcave_swarm_2spawn"
        const val HEALER = "npc.tzhaar_fightcave_swarm_boss_cleric"
        const val TOKKUL = "obj.tzhaar_token"
        const val FIRE_CAPE = "obj.tzhaar_cape_fire"
        const val HEAL_SEQ = "seq.lizard_cleric_heal"
        const val HEAL_SPOTANIM = "spotanim.tzhaar_heal"
        const val JAD_HEALER_COUNT = 4
        const val HEALER_HEAL_AMOUNT = 5
        const val HEALER_HEAL_INTERVAL = 4
        const val HEALER_LEASH = 5
        const val MEJKOT_HEAL_AMOUNT = 10

        val SPLIT_OFFSETS = listOf(1 to 0, 0 to 1, -1 to 0, 0 to -1, 1 to 1)
    }
}
