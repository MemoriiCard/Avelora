package org.rsmod.content.bosses.nightmare

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Ranged
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
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
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

class Nightmare
@Inject
constructor(
    private val deps: BossDeps,
    private val fight: NightmareFight,
    private val ai: AiPlayerInteractions,
    private val routeFactory: RouteFactory,
    private val npcHitModifier: StandardNpcHitModifier,
) : PluginScript() {
    private val nightmareId by lazy { NIGHTMARE.asRSCM(RSCMType.NPC) }
    private val entryId by lazy { ENTRY.asRSCM(RSCMType.NPC) }
    private val spec = nightmareSpec()
    private var heartbeat = false

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onLethal = { fight.state = NightmareFight.State.Dead }, onModifyHit = { modifyNightmareHit() })
        BossCombat.register(this, huskSpec(HUSK_MAGIC, "spotanim.nightmare_husk_magic_travel", Magic), deps)
        BossCombat.register(this, huskSpec(HUSK_RANGED, "spotanim.nightmare_husk_ranged_travel", Ranged), deps)
        with(deps.extensionRegistry) {
            register(CLAWS_HANDLER) { _, npc, _, _ -> graspingClaws(npc) }
            register(HUSKS_HANDLER) { _, npc, target, _ -> husks(npc, target) }
            register(PARASITE_HANDLER) { _, npc, _, _ -> parasites(npc) }
            register(SURGE_HANDLER) { _, npc, target, _ -> surge(npc, target) }
            register(SPORES_HANDLER) { _, npc, _, _ -> spores(npc) }
        }
        for (totem in NightmareTotem.entries) {
            onEvent<NpcStateEvents.Spawn>(totem.dormant.asRSCM(RSCMType.NPC)) { fight.totems[totem] = npc }
            for (form in listOf(totem.ready, totem.charged)) {
                val type = checkNotNull(ServerCacheManager.getNpc(form.asRSCM(RSCMType.NPC))) { form }
                onModifyNpcHit(type) { chargeTotem(totem) }
            }
        }
        onEvent<NpcStateEvents.Spawn>(entryId) { fight.entry = npc }
        onEvent<NpcStateEvents.Spawn>(nightmareId) { deps.worldQueues.add(1) { prepare(npc) } }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id == nightmareId) deps.worldQueues.add(1) { prepare(npc) } }
    }

    private fun prepare(npc: Npc) {
        fight.nightmare = npc
        fight.state = NightmareFight.State.Idle
        fight.phase = NightmarePhase.One
        fight.clearMechanics()
        npc.vars[CALM_ATTACKS_VARN] = 0
        deps.encounter(npc).invulnerable = true
        resetTotems()
        fight.entry?.resetTransmog()
        if (!heartbeat) {
            heartbeat = true
            beat()
        }
    }

    private fun beat() {
        deps.worldQueues.add(1) {
            tick()
            beat()
        }
    }

    private fun tick() {
        val npc = fight.nightmare ?: return
        val cycle = deps.mapClock.cycle
        if (fight.state == NightmareFight.State.Lobby && cycle >= fight.lobbyEndsAt) awaken(npc)
        if (!fight.inProgress) return
        val players = arenaPlayers()
        if (players.isEmpty()) {
            if (++fight.emptyTicks >= EMPTY_RESET_TICKS) resetFight(npc)
            return
        }
        fight.emptyTicks = 0
        releaseBound()
        breatheSpores(players, cycle)
        if (fight.state == NightmareFight.State.Sleepwalkers) stepSleepwalkers(npc, players, cycle)
        keepTarget(npc, players, cycle)
    }

    private fun awaken(npc: Npc) {
        val players = arenaPlayers()
        if (players.isEmpty()) {
            fight.state = NightmareFight.State.Idle
            fight.entry?.resetTransmog()
            return
        }
        npc.anim("seq.nightmare_spawn_initial")
        startPhase(npc, NightmarePhase.One, players.size)
    }

    private fun startPhase(npc: Npc, phase: NightmarePhase, players: Int) {
        fight.phase = phase
        fight.state = NightmareFight.State.Shield
        fight.shield = NightmareFight.shieldFor(players)
        fight.clearMechanics()
        resetTotems()
        npc.hitpoints = npc.baseHitpointsLvl
        transmog(npc, phase.shielded)
        val encounter = deps.encounter(npc)
        encounter.transitionTo(phase.key, deps.mapClock.cycle)
        encounter.invulnerable = false
        fight.entry?.let { transmog(it, phase.entry) }
        arenaPlayers().forEach { it.mes("<col=ef1020>The Nightmare's shield is up! Break it to reach her.") }
    }

    private fun resetFight(npc: Npc) {
        if (!npc.isValidTarget()) return
        deps.interrupt(npc)
        npc.resetTransmog()
        npc.hitpoints = npc.baseHitpointsLvl
        deps.startEncounter(npc, spec)
        PathingEntityCommon.telejump(npc, deps.collision, NightmareArena.SPAWN)
        prepare(npc)
    }

    private fun NpcHitEvents.Modify.modifyNightmareHit() {
        when (fight.state) {
            NightmareFight.State.Shield -> {
                fight.shield -= hit.damage
                hit.damage = 0
                if (fight.shield <= 0) breakShield(npc)
            }
            NightmareFight.State.Vulnerable ->
                if (npc.hitpoints - hit.damage <= 0) {
                    hit.damage = maxOf(0, npc.hitpoints - 1)
                    awakenTotems(npc)
                }
            NightmareFight.State.Dying -> Unit
            else -> hit.damage = 0
        }
    }

    private fun breakShield(npc: Npc) {
        fight.state = NightmareFight.State.Vulnerable
        transmog(npc, fight.phase.weak)
        arenaPlayers().forEach { it.mes("The Nightmare's shield shatters!") }
    }

    private fun awakenTotems(npc: Npc) {
        fight.state = NightmareFight.State.Totems
        deps.encounter(npc).invulnerable = true
        for ((totem, totemNpc) in fight.totems) transmog(totemNpc, totem.ready)
        arenaPlayers().forEach { it.mes("<col=ef1020>The Nightmare is weakened. Charge the totems!") }
    }

    private fun NpcHitEvents.Modify.chargeTotem(totem: NightmareTotem) {
        if (fight.isTotemShut(npc)) {
            hit.damage = 0
            return
        }
        if (npc.hitpoints - hit.damage > 0) return
        hit.damage = maxOf(0, npc.hitpoints - 1)
        fight.chargedTotems += totem
        transmog(npc, totem.charged)
        npc.anim("seq.nightmare_totem_fully_charged")
        if (fight.chargedTotems.size == NightmareTotem.entries.size) deps.worldQueues.add(1) { totemBlast() }
    }

    private fun totemBlast() {
        val npc = fight.nightmare ?: return
        if (!npc.isValidTarget() || fight.state != NightmareFight.State.Totems) return
        val spell = spotanim(TOTEM_SPELL).id
        val travel = TOTEM_BLAST_TICKS * CYCLES_PER_TICK
        for (totemNpc in fight.totems.values) {
            deps.bossProjectile(spell, totemNpc.coords.translate(1, 1), centre(npc), 80, 80, 0, travel, 0)
        }
        deps.worldQueues.add(TOTEM_BLAST_TICKS) {
            if (!npc.isValidTarget()) return@add
            npc.spotanim("spotanim.nightmare_impact_blast_spotanim")
            val next = fight.phase.next
            if (next == null) {
                finish(npc)
            } else {
                summonSleepwalkers(npc, next)
            }
        }
    }

    private fun finish(npc: Npc) {
        fight.state = NightmareFight.State.Dying
        resetTotems()
        val killer = deps.encounter(npc).lastTarget?.takeIf { it in arenaPlayers() } ?: arenaPlayers().randomOrNull()
        if (killer == null) {
            resetFight(npc)
            return
        }
        npc.queueHit(killer, 0, HitType.Typeless, npc.hitpoints, npcHitModifier)
    }

    private fun summonSleepwalkers(npc: Npc, next: NightmarePhase) {
        fight.state = NightmareFight.State.Sleepwalkers
        resetTotems()
        transmog(npc, NIGHTMARE_BLAST)
        deps.suppressAttacks(npc, SLEEPWALKER_TICKS + BLAST_TICKS)
        val players = arenaPlayers()
        val count = players.size.coerceIn(1, SLEEPWALKER_TYPES.size)
        val tiles = NightmareArena.edgeTiles().filterNot { deps.collision.isWalkBlocked(it) }.shuffled().take(count)
        tiles.forEachIndexed { index, tile ->
            val walker = deps.spawnOwnedNpc(npc, SLEEPWALKER_TYPES[index], tile) ?: return@forEachIndexed
            walker.walkTo(routeFactory, centre(npc))
            fight.sleepwalkers += walker
        }
        fight.sleepwalkersSummoned = fight.sleepwalkers.size
        fight.sleepwalkersEndAt = deps.mapClock.cycle + SLEEPWALKER_TICKS
        fight.phase = next
        arenaPlayers().forEach { it.mes("<col=ef1020>Sleepwalkers stumble towards the Nightmare. Stop them!") }
    }

    private fun stepSleepwalkers(npc: Npc, players: List<Player>, cycle: Int) {
        val body = footprint(npc.coords, NIGHTMARE_SIZE, margin = 1)
        val iterator = fight.sleepwalkers.iterator()
        while (iterator.hasNext()) {
            val walker = iterator.next()
            if (!walker.isValidTarget()) {
                iterator.remove()
                continue
            }
            if (walker.coords !in body) continue
            deps.npcRepo.del(walker, Int.MAX_VALUE)
            fight.sleepwalkersAbsorbed++
            iterator.remove()
        }
        if (fight.sleepwalkers.isNotEmpty() && cycle < fight.sleepwalkersEndAt) return
        fight.sleepwalkers.forEach { if (it.isValidTarget()) deps.npcRepo.del(it, Int.MAX_VALUE) }
        fight.sleepwalkers.clear()
        sleepwalkerBlast(npc, players)
    }

    private fun sleepwalkerBlast(npc: Npc, players: List<Player>) {
        fight.state = NightmareFight.State.Blast
        npc.anim("seq.nightmare_attack_blast")
        val summoned = fight.sleepwalkersSummoned.coerceAtLeast(1)
        val damage = BLAST_BASE_DAMAGE + BLAST_ABSORBED_DAMAGE * fight.sleepwalkersAbsorbed / summoned
        deps.worldQueues.add(BLAST_TICKS) {
            if (!npc.isValidTarget()) return@add
            for (player in arenaPlayers()) {
                player.queueHit(npc, 0, HitType.Typeless, damage, deps.playerHitModifier)
            }
            startPhase(npc, fight.phase, players.size)
        }
    }

    private fun keepTarget(npc: Npc, players: List<Player>, cycle: Int) {
        if (fight.state != NightmareFight.State.Shield && fight.state != NightmareFight.State.Vulnerable && fight.state != NightmareFight.State.Totems) return
        val target = deps.encounter(npc).lastTarget
        if (target != null && target in players) return
        if (cycle < fight.retargetAt) return
        fight.retargetAt = cycle + RETARGET_TICKS
        npc.opPlayer2(players.random(), ai)
    }

    private fun graspingClaws(npc: Npc) {
        val players = arenaPlayers()
        val tiles = players.map { it.coords }.toMutableSet()
        repeat(CLAWS_BASE + CLAWS_PER_PLAYER * players.size) { randomCoreTile()?.let(tiles::add) }
        val telegraph = spotanim("spotanim.nightmare_rift")
        tiles.forEach { deps.worldRepo.spotanimMap(telegraph, it) }
        deps.worldQueues.add(CLAWS_TICKS) {
            if (!npc.isValidTarget()) return@add
            for (player in arenaPlayers()) {
                if (player.coords in tiles) hitTypeless(npc, player, CLAWS_MAX_HIT)
            }
        }
    }

    private fun husks(npc: Npc, target: Player) {
        if (target in fight.bound) return
        val tiles = ring(target.coords, 1, margin = 1).shuffled().take(2)
        if (tiles.size < 2) return
        val husks = listOfNotNull(deps.spawnOwnedNpc(npc, HUSK_MAGIC, tiles[0]), deps.spawnOwnedNpc(npc, HUSK_RANGED, tiles[1]))
        husks.forEach {
            it.anim("seq.husk_spawn")
            it.opPlayer2(target, ai)
        }
        CombatEffects.clearFreezeImmunity(target)
        CombatEffects.freeze(target, HUSK_BIND_TICKS)
        target.mes("<col=ef1020>The Nightmare restrains you with husks! Kill them to break free.")
        fight.bound[target] = husks
    }

    private fun releaseBound() {
        val freed = fight.bound.filterValues { husks -> husks.none { it.isValidTarget() } }.keys
        for (player in freed) {
            CombatEffects.unfreeze(player)
            fight.bound -= player
        }
    }

    private fun parasites(npc: Npc) {
        val hosts = arenaPlayers().shuffled().take(MAX_PARASITES)
        for (host in hosts) {
            host.mes("<col=ef1020>The Nightmare has impregnated you with a deadly parasite!")
            deps.bossProjectile(spotanim("spotanim.nightmare_parasite_travel").id, centre(npc), host.coords, 80, 0, 0, 2 * CYCLES_PER_TICK, 16, homing = host)
            deps.worldQueues.add(PARASITE_TICKS) { burst(npc, host) }
        }
    }

    private fun burst(npc: Npc, host: Player) {
        if (!npc.isValidTarget() || host.hitpoints <= 0 || !NightmareArena.contains(host.coords)) return
        host.mes("The parasite bursts out of you!")
        host.spotanim("spotanim.nightmare_parasite_vomit")
        hitTypeless(npc, host, PARASITE_BURST_MAX_HIT)
        val tile = ring(host.coords, 1, margin = 1).randomOrNull() ?: return
        val parasite = deps.spawnOwnedNpc(npc, PARASITE, tile) ?: return
        parasite.anim("seq.nightmare_parasite_spawn")
        parasite.opPlayer2(host, ai)
        feed(npc, parasite)
    }

    private fun feed(npc: Npc, parasite: Npc) {
        deps.worldQueues.add(PARASITE_FEED_GAP) {
            if (!parasite.isValidTarget() || !npc.isValidTarget() || !fight.inProgress) return@add
            deps.bossProjectile(spotanim("spotanim.nightmare_parasite_heal_travel").id, parasite.coords, centre(npc), 30, 80, 0, CYCLES_PER_TICK, 16)
            if (fight.state == NightmareFight.State.Shield) fight.shield += PARASITE_HEAL else npc.heal(PARASITE_HEAL, showHitsplat = true)
            feed(npc, parasite)
        }
    }

    private fun surge(npc: Npc, target: Player) {
        val west = target.coords.x >= NightmareArena.SURGE_WEST.x + (NightmareArena.SURGE_EAST.x - NightmareArena.SURGE_WEST.x) / 2
        val start = if (west) NightmareArena.SURGE_WEST else NightmareArena.SURGE_EAST
        val end = if (west) NightmareArena.SURGE_EAST else NightmareArena.SURGE_WEST
        deps.suppressAttacks(npc, SURGE_TICKS + 2)
        PathingEntityCommon.telejump(npc, deps.collision, start)
        deps.worldQueues.add(SURGE_TICKS) {
            if (!npc.isValidTarget()) return@add
            PathingEntityCommon.telejump(npc, deps.collision, end)
            val path = NightmareArena.surgePath(start, end)
            for (player in arenaPlayers()) {
                if (player.coords in path) hitTypeless(npc, player, SURGE_MAX_HIT)
            }
        }
    }

    private fun spores(npc: Npc) {
        val count = (SPORES_BASE + SPORES_PER_PLAYER * arenaPlayers().size).coerceAtMost(SPORES_MAX)
        val expires = deps.mapClock.cycle + SPORE_DURATION
        repeat(count) {
            val tile = randomCoreTile() ?: return@repeat
            if (tile in footprint(npc.coords, NIGHTMARE_SIZE, margin = 0)) return@repeat
            deps.locRepo.add(tile, "loc.nightmare_spores", SPORE_DURATION, LocAngle[0], LocShape.CentrepieceStraight)
            fight.spores[tile] = expires
        }
    }

    private fun breatheSpores(players: List<Player>, cycle: Int) {
        fight.spores.entries.removeAll { it.value <= cycle }
        if (fight.spores.isEmpty()) return
        for (player in players) {
            if ((fight.drowsyUntil[player] ?: 0) > cycle) continue
            if (fight.spores.keys.none { it.chebyshevDistance(player.coords) <= 1 }) continue
            fight.drowsyUntil[player] = cycle + DROWSY_TICKS
            player.say("*Yawn*")
            player.runEnergy = 0
            player.actionDelay = maxOf(player.actionDelay, cycle + DROWSY_ATTACK_DELAY)
            player.mes("The spores make you drowsy.")
        }
    }

    private fun resetTotems() {
        for (totemNpc in fight.totems.values) {
            totemNpc.resetTransmog()
            totemNpc.hitpoints = totemNpc.baseHitpointsLvl
        }
        fight.chargedTotems.clear()
    }

    private fun randomCoreTile(): CoordGrid? {
        repeat(TILE_ATTEMPTS) {
            val tile =
                CoordGrid(
                    deps.random.of(NightmareArena.MIN_X, NightmareArena.MAX_X),
                    deps.random.of(NightmareArena.CORE_MIN_Z, NightmareArena.CORE_MAX_Z),
                    NightmareArena.LEVEL,
                )
            if (!deps.collision.isWalkBlocked(tile)) return tile
        }
        return null
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
            .filter { NightmareArena.contains(it) && !deps.collision.isWalkBlocked(it) }

    private fun centre(npc: Npc): CoordGrid = npc.coords.translate(NIGHTMARE_SIZE / 2, NIGHTMARE_SIZE / 2)

    private fun arenaPlayers(): List<Player> =
        deps.playerList.filter { NightmareArena.contains(it.coords) && it.hitpoints > 0 }

    private fun transmog(npc: Npc, type: String) {
        val serverType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(serverType, Int.MAX_VALUE)
    }

    private fun spotanim(name: String) = SpotanimType(name.asRSCM(RSCMType.SPOTANIM))

    private companion object {
        const val ENTRY = "npc.nightmare_entry_ready"
        const val HUSK_MAGIC = "npc.nightmare_husk_magic"
        const val HUSK_RANGED = "npc.nightmare_husk_ranged"
        const val PARASITE = "npc.nightmare_parasite"
        const val TOTEM_SPELL = "spotanim.nightmare_totem_spell_travel"
        val SLEEPWALKER_TYPES = (1..6).map { "npc.nightmare_sleepwalker_$it" }

        const val CYCLES_PER_TICK = 30
        const val EMPTY_RESET_TICKS = 10
        const val RETARGET_TICKS = 5
        const val TILE_ATTEMPTS = 20

        const val TOTEM_BLAST_TICKS = 3

        const val SLEEPWALKER_TICKS = 20
        const val BLAST_TICKS = 2
        const val BLAST_BASE_DAMAGE = 5
        const val BLAST_ABSORBED_DAMAGE = 60

        const val CLAWS_BASE = 10
        const val CLAWS_PER_PLAYER = 3
        const val CLAWS_TICKS = 3
        const val CLAWS_MAX_HIT = 40

        const val HUSK_BIND_TICKS = 50

        const val MAX_PARASITES = 3
        const val PARASITE_TICKS = 18
        const val PARASITE_BURST_MAX_HIT = 20
        const val PARASITE_FEED_GAP = 4
        const val PARASITE_HEAL = 10

        const val SURGE_TICKS = 3
        const val SURGE_MAX_HIT = 60

        const val SPORES_BASE = 6
        const val SPORES_PER_PLAYER = 2
        const val SPORES_MAX = 16
        const val SPORE_DURATION = 20
        const val DROWSY_TICKS = 5
        const val DROWSY_ATTACK_DELAY = 2
    }
}
