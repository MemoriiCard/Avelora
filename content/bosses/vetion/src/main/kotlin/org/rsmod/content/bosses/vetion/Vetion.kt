package org.rsmod.content.bosses.vetion

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossEncounter
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.interrupt
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Vetion @Inject constructor(deps: BossDeps, ai: AiPlayerInteractions) :
    VetionBoss(VETION_REST, deps, ai)

class Calvarion @Inject constructor(deps: BossDeps, ai: AiPlayerInteractions) :
    VetionBoss(SKELETAL_TOMB, deps, ai)

abstract class VetionBoss
internal constructor(
    private val lair: VetionLair,
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {
    private val formId by lazy { lair.form.asRSCM(RSCMType.NPC) }
    private val hounds = mutableMapOf<Npc, List<Npc>>()
    private val enrages = mutableMapOf<Npc, Int>()

    override fun ScriptContext.startup() {
        BossCombat.register(this, vetionSpec(lair), deps, onModifyHit = { modifyHit() }, onHit = { checkHounds(npc) })
        deps.extensionRegistry.register(lightningHandler(lair)) { _, npc, target, _ -> lightning(npc, target) }
        deps.extensionRegistry.register(bashHandler(lair)) { _, npc, _, _ -> shieldBash(npc) }
        onEvent<NpcStateEvents.Spawn>(formId) { forget(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id == formId) forget(npc) }
    }

    private fun forget(npc: Npc) {
        hounds -= npc
        enrages -= npc
    }

    private fun NpcHitEvents.Modify.modifyHit() {
        if (houndsAlive(npc)) {
            hit.damage = 0
            return
        }
        val phase = deps.encounter(npc).currentPhaseName
        if (phase in NORMAL_PHASES && hit.damage >= npc.hitpoints) {
            hit.damage = npc.hitpoints - 1
            enrage(npc)
        }
    }

    private fun houndsAlive(npc: Npc): Boolean = hounds[npc].orEmpty().any { it.isValidTarget() }

    private fun checkHounds(npc: Npc) {
        if (npc.hitpoints <= 0 || npc.hitpoints * 2 >= npc.baseHitpointsLvl) return
        val encounter = deps.encounter(npc)
        when (encounter.currentPhaseName) {
            PHASE_NORMAL -> {
                encounter.transitionTo(PHASE_NORMAL_HOUNDS, deps.mapClock.cycle)
                summonHounds(npc, lair.hound)
            }
            PHASE_ENRAGED -> {
                encounter.transitionTo(PHASE_ENRAGED_HOUNDS, deps.mapClock.cycle)
                summonHounds(npc, lair.greaterHound)
            }
        }
    }

    private fun summonHounds(npc: Npc, type: String) {
        npc.say(shout(HOUND_SHOUTS, npc))
        val target = deps.encounter(npc).lastTarget
        val summoned = freeTilesAround(npc, HOUND_COUNT).mapNotNull { deps.spawnOwnedNpc(npc, type, it) }
        hounds[npc] = summoned
        if (target != null && target.isValidTarget()) {
            summoned.forEach { it.opPlayer2(target, aiPlayerInteractions) }
        }
    }

    private fun enrage(npc: Npc) {
        val encounter = deps.encounter(npc)
        encounter.invulnerable = true
        deps.interrupt(npc)
        deps.suppressAttacks(npc, ENRAGE_TICKS + 1)
        npc.anim(ENRAGE_SEQ)
        npc.say(ENRAGE_SHOUT)
        val ring = ringTiles(npc)
        ring.forEach { deps.worldRepo.spotanimMap(spotanim(ENRAGED_TELEGRAPH), it) }
        deps.worldQueues.add(ENRAGE_TICKS) {
            if (!npc.isValidTarget()) return@add
            strike(npc, ring, lair.enragedLightningMaxHit, ENRAGED_IMPACT)
            encounter.transitionTo(PHASE_ENRAGED, deps.mapClock.cycle)
            npc.hitpoints = npc.baseHitpointsLvl
            npc.restoreLevels()
            encounter.invulnerable = false
            val enrage = (enrages[npc] ?: 0) + 1
            enrages[npc] = enrage
            deps.worldQueues.add(ENRAGE_DURATION) { calmDown(npc, encounter, enrage) }
        }
    }

    private fun calmDown(npc: Npc, encounter: BossEncounter, enrage: Int) {
        if (!npc.isValidTarget() || deps.encounter(npc) !== encounter || enrages[npc] != enrage) return
        val phase = encounter.currentPhaseName
        if (phase !in ENRAGED_PHASES) return
        val houndsDone = phase == PHASE_ENRAGED_HOUNDS
        encounter.transitionTo(if (houndsDone) PHASE_NORMAL_HOUNDS else PHASE_NORMAL, deps.mapClock.cycle)
        npc.resetTransmog()
    }

    private fun Npc.restoreLevels() {
        attackLvl = baseAttackLvl
        strengthLvl = baseStrengthLvl
        defenceLvl = baseDefenceLvl
        rangedLvl = baseRangedLvl
        magicLvl = baseMagicLvl
    }

    private fun lightning(npc: Npc, target: Player) {
        val enraged = deps.encounter(npc).currentPhaseName in ENRAGED_PHASES
        npc.anim(if (enraged) "seq.npc_vetion_attack_magic_02" else "seq.npc_vetion_attack_magic_01")
        npc.spotanim(if (enraged) "spotanim.fx_vetion_attack_magic_02" else "spotanim.fx_vetion_attack_magic_01")
        maybeShout(npc, ATTACK_SHOUTS, enraged)
        val tiles = boltTiles(target)
        val telegraph = spotanim(if (enraged) ENRAGED_TELEGRAPH else TELEGRAPH)
        tiles.forEach { deps.worldRepo.spotanimMap(telegraph, it) }
        val maxHit = if (enraged) lair.enragedLightningMaxHit else lair.lightningMaxHit
        val impact = if (enraged) ENRAGED_IMPACT else IMPACT
        deps.worldQueues.add(LIGHTNING_TICKS) { if (npc.isValidTarget()) strike(npc, tiles, maxHit, impact) }
    }

    private fun boltTiles(target: Player): List<CoordGrid> {
        val players = arenaPlayers().take(MAX_BOLTS).ifEmpty { listOf(target) }
        val tiles = players.map { near(it.coords) }.toMutableList()
        if (players.size == 1) {
            repeat(SOLO_EXTRA_BOLTS) { randomArenaTile()?.let { tiles += it } }
        }
        return tiles.distinct()
    }

    private fun near(tile: CoordGrid): CoordGrid {
        val moved = tile.translate(deps.random.of(-1, 1), deps.random.of(-1, 1))
        return if (lair.contains(moved) && !deps.collision.isWalkBlocked(moved)) moved else tile
    }

    private fun randomArenaTile(): CoordGrid? {
        repeat(TILE_ATTEMPTS) {
            val tile = CoordGrid(deps.random.of(lair.minX, lair.maxX), deps.random.of(lair.minZ, lair.maxZ), VetionLair.LEVEL)
            if (!deps.collision.isWalkBlocked(tile)) return tile
        }
        return null
    }

    /** Each player takes the strongest bolt that reaches them: full damage on the tile, a third beside it. */
    private fun strike(npc: Npc, tiles: List<CoordGrid>, maxHit: Int, impact: String) {
        val impactAnim = spotanim(impact)
        tiles.forEach { deps.worldRepo.spotanimMap(impactAnim, it) }
        for (player in arenaPlayers()) {
            val closest = tiles.minOfOrNull { it.chebyshevDistance(player.coords) } ?: continue
            val max = when (closest) {
                0 -> maxHit
                1 -> maxHit / INDIRECT_DIVISOR
                else -> continue
            }
            player.queueHit(npc, 0, HitType.Typeless, deps.random.of(1, max), deps.playerHitModifier)
        }
    }

    private fun shieldBash(npc: Npc) {
        val enraged = deps.encounter(npc).currentPhaseName in ENRAGED_PHASES
        npc.anim(if (enraged) "seq.npc_vetion_attack_melee_02" else "seq.npc_vetion_attack_melee_01")
        npc.say(shout(BASH_SHOUTS, npc, enraged))
        val area = footprint(npc, margin = 1)
        val shadow = spotanim(BASH_TELEGRAPH)
        area.forEach { deps.worldRepo.spotanimMap(shadow, it) }
        deps.worldQueues.add(BASH_TICKS) {
            if (!npc.isValidTarget()) return@add
            for (player in arenaPlayers()) {
                if (player.coords !in area) continue
                player.queueHit(npc, 0, HitType.Typeless, deps.random.of(1, lair.bashMaxHit), deps.playerHitModifier)
                player.actionDelay = maxOf(player.actionDelay, deps.mapClock.cycle + BASH_STUN_TICKS)
            }
        }
    }

    private fun footprint(npc: Npc, margin: Int): Set<CoordGrid> {
        val tiles = mutableSetOf<CoordGrid>()
        for (dx in -margin until npc.size + margin) {
            for (dz in -margin until npc.size + margin) tiles += npc.coords.translate(dx, dz)
        }
        return tiles
    }

    private fun ringTiles(npc: Npc): List<CoordGrid> =
        (footprint(npc, margin = RING_MARGIN) - footprint(npc, margin = RING_MARGIN - 1))
            .filterIndexed { index, _ -> index % 2 == 0 }
            .filter { lair.contains(it) && !deps.collision.isWalkBlocked(it) }

    private fun freeTilesAround(npc: Npc, count: Int): List<CoordGrid> =
        (footprint(npc, margin = 1) - footprint(npc, margin = 0))
            .filter { lair.contains(it) && !deps.collision.isWalkBlocked(it) }
            .shuffled()
            .take(count)

    private fun arenaPlayers(): List<Player> = deps.playerList.filter { lair.contains(it.coords) && it.hitpoints > 0 }

    private fun maybeShout(npc: Npc, lines: List<String>, enraged: Boolean) {
        if (deps.random.of(SHOUT_ONE_IN) == 0) npc.say(shout(lines, npc, enraged))
    }

    private fun shout(lines: List<String>, npc: Npc, enraged: Boolean = deps.encounter(npc).currentPhaseName in ENRAGED_PHASES): String {
        val line = lines[deps.random.of(lines.size)]
        return if (enraged) line.uppercase() else line
    }

    private fun spotanim(name: String) = SpotanimType(name.asRSCM(RSCMType.SPOTANIM))

    private companion object {
        val NORMAL_PHASES = setOf(PHASE_NORMAL, PHASE_NORMAL_HOUNDS)
        val ENRAGED_PHASES = setOf(PHASE_ENRAGED, PHASE_ENRAGED_HOUNDS)

        const val HOUND_COUNT = 2
        const val MAX_BOLTS = 7
        const val SOLO_EXTRA_BOLTS = 4
        const val INDIRECT_DIVISOR = 3
        const val TILE_ATTEMPTS = 20
        const val LIGHTNING_TICKS = 3
        const val BASH_TICKS = 2
        const val BASH_STUN_TICKS = 8
        const val ENRAGE_TICKS = 4
        const val ENRAGE_DURATION = 500
        const val RING_MARGIN = 2
        const val SHOUT_ONE_IN = 3

        const val ENRAGE_SEQ = "seq.npc_vetion_enrage_01"
        const val ENRAGE_SHOUT = "Now... DO IT AGAIN!!!"
        const val TELEGRAPH = "spotanim.spells_vetion01_travel"
        const val ENRAGED_TELEGRAPH = "spotanim.spells_vetion01_travel02"
        const val IMPACT = "spotanim.fx_vetion_attack_magic_impact_01"
        const val ENRAGED_IMPACT = "spotanim.fx_vetion_attack_magic_impact_02"
        const val BASH_TELEGRAPH = "spotanim.fx_vetion_attack_melee"

        val ATTACK_SHOUTS =
            listOf(
                "I will smite you!",
                "I've got you now!",
                "Stand still, rat!",
                "You can't escape!",
                "For the lord!",
                "You call that a weapon?!",
                "Dodge this!",
                "You are powerless to me!",
                "I will end you!",
                "Time to die, mortal!",
                "Die, rodent!",
            )

        val BASH_SHOUTS =
            listOf(
                "Now I've got you!",
                "Hands off, wretch!",
                "Grrrah!",
                "Take this!",
                "You're not blocking this one!",
                "Back off, mutt!",
            )

        val HOUND_SHOUTS =
            listOf(
                "Time to feast, hounds!",
                "Go forth, my hounds, and destroy them!",
                "I've had enough of this! Hounds!",
                "Hounds! Get rid of these interlopers!",
            )
    }
}
