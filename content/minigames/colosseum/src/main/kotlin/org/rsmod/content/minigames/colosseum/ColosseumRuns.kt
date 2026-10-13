package org.rsmod.content.minigames.colosseum

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
import org.rsmod.map.CoordGrid

internal var Player.colosseumGlory by intVarp("varp.colosseum_glory")
internal var Player.colosseumWavesCompleted by intVarp("varp.total_colosseum_waves_completed")

internal enum class RunState {
    Waiting,
    Fighting,
    Done,
}

internal class ColosseumRun(val player: Player, val session: InstanceSession) {
    var state = RunState.Waiting
    var cleared = 0
    var glory = 0
    var reinforcementsPending = false
    val modifiers: MutableSet<ColosseumModifier> = linkedSetOf()
    val pool: MutableList<ColosseumRewards.Reward> = ArrayList()
    val monsters: MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap())

    val nextWave: Int get() = cleared + 1
}

@Singleton
internal class ColosseumRuns
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val objRepo: ObjRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val runs = HashMap<InstanceId, ColosseumRun>()
    private val runByNpc = IdentityHashMap<Npc, ColosseumRun>()

    fun runFor(session: InstanceSession): ColosseumRun? = runs[session.id]

    fun start(player: Player, session: InstanceSession) {
        runs[session.id] = ColosseumRun(player, session)
        player.mes("Speak to Minimus when you are ready to begin.")
    }

    fun end(id: InstanceId) {
        val run = runs.remove(id) ?: return
        run.state = RunState.Done
        run.monsters.forEach { runByNpc.remove(it) }
    }

    fun offers(run: ColosseumRun): List<ColosseumModifier> =
        if (run.nextWave > ColosseumWaves.FINAL_REGULAR_WAVE) {
            emptyList()
        } else {
            ColosseumModifier.offers(run.modifiers) { deps.random.of(it) }
        }

    fun beginWave(run: ColosseumRun, modifier: ColosseumModifier?) {
        if (run.state != RunState.Waiting) return
        if (modifier != null) run.modifiers += modifier
        run.state = RunState.Fighting
        val wave = run.nextWave
        run.player.mes("<col=ef1020>Wave: $wave</col>")
        if (wave == ColosseumWaves.SOL_WAVE) {
            spawnAt(run, ColosseumMonster.Sol, ColosseumArena.SOL)
            return
        }
        val quartet = ColosseumModifier.Quartet in run.modifiers
        val duo = ColosseumModifier.DynamicDuo in run.modifiers
        val main = ColosseumWaves.main(wave, quartet, duo)
        for ((index, monster) in main.withIndex()) {
            spawnAt(run, monster, ColosseumArena.spawnPoint(wave, index))
        }
        run.reinforcementsPending = ColosseumWaves.reinforcements(wave).isNotEmpty()
        if (run.reinforcementsPending) {
            deps.worldQueues.add(ColosseumWaves.REINFORCEMENT_TICKS) { spawnReinforcements(run, wave) }
        }
    }

    private fun spawnReinforcements(run: ColosseumRun, wave: Int) {
        if (runs[run.session.id] !== run || run.state != RunState.Fighting || run.cleared + 1 != wave) return
        run.reinforcementsPending = false
        for ((index, monster) in ColosseumWaves.reinforcements(wave).withIndex()) {
            spawnAt(run, monster, ColosseumArena.gate(index))
        }
        run.player.mes("Reinforcements pour into the arena.")
    }

    private fun spawnAt(run: ColosseumRun, monster: ColosseumMonster, point: CoordGrid) {
        val coords = instances.resolveCoord(run.session, point) ?: return
        val type =
            if (monster == ColosseumMonster.Minotaur && ColosseumModifier.RedFlag in run.modifiers) {
                ColosseumMonster.MinotaurRoute.npc
            } else {
                monster.npc
            }
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        instances.attachNpc(run.session.id, npc)
        runByNpc[npc] = run
        run.monsters += npc
        if (type in RANGED_TYPES) {
            npc.apRangeOverride = RANGED_ATTACK_RANGE
            npc.apPlayer2(run.player, aiPlayerInteractions)
        } else {
            npc.opPlayer2(run.player, aiPlayerInteractions)
        }
    }

    suspend fun onDeath(access: StandardNpcAccess, monster: ColosseumMonster) {
        val npc = access.npc
        access.noneMode()
        access.hideAllOps()
        access.anim(monster.deathSeq)
        access.delay(monster.deathTicks)
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        val run = runByNpc.remove(npc) ?: return
        if (run.state != RunState.Fighting) return
        if (!run.monsters.remove(npc)) return
        if (run.monsters.isEmpty() && !run.reinforcementsPending) waveCleared(run)
    }

    private fun waveCleared(run: ColosseumRun) {
        val wave = run.nextWave
        run.cleared = wave
        run.glory += ColosseumRewards.glory(wave, run.modifiers)
        run.pool += ColosseumRewards.forWave(wave) { deps.random.of(it) }
        val player = run.player
        if (wave == ColosseumWaves.SOL_WAVE) {
            player.mes("<col=00ff00>Sol Heredit has fallen.</col>")
            run.pool += ColosseumRewards.completion { deps.random.of(it) }
            claim(run)
            return
        }
        run.state = RunState.Waiting
        player.mes("Wave $wave complete. Glory: ${run.glory}. Speak to Minimus.")
    }

    fun claim(run: ColosseumRun) {
        if (run.state == RunState.Done) return
        val player = run.player
        run.state = RunState.Done
        payOut(run, player)
        leave(run, player)
    }

    fun forfeit(player: Player, run: ColosseumRun) {
        if (run.state == RunState.Done) return
        run.state = RunState.Done
        player.mes("You have fallen. Your unclaimed rewards are lost.")
        end(run.session.id)
    }

    private fun payOut(run: ColosseumRun, player: Player) {
        for (reward in run.pool) {
            player.invAddOrDrop(objRepo, reward.obj, reward.count, coords = ColosseumArena.LOBBY)
        }
        player.colosseumWavesCompleted += run.cleared
        if (run.glory > player.colosseumGlory) player.colosseumGlory = run.glory
        if (run.cleared > 0) player.mes("Minimus hands over your rewards. Glory earned: ${run.glory}.")
    }

    private fun leave(run: ColosseumRun, player: Player) {
        end(run.session.id)
        deps.worldQueues.add(1) {
            if (instances.sessionForPlayer(player)?.id != run.session.id) return@add
            val exit = instances.leave(player, run.session, deps.mapClock.cycle)
            PathingEntityCommon.telejump(player, deps.collision, exit)
        }
    }

    private companion object {
        const val RANGED_ATTACK_RANGE = 12

        val RANGED_TYPES =
            setOf(
                ColosseumMonster.Seer.npc,
                ColosseumMonster.Archer.npc,
                ColosseumMonster.Shaman.npc,
                ColosseumMonster.Javelin.npc,
                ColosseumMonster.Manticore.npc,
            )
    }
}
