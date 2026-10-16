package org.rsmod.content.bosses.scorpia

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ProjAnimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.clearOwnedNpcs
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Scorpia
@Inject
constructor(private val deps: BossDeps, private val routeFactory: RouteFactory) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            scorpiaSpec(),
            deps,
            onLethal = { deps.clearOwnedNpcs(it) },
            onHit = { onScorpiaHit(npc) },
        )
        BossCombat.register(this, offspringSpec(), deps)
    }

    private fun onScorpiaHit(scorpia: Npc) {
        if (scorpia.hitpoints <= 0) return
        val encounter = deps.encounter(scorpia)
        val tick = deps.mapClock.cycle
        when (encounter.currentPhaseName) {
            PHASE_FIGHT -> if (scorpia.hitpoints * 2 < scorpia.baseHitpointsLvl) encounter.transitionTo(PHASE_WOUNDED, tick)
            PHASE_WOUNDED -> {
                encounter.transitionTo(PHASE_GUARDED, tick)
                summonGuardians(scorpia)
            }
        }
    }

    private fun summonGuardians(scorpia: Npc) {
        for (tile in guardianTiles(scorpia)) {
            val guardian = deps.spawnOwnedNpc(scorpia, GUARDIAN, tile) ?: continue
            tendScorpia(guardian, scorpia)
        }
    }

    private fun guardianTiles(scorpia: Npc): List<CoordGrid> {
        val tiles = mutableListOf<CoordGrid>()
        var attempts = 0
        while (tiles.size < GUARDIAN_COUNT && attempts++ < SPAWN_ATTEMPTS) {
            val dx = deps.random.of(scorpia.size + 2) - 1
            val dz = deps.random.of(scorpia.size + 2) - 1
            val tile = scorpia.coords.translate(dx, dz)
            val underScorpia = dx in 0 until scorpia.size && dz in 0 until scorpia.size
            if (underScorpia || tile in tiles || !ScorpionPit.inCave(tile)) continue
            if (deps.collision.isWalkBlocked(tile)) continue
            tiles += tile
        }
        return tiles
    }

    /** Heals [scorpia] every few ticks while in range; despawns after too long without a heal. */
    private fun tendScorpia(guardian: Npc, scorpia: Npc) {
        var idleTicks = 0
        var nextHeal = HEAL_INTERVAL
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (!guardian.isValidTarget() || !scorpia.isValidTarget()) return@repeatTick false
                val inRange = guardian.coords.chebyshevDistance(scorpia.coords) <= HEAL_RANGE + scorpia.size - 1
                if (!inRange && !CombatEffects.isFrozen(guardian) && !guardian.hasMovementQueued()) {
                    guardian.walkTo(routeFactory, scorpia.coords, passThroughEntities = false)
                }
                if (inRange) {
                    idleTicks = 0
                    if (--nextHeal <= 0) {
                        healScorpia(guardian, scorpia)
                        nextHeal = HEAL_INTERVAL
                    }
                } else if (++idleTicks >= DESPAWN_TICKS) {
                    deps.npcRepo.del(guardian, Int.MAX_VALUE)
                    return@repeatTick false
                }
                true
            },
        )
    }

    private fun healScorpia(guardian: Npc, scorpia: Npc) {
        val proj = ProjAnim.fromNpcToNpc(guardian, scorpia, HEAL_SPOTANIM.asRSCM(RSCMType.SPOTANIM), HEAL_PROJECTILE)
        deps.worldRepo.projAnim(proj)
        deps.worldQueues.add(proj.serverCycles) {
            if (scorpia.isValidTarget() && scorpia.hitpoints < scorpia.baseHitpointsLvl) scorpia.heal(HEAL_AMOUNT, showHitsplat = true)
        }
    }

    private fun Npc.hasMovementQueued(): Boolean = routeDestination.isNotEmpty()

    private companion object {
        const val GUARDIAN_COUNT = 2
        const val SPAWN_ATTEMPTS = 30
        const val HEAL_AMOUNT = 8
        const val HEAL_INTERVAL = 3
        const val HEAL_RANGE = 3
        const val DESPAWN_TICKS = 25
        const val HEAL_SPOTANIM = "spotanim.curse_travel"

        val HEAL_PROJECTILE =
            ProjAnimType(
                startHeight = 20,
                endHeight = 40,
                delay = 30,
                angle = 10,
                lengthAdjustment = 0,
                progress = 0,
                stepMultiplier = 5,
            )
    }
}
