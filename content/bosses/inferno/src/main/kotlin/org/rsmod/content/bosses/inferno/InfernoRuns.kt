package org.rsmod.content.bosses.inferno

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

internal var Player.infernoWave by intVarp("varp.inferno_wave")

internal class InfernoRun(val player: Player, val session: InstanceSession) {
    var wave: Int = 0
    var finished: Boolean = false
    val monsters: MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap())
}

@Singleton
internal class InfernoRuns
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val objRepo: ObjRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val runs = HashMap<InstanceId, InfernoRun>()
    private val runByNpc = IdentityHashMap<Npc, InfernoRun>()

    fun runFor(session: InstanceSession): InfernoRun? = runs[session.id]

    fun start(player: Player, session: InstanceSession) {
        val saved = player.infernoWave
        val resuming = saved in 1..InfernoWaves.FINAL_WAVE
        val run = InfernoRun(player, session)
        runs[session.id] = run
        if (resuming && saved > 1) player.mes("Your Inferno run resumes at wave $saved.")
        scheduleWave(run, if (resuming) saved else 1, START_DELAY)
    }

    fun end(id: InstanceId) {
        val run = runs.remove(id) ?: return
        run.finished = true
        run.monsters.forEach { runByNpc.remove(it) }
    }

    fun forfeit(player: Player, run: InfernoRun, rewardCoords: CoordGrid) {
        val completed = (run.wave - 1).coerceAtLeast(0)
        player.infernoWave = 0
        end(run.session.id)
        payTokkul(player, completed, rewardCoords)
    }

    private fun scheduleWave(run: InfernoRun, wave: Int, delay: Int) {
        deps.worldQueues.add(delay) {
            if (runs[run.session.id] !== run || run.finished) return@add
            spawnWave(run, wave)
        }
    }

    private fun spawnWave(run: InfernoRun, wave: Int) {
        run.wave = wave
        run.player.infernoWave = wave
        run.player.mes("<col=ef1020>Wave: $wave</col>")
        for ((index, monster) in InfernoWaves.wave(wave).withIndex()) {
            val coords = instances.resolveCoord(run.session, InfernoArena.spawnPoint(wave, index)) ?: continue
            run.monsters += spawn(run, monster.npc, coords)
        }
    }

    private fun spawn(run: InfernoRun, type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        instances.attachNpc(run.session.id, npc)
        runByNpc[npc] = run
        if (type in RANGED_TYPES) {
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
        if (!run.monsters.remove(npc)) return
        if (npc.type.internalName == InfernoMonster.Blob.npc) splitBlob(run, coords)
        if (run.monsters.isEmpty() && !run.finished) {
            if (run.wave >= InfernoWaves.FINAL_WAVE) finish(run) else scheduleWave(run, run.wave + 1, WAVE_DELAY)
        }
    }

    private fun splitBlob(run: InfernoRun, coords: CoordGrid) {
        val tiles = SPLIT_OFFSETS.map { coords.translate(it.first, it.second) }
        val free = tiles.filter { !deps.collision.isWalkBlocked(it) }.ifEmpty { listOf(coords) }
        for ((index, type) in MINI_BLOBS.withIndex()) {
            run.monsters += spawn(run, type, free[index % free.size])
        }
    }

    private fun finish(run: InfernoRun) {
        run.finished = true
        val player = run.player
        player.infernoWave = 0
        payTokkul(player, InfernoWaves.FINAL_WAVE, InfernoArena.OUTSIDE)
        player.mes("You have cleared every wave. TzKal-Zuk is not yet awake.")
        deps.worldQueues.add(FINISH_DELAY) {
            if (instances.sessionForPlayer(player)?.id != run.session.id) return@add
            val exit = instances.leave(player, run.session, deps.mapClock.cycle)
            PathingEntityCommon.telejump(player, deps.collision, exit)
        }
    }

    private fun payTokkul(player: Player, wavesCompleted: Int, coords: CoordGrid) {
        val tokkul = InfernoWaves.tokkulFor(wavesCompleted)
        if (tokkul <= 0) return
        player.invAddOrDrop(objRepo, TOKKUL, tokkul, coords = coords)
        player.mes("TzHaar-Ket-Keh: You fought well. Take TokKul as your reward.")
    }

    private companion object {
        const val START_DELAY = 4
        const val WAVE_DELAY = 8
        const val FINISH_DELAY = 6
        const val RANGED_ATTACK_RANGE = 15
        const val TOKKUL = "obj.tzhaar_token"

        val RANGED_TYPES =
            setOf(
                InfernoMonster.Bat.npc,
                InfernoMonster.Ranger.npc,
                InfernoMonster.Mager.npc,
                InfernoMonster.Jad.npc,
                BLOB_MAGE,
                BLOB_RANGE,
            )
        val MINI_BLOBS = listOf(BLOB_MELEE, BLOB_RANGE, BLOB_MAGE)
        val SPLIT_OFFSETS = listOf(0 to 0, 1 to 0, 0 to 1, -1 to 0, 0 to -1)
    }
}
