package org.rsmod.content.bosses.kalphitequeen

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.interrupt
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

class KalphiteQueen @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = kalphiteQueenSpec()

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val encounter = deps.encounter(npc)
                if (encounter.currentPhaseName == CRAWLING && hit.damage >= npc.hitpoints) {
                    hit.damage = npc.hitpoints - 1
                    transform(npc)
                }
            },
        )
    }

    private fun transform(npc: Npc) {
        val encounter = deps.encounter(npc)
        encounter.invulnerable = true
        deps.interrupt(npc)
        deps.suppressAttacks(npc, TRANSFORM_TICKS)
        npc.noneMode()
        npc.anim("seq.kalphite_update_queen_death")
        deps.worldQueues.add(SHED_TICKS) {
            encounter.transitionTo(AIRBORNE, deps.mapClock.cycle)
            npc.hitpoints = npc.baseHitpointsLvl
            npc.anim("seq.kalphite_update_flying_queen_emerging")
            npc.spotanim("spotanim.kalphite_update_flying_queen_emerge_spotanim")
        }
        deps.worldQueues.add(TRANSFORM_TICKS) {
            encounter.invulnerable = false
            npc.defaultMode()
        }
    }

    private companion object {
        const val SHED_TICKS = 8
        const val TRANSFORM_TICKS = 20
    }
}
