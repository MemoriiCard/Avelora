package org.rsmod.content.bosses.nex

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.interrupt
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.startEncounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Nex
@Inject
constructor(
    private val deps: BossDeps,
    private val fight: NexFight,
    private val ai: AiPlayerInteractions,
) : PluginScript() {
    private val nexId by lazy { NEX.asRSCM(RSCMType.NPC) }
    private val spec = nexSpec()
    private var heartbeat = false

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onLethal = ::nexDefeated, onModifyHit = { modifyNexHit() }, onHit = { checkMageTurn() })
        for (mage in NexMage.entries) {
            BossCombat.register(this, mageSpec(mage), deps, onLethal = { mageDefeated(mage, it) })
        }
        with(deps.extensionRegistry) {
            register(NO_ESCAPE_HANDLER) { _, npc, _, _ -> noEscape(npc) }
            register(VIRUS_HANDLER) { _, npc, target, _ -> virus(npc, target) }
            register(SHADOW_SMASH_HANDLER) { _, npc, _, _ -> shadowSmash(npc) }
            register(SIPHON_HANDLER) { _, npc, _, _ -> bloodSiphon(npc) }
            register(SACRIFICE_HANDLER) { _, npc, target, _ -> bloodSacrifice(npc, target) }
            register(CONTAIN_HANDLER) { _, npc, _, _ -> contain(npc) }
            register(ICE_PRISON_HANDLER) { _, npc, target, _ -> icePrison(npc, target) }
        }
        onEvent<NpcStateEvents.Spawn>(nexId) { deps.worldQueues.add(1) { prepare(npc) } }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id == nexId) deps.worldQueues.add(1) { prepare(npc) } }
    }

    private fun prepare(npc: Npc) {
        fight.reset(npc)
        npc.vars[CALM_ATTACKS_VARN] = 0
        deps.encounter(npc).invulnerable = true
        for (mage in NexMage.entries) {
            val spawned = deps.spawnOwnedNpc(npc, mage.npc, mage.tile) ?: continue
            spawned.movementLocked = true
            fight.mages[mage] = spawned
        }
        if (!heartbeat) {
            heartbeat = true
            beat()
        }
    }

    private fun startIntro() {
        val npc = fight.nex ?: return
        fight.state = NexFight.State.Intro
        deps.suppressAttacks(npc, INTRO_TICKS + 1)
        npc.anim("seq.nex_ready")
        npc.say("AT LAST!")
        NexMage.entries.forEachIndexed { index, mage ->
            deps.worldQueues.add(INTRO_GAP * (index + 1)) {
                if (fight.nex !== npc || fight.state != NexFight.State.Intro) return@add
                npc.say(mage.introShout)
                fight.mages[mage]?.let { npc.faceSquare(it.coords) }
            }
        }
        deps.worldQueues.add(INTRO_TICKS) {
            if (fight.nex !== npc || fight.state != NexFight.State.Intro) return@add
            fight.state = NexFight.State.Fighting
            deps.encounter(npc).invulnerable = false
            npc.resetFaceEntity()
            npc.say(NexPhase.Smoke.shout)
            arenaPlayers().randomOrNull()?.let { npc.opPlayer2(it, ai) }
        }
    }

    private fun beat() {
        deps.worldQueues.add(1) {
            tick()
            beat()
        }
    }

    private fun tick() {
        updateBarrier()
        if (fight.siphonEndsAt in 1..deps.mapClock.cycle) endSiphon()
        if (fight.state == NexFight.State.Idle && arenaPlayers().isNotEmpty()) startIntro()
        if (!fight.inProgress) return
        val players = arenaPlayers()
        if (players.isNotEmpty()) {
            fight.emptyTicks = 0
            if (fight.state == NexFight.State.Fighting) keepTarget(players)
            return
        }
        fight.emptyTicks++
        if (fight.emptyTicks >= EMPTY_RESET_TICKS) resetFight()
    }

    private fun keepTarget(players: List<Player>) {
        val npc = fight.nex ?: return
        val target = deps.encounter(npc).lastTarget
        if (target != null && target in players) return
        val cycle = deps.mapClock.cycle
        if (cycle < fight.retargetAt) return
        fight.retargetAt = cycle + RETARGET_TICKS
        npc.opPlayer2(players.random(), ai)
    }

    private fun updateBarrier() {
        val inside = arenaPlayers().toSet()
        val locked = if (fight.inProgress) inside else emptySet()
        for (player in fight.lockedIn - locked) VarPlayerIntMapSetter.set(player, BARRIER_VARBIT, BARRIER_OPEN)
        for (player in locked - fight.lockedIn) VarPlayerIntMapSetter.set(player, BARRIER_VARBIT, BARRIER_SEALED)
        fight.lockedIn.clear()
        fight.lockedIn += locked
    }

    private fun resetFight() {
        val npc = fight.nex ?: return
        if (!npc.isValidTarget()) return
        deps.interrupt(npc)
        npc.resetTransmog()
        npc.hitpoints = npc.baseHitpointsLvl
        npc.restoreLevels()
        deps.startEncounter(npc, spec)
        PathingEntityCommon.telejump(npc, deps.collision, NexArena.SPAWN)
        prepare(npc)
    }

    private fun NpcHitEvents.Modify.modifyNexHit() {
        if (fight.siphonEndsAt > 0 && hit.damage > 0) {
            npc.heal(hit.damage, showHitsplat = true)
            hit.damage = 0
            return
        }
        val phase = fight.phase
        if (phase.mage == null || fight.mageTurn) return
        val floor = phase.mageThreshold(npc.baseHitpointsLvl)
        if (npc.hitpoints - hit.damage < floor) hit.damage = maxOf(0, npc.hitpoints - floor)
    }

    private fun NpcHitEvents.Impact.checkMageTurn() {
        val phase = fight.phase
        val mage = phase.mage ?: return
        if (fight.state != NexFight.State.Fighting || fight.mageTurn) return
        if (npc.hitpoints > phase.mageThreshold(npc.baseHitpointsLvl)) return
        fight.mageTurn = true
        deps.encounter(npc).invulnerable = true
        npc.say(mage.turnShout)
        val mageNpc = fight.mages[mage] ?: return
        val target = deps.encounter(npc).lastTarget?.takeIf { it.isValidTarget() } ?: arenaPlayers().randomOrNull()
        target?.let { mageNpc.opPlayer2(it, ai) }
    }

    private fun mageDefeated(mage: NexMage, mageNpc: Npc) {
        if (fight.mages[mage] !== mageNpc || fight.phase.mage != mage) return
        deps.worldQueues.add(1) { advancePhase() }
    }

    private fun advancePhase() {
        val npc = fight.nex ?: return
        val next = fight.phase.next ?: return
        if (!npc.isValidTarget()) return
        fight.phase = next
        fight.mageTurn = false
        val encounter = deps.encounter(npc)
        encounter.transitionTo(next.key, deps.mapClock.cycle)
        encounter.invulnerable = false
        npc.say(next.shout)
        if (next == NexPhase.Zaros) {
            npc.anim("seq.nex_turmoil")
            npc.spotanim("spotanim.nex_turmoil")
            npc.heal(ZAROS_HEAL, showHitsplat = true)
        }
    }

    private fun nexDefeated(npc: Npc) {
        fight.state = NexFight.State.Dead
        npc.say("Taste my wrath!")
        val blast = footprint(npc.coords, NEX_SIZE, margin = DEATH_BLAST_RADIUS)
        deps.worldQueues.add(DEATH_BLAST_TICKS) {
            val spot = spotanim("spotanim.nex_mushroom_cloud_spotanim")
            deps.worldRepo.spotanimMap(spot, npc.coords.translate(1, 1))
            for (player in arenaPlayers()) {
                if (player.coords in blast) hitTypeless(npc, player, DEATH_BLAST_MAX_HIT)
            }
        }
    }

    private fun noEscape(npc: Npc) {
        val dash = NexArena.DASH_ENDS.random()
        npc.say("There is...")
        deps.suppressAttacks(npc, NO_ESCAPE_TICKS + 2)
        deps.worldQueues.add(NO_ESCAPE_WINDUP) {
            if (!npc.isValidTarget()) return@add
            PathingEntityCommon.telejump(npc, deps.collision, dash.start)
            npc.say("NO ESCAPE!")
            npc.anim("seq.nex_dash_attack")
        }
        deps.worldQueues.add(NO_ESCAPE_TICKS) {
            if (!npc.isValidTarget()) return@add
            PathingEntityCommon.telejump(npc, deps.collision, dash.end)
            val path = dash.path(NEX_SIZE)
            for (player in arenaPlayers()) {
                if (player.coords in path) hitTypeless(npc, player, NO_ESCAPE_MAX_HIT)
            }
        }
    }

    private fun virus(npc: Npc, target: Player) {
        npc.say("Let the virus flow through you!")
        infect(npc, target)
    }

    private fun infect(npc: Npc, player: Player) {
        if (!fight.infected.add(player)) return
        repeat(VIRUS_COUGHS) { index ->
            deps.worldQueues.add(VIRUS_COUGH_GAP * (index + 1)) {
                if (!NexArena.contains(player.coords) || player.hitpoints <= 0) return@add
                player.say("*Cough*")
                CombatEffects.statDrain(player, VIRUS_DRAINED_STATS, VIRUS_STAT_DRAIN)
                player.statSub("stat.prayer", constant = VIRUS_PRAYER_DRAIN, percent = 0)
                arenaPlayers()
                    .filter { it !== player && it.coords.chebyshevDistance(player.coords) <= 1 }
                    .forEach { infect(npc, it) }
                if (index == VIRUS_COUGHS - 1) fight.infected -= player
            }
        }
    }

    private fun shadowSmash(npc: Npc) {
        npc.say("Fear the shadow!")
        val tiles = arenaPlayers().map { it.coords }.distinct()
        tiles.forEach { addLoc(it, "loc.nex_shadow_smash", SHADOW_SMASH_TICKS + 1) }
        deps.worldQueues.add(SHADOW_SMASH_TICKS) {
            if (!npc.isValidTarget()) return@add
            for (player in arenaPlayers()) {
                if (player.coords in tiles) hitTypeless(npc, player, SHADOW_SMASH_MAX_HIT)
            }
        }
    }

    private fun bloodSiphon(npc: Npc) {
        npc.say("A siphon will solve this!")
        npc.spotanim("spotanim.nex_blood_siphon")
        fight.siphonEndsAt = deps.mapClock.cycle + SIPHON_TICKS
        deps.suppressAttacks(npc, SIPHON_TICKS)
        val players = arenaPlayers()
        val count = players.size.coerceIn(1, MAX_REAVERS)
        val tiles = ring(npc.coords, NEX_SIZE, margin = 2).shuffled().take(count)
        for (tile in tiles) {
            val reaver = deps.spawnOwnedNpc(npc, REAVER, tile) ?: continue
            fight.reavers += reaver
            players.randomOrNull()?.let { reaver.opPlayer2(it, ai) }
        }
    }

    private fun endSiphon() {
        fight.siphonEndsAt = 0
        val npc = fight.nex
        val alive = fight.reavers.filter { it.isValidTarget() }
        fight.reavers.clear()
        if (npc == null || !npc.isValidTarget()) return
        val heal = alive.sumOf { it.hitpoints }
        alive.forEach { deps.npcRepo.del(it, Int.MAX_VALUE) }
        if (heal > 0) npc.heal(heal, showHitsplat = true)
    }

    private fun bloodSacrifice(npc: Npc, target: Player) {
        npc.say("I demand a blood sacrifice!")
        target.mes("<col=ef1020>Nex has marked you for a blood sacrifice! RUN!")
        target.spotanim("spotanim.nex_blood_siphon")
        deps.worldQueues.add(SACRIFICE_TICKS) {
            if (!npc.isValidTarget() || target.hitpoints <= 0 || !NexArena.contains(target.coords)) return@add
            val centre = npc.coords.translate(1, 1)
            if (target.coords.chebyshevDistance(centre) > SACRIFICE_RANGE) {
                target.mes("You escape the blood sacrifice.")
                return@add
            }
            target.mes("You didn't make it far enough in time - Nex feeds on your blood!")
            target.statSub("stat.prayer", constant = 0, percent = SACRIFICE_PRAYER_PERCENT)
            val damage = deps.random.of(1, SACRIFICE_MAX_HIT)
            target.queueHit(npc, 0, HitType.Typeless, damage, deps.playerHitModifier)
            npc.heal(damage, showHitsplat = true)
        }
    }

    private fun contain(npc: Npc) {
        npc.say("Contain this!")
        val inner = footprint(npc.coords, NEX_SIZE, margin = 1)
        ring(npc.coords, NEX_SIZE, margin = 2).forEach { addLoc(it, "loc.nex_icicle_1", CONTAIN_TICKS + 2) }
        deps.worldQueues.add(CONTAIN_TICKS) {
            if (!npc.isValidTarget()) return@add
            for (player in arenaPlayers()) {
                if (player.coords !in inner) continue
                hitTypeless(npc, player, CONTAIN_MAX_HIT)
                CombatEffects.freeze(player, CONTAIN_FREEZE_TICKS)
            }
        }
    }

    private fun icePrison(npc: Npc, target: Player) {
        npc.say("Die now, in a prison of ice!")
        val centre = target.coords
        val travel = PRISON_LAND_TICKS * CYCLES_PER_TICK
        deps.bossProjectile(spotanim(PRISON_PROJ).id, npc.coords.translate(1, 1), centre, 43, 0, 0, travel, 16)
        val cell = footprint(centre, 1, margin = 1)
        deps.worldQueues.add(PRISON_LAND_TICKS) {
            if (!npc.isValidTarget()) return@add
            ring(centre, 1, margin = 1).forEach { addLoc(it, "loc.nex_icicle_1", PRISON_TICKS + 1) }
            if (target.coords == centre) CombatEffects.freeze(target, PRISON_TICKS)
        }
        deps.worldQueues.add(PRISON_LAND_TICKS + PRISON_TICKS) {
            if (!npc.isValidTarget()) return@add
            for (player in arenaPlayers()) {
                if (player.coords in cell) hitTypeless(npc, player, PRISON_MAX_HIT)
            }
        }
    }

    private fun addLoc(tile: CoordGrid, loc: String, duration: Int) {
        if (deps.collision.isWalkBlocked(tile)) return
        deps.locRepo.add(tile, loc, duration, LocAngle[0], LocShape.CentrepieceStraight)
    }

    private fun hitTypeless(npc: Npc, player: Player, maxHit: Int) {
        player.queueHit(npc, 0, HitType.Typeless, deps.random.of(1, maxHit), deps.playerHitModifier)
    }

    private fun footprint(sw: CoordGrid, size: Int, margin: Int): Set<CoordGrid> {
        val tiles = mutableSetOf<CoordGrid>()
        for (dx in -margin until size + margin) {
            for (dz in -margin until size + margin) tiles += sw.translate(dx, dz)
        }
        return tiles
    }

    private fun ring(sw: CoordGrid, size: Int, margin: Int): List<CoordGrid> =
        (footprint(sw, size, margin) - footprint(sw, size, margin - 1))
            .filter { NexArena.contains(it) && !deps.collision.isWalkBlocked(it) }

    private fun arenaPlayers(): List<Player> =
        deps.playerList.filter { NexArena.contains(it.coords) && it.hitpoints > 0 }

    private fun Npc.restoreLevels() {
        attackLvl = baseAttackLvl
        strengthLvl = baseStrengthLvl
        defenceLvl = baseDefenceLvl
        rangedLvl = baseRangedLvl
        magicLvl = baseMagicLvl
    }

    private fun spotanim(name: String) = SpotanimType(name.asRSCM(RSCMType.SPOTANIM))

    private companion object {
        const val NEX_SIZE = 3
        const val CYCLES_PER_TICK = 30
        const val INTRO_GAP = 2
        const val INTRO_TICKS = 10
        const val EMPTY_RESET_TICKS = 10
        const val RETARGET_TICKS = 5
        const val ZAROS_HEAL = 500

        const val BARRIER_VARBIT = "varbit.nex_barrier"
        const val BARRIER_OPEN = 0
        const val BARRIER_SEALED = 2

        const val DEATH_BLAST_TICKS = 3
        const val DEATH_BLAST_RADIUS = 2
        const val DEATH_BLAST_MAX_HIT = 40

        const val NO_ESCAPE_WINDUP = 2
        const val NO_ESCAPE_TICKS = 4
        const val NO_ESCAPE_MAX_HIT = 50

        const val VIRUS_COUGHS = 5
        const val VIRUS_COUGH_GAP = 2
        const val VIRUS_STAT_DRAIN = 1
        const val VIRUS_PRAYER_DRAIN = 2
        val VIRUS_DRAINED_STATS = listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")

        const val SHADOW_SMASH_TICKS = 3
        const val SHADOW_SMASH_MAX_HIT = 30

        const val REAVER = "npc.nex_prison_blood_reaver_boss"
        const val MAX_REAVERS = 3
        const val SIPHON_TICKS = 8

        const val SACRIFICE_TICKS = 7
        const val SACRIFICE_RANGE = 7
        const val SACRIFICE_PRAYER_PERCENT = 33
        const val SACRIFICE_MAX_HIT = 50

        const val CONTAIN_TICKS = 3
        const val CONTAIN_MAX_HIT = 60
        const val CONTAIN_FREEZE_TICKS = 5

        const val PRISON_PROJ = "spotanim.nex_ice_prison_proj"
        const val PRISON_LAND_TICKS = 2
        const val PRISON_TICKS = 5
        const val PRISON_MAX_HIT = 70
    }
}
