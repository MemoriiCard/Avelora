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
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

internal var Player.infernoWave by intVarp("varp.inferno_wave")

internal class InfernoRun(val player: Player, val session: InstanceSession) {
    var wave: Int = 0
    var finished: Boolean = false
    val monsters: MutableSet<Npc> = identitySet()
    val healers: MutableSet<Npc> = identitySet()
    val distractedHealers: MutableSet<Npc> = identitySet()
    val healedJads: MutableSet<Npc> = identitySet()
    val nextHeal: MutableMap<Npc, Int> = IdentityHashMap()
    var zuk: Npc? = null
    var glyph: Npc? = null
    var shield: ZukShield? = null
}

private fun identitySet(): MutableSet<Npc> = java.util.Collections.newSetFromMap(IdentityHashMap())

@Singleton
internal class InfernoRuns
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val objRepo: ObjRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val pets: PetRewards,
) {
    private val runs = HashMap<InstanceId, InfernoRun>()
    private val runByNpc = IdentityHashMap<Npc, InfernoRun>()

    fun runFor(session: InstanceSession): InfernoRun? = runs[session.id]

    fun start(player: Player, session: InstanceSession) {
        val saved = player.infernoWave
        val resuming = saved in 1..InfernoWaves.ZUK_WAVE
        val run = InfernoRun(player, session)
        runs[session.id] = run
        if (resuming && saved > 1) player.mes("Your Inferno run resumes at wave $saved.")
        scheduleWave(run, if (resuming) saved else 1, START_DELAY)
    }

    fun end(id: InstanceId) {
        val run = runs.remove(id) ?: return
        run.finished = true
        (run.monsters + run.healers + listOfNotNull(run.glyph)).forEach { runByNpc.remove(it) }
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
        if (wave == InfernoWaves.ZUK_WAVE) {
            spawnZuk(run)
            return
        }
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
        run.healers.remove(npc)
        run.distractedHealers.remove(npc)
        if (run.finished) return
        if (npc === run.zuk) {
            defeatZuk(run)
            return
        }
        if (!run.monsters.remove(npc)) return
        if (npc.type.internalName == InfernoMonster.Blob.npc) splitBlob(run, coords)
        if (run.monsters.isEmpty() && !run.finished) scheduleWave(run, run.wave + 1, WAVE_DELAY)
    }

    private fun splitBlob(run: InfernoRun, coords: CoordGrid) {
        val tiles = SPLIT_OFFSETS.map { coords.translate(it.first, it.second) }
        val free = tiles.filter { !deps.collision.isWalkBlocked(it) }.ifEmpty { listOf(coords) }
        for ((index, type) in MINI_BLOBS.withIndex()) {
            run.monsters += spawn(run, type, free[index % free.size])
        }
    }

    private fun spawnZuk(run: InfernoRun) {
        val spawn = instances.resolveCoord(run.session, ZukStats.SPAWN) ?: return
        val zuk = spawn(run, ZukStats.NPC, spawn)
        run.zuk = zuk
        run.monsters += zuk
        val shield = ZukShield(deps.mapClock.cycle, spawn.x - ZukStats.SPAWN.x, spawn.z - ZukStats.SPAWN.z)
        run.shield = shield
        val glyph = Npc(ZukStats.GLYPH, shield.glyphCoords(deps.mapClock.cycle))
        deps.npcRepo.add(glyph, Int.MAX_VALUE)
        glyph.respawns = false
        instances.attachNpc(run.session.id, glyph)
        runByNpc[glyph] = run
        run.glyph = glyph
        deps.worldQueues.add(ZukArena.GLYPH_STEP_TICKS) { tickGlyph(run) }
    }

    private fun tickGlyph(run: InfernoRun) {
        val glyph = run.glyph ?: return
        val shield = run.shield ?: return
        if (run.finished || shield.broken || !glyph.isSlotAssigned) return
        PathingEntityCommon.telejump(glyph, deps.collision, shield.glyphCoords(deps.mapClock.cycle))
        deps.worldQueues.add(ZukArena.GLYPH_STEP_TICKS) { tickGlyph(run) }
    }

    fun zukDamage(zuk: Npc, target: Player): Int {
        val damage = deps.random.of(ZukStats.MAX_HIT + 1)
        val run = runByNpc[zuk] ?: return damage
        val shield = run.shield ?: return damage
        if (!shield.covers(target.coords.x, target.coords.z, deps.mapClock.cycle)) return damage
        shield.absorb(damage)
        if (shield.broken) breakShield(run)
        return 0
    }

    private fun breakShield(run: InfernoRun) {
        run.glyph?.let {
            runByNpc.remove(it)
            if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE)
        }
        run.glyph = null
        run.player.mes("<col=ef1020>The Ancestral Glyph shatters.</col>")
    }

    fun zukSet(zuk: Npc) {
        val run = runByNpc[zuk]?.takeIf { !it.finished } ?: return
        if (run.monsters.size > MAX_ZUK_MINIONS) return
        for ((index, type) in ZUK_SET.withIndex()) {
            val coords =
                instances.resolveCoord(run.session, InfernoArena.SPAWN_POINTS[index % ZUK_SET_POINTS]) ?: continue
            run.monsters += spawn(run, type, coords)
        }
    }

    fun zukJad(zuk: Npc) {
        val run = runByNpc[zuk]?.takeIf { !it.finished } ?: return
        val coords = instances.resolveCoord(run.session, InfernoArena.SPAWN_POINTS.last()) ?: return
        run.monsters += spawn(run, InfernoMonster.Jad.npc, coords)
    }

    fun zukHealers(zuk: Npc) {
        val run = runByNpc[zuk]?.takeIf { !it.finished } ?: return
        for (tile in ZukArena.HEALER_TILES.take(ZukStats.HEALER_COUNT)) {
            val coords = instances.resolveCoord(run.session, tile) ?: continue
            val healer = spawnHealer(run, ZukStats.HEALER, coords)
            healer.apRangeOverride = RANGED_ATTACK_RANGE
            scheduleHealer(run, healer, zuk, ZUK_HEAL_REACH, ZukStats.ZUK_HEAL_AMOUNT, ZukStats.ZUK_HEAL_INTERVAL)
        }
    }

    fun summonJadHealers(jad: Npc) {
        val run = runByNpc[jad] ?: return
        if (run.finished || !run.healedJads.add(jad)) return
        for (tile in healerTiles(jad).shuffled().take(JAD_HEALER_COUNT)) {
            val healer = spawnHealer(run, ZukStats.JAD_HEALER, tile)
            scheduleHealer(run, healer, jad, 1, JAD_HEAL_AMOUNT, JAD_HEAL_INTERVAL)
        }
    }

    fun distractHealer(healer: Npc) {
        val run = runByNpc[healer] ?: return
        if (healer in run.healers) run.distractedHealers += healer
    }

    private fun spawnHealer(run: InfernoRun, type: String, coords: CoordGrid): Npc {
        val healer = Npc(type, coords)
        deps.npcRepo.add(healer, Int.MAX_VALUE)
        healer.respawns = false
        instances.attachNpc(run.session.id, healer)
        runByNpc[healer] = run
        run.healers += healer
        return healer
    }

    private fun healerTiles(jad: Npc): List<CoordGrid> {
        val size = jad.size
        val ring =
            buildList {
                for (d in -1..size) {
                    add(jad.coords.translate(d, -1))
                    add(jad.coords.translate(d, size))
                    add(jad.coords.translate(-1, d))
                    add(jad.coords.translate(size, d))
                }
            }
        return ring.distinct().filter { !deps.collision.isWalkBlocked(it) }
    }

    private fun scheduleHealer(run: InfernoRun, healer: Npc, patient: Npc, reach: Int, amount: Int, interval: Int) {
        deps.worldQueues.add(1) { tickHealer(run, healer, patient, reach, amount, interval) }
    }

    private fun tickHealer(run: InfernoRun, healer: Npc, patient: Npc, reach: Int, amount: Int, interval: Int) {
        if (run.finished || !healer.isSlotAssigned || healer.hitpoints <= 0) return
        if (!patient.isSlotAssigned || patient.hitpoints <= 0) return
        if (healer in run.distractedHealers) {
            val permanent = patient === run.zuk
            if (permanent || healer.isWithinDistance(run.player, HEALER_LEASH)) {
                scheduleHealer(run, healer, patient, reach, amount, interval)
                return
            }
            run.distractedHealers -= healer
            healer.resetMode()
        }
        if (healer.isWithinDistance(patient, reach)) {
            heal(run, healer, patient, amount, interval)
        } else if (healer.routeDestination.isEmpty()) {
            healer.walk(patient.coords)
        }
        scheduleHealer(run, healer, patient, reach, amount, interval)
    }

    private fun heal(run: InfernoRun, healer: Npc, patient: Npc, amount: Int, interval: Int) {
        val now = deps.mapClock.cycle
        if (now < (run.nextHeal[healer] ?: 0)) return
        run.nextHeal[healer] = now + interval
        healer.faceNpc(patient)
        healer.anim(HEAL_SEQ)
        patient.spotanim(HEAL_SPOTANIM)
        patient.hitpoints = (patient.hitpoints + amount).coerceAtMost(patient.baseHitpointsLvl)
    }

    private fun defeatZuk(run: InfernoRun) {
        run.finished = true
        val player = run.player
        (run.monsters + run.healers + listOfNotNull(run.glyph)).forEach {
            runByNpc.remove(it)
            if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE)
        }
        run.monsters.clear()
        run.healers.clear()
        run.glyph = null
        player.infernoWave = 0
        player.invAddOrDrop(objRepo, ZukStats.CAPE, 1, coords = InfernoArena.OUTSIDE)
        player.invAddOrDrop(objRepo, TOKKUL, ZukStats.TOKKUL, coords = InfernoArena.OUTSIDE)
        player.mes("<col=ef1020>Congratulations! You have defeated TzKal-Zuk and completed the Inferno.</col>")
        if (deps.random.of(ZukStats.PET_ODDS) == 0) pets.give(player, ZukStats.PET)
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
        const val MAX_ZUK_MINIONS = 10
        const val ZUK_SET_POINTS = 2
        const val ZUK_HEAL_REACH = 15
        const val HEALER_LEASH = 5
        const val JAD_HEALER_COUNT = 3
        const val JAD_HEAL_AMOUNT = 5
        const val JAD_HEAL_INTERVAL = 4
        const val HEAL_SEQ = "seq.lizard_cleric_heal"
        const val HEAL_SPOTANIM = "spotanim.tzhaar_heal"
        val ZUK_SET = listOf(InfernoMonster.Ranger.npc, InfernoMonster.Mager.npc)

        val RANGED_TYPES =
            setOf(
                InfernoMonster.Bat.npc,
                InfernoMonster.Ranger.npc,
                InfernoMonster.Mager.npc,
                InfernoMonster.Jad.npc,
                ZukStats.NPC,
                BLOB_MAGE,
                BLOB_RANGE,
            )
        val MINI_BLOBS = listOf(BLOB_MELEE, BLOB_RANGE, BLOB_MAGE)
        val SPLIT_OFFSETS = listOf(0 to 0, 1 to 0, 0 to 1, -1 to 0, 0 to -1)
    }
}
