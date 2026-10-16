package org.rsmod.content.bosses.fightcaves

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FightCaveMonsters
@Inject
internal constructor(private val deps: BossDeps, private val runs: FightCaveRuns) : PluginScript() {
    override fun ScriptContext.startup() {
        val canHeal = Condition.Custom { npc, _ -> runs.woundedNear(npc) != null }
        for (spec in fightCaveSpecs(canHeal)) {
            when (spec.npcTypes.first()) {
                FightCaveMonster.TzTokJad.npc ->
                    BossCombat.register(this, spec, deps, onHit = {
                        val run = runs.runFor(npc) ?: return@register
                        if (!run.healersSummoned && npc.hitpoints in 1 until JAD_HEALER_THRESHOLD) {
                            runs.summonHealers(run, npc)
                        }
                    })
                HEALER ->
                    BossCombat.register(this, spec, deps, onHit = {
                        if (hit.isFromPlayer) runs.runFor(npc)?.let { runs.distractHealer(it, npc) }
                    })
                else -> BossCombat.register(this, spec, deps)
            }
        }
        deps.extensionRegistry.register(MEJKOT_HEAL_HANDLER) { _, npc, _, _ -> runs.mejKotHeal(npc) }

        for ((type, death) in DEATHS) {
            val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(npcType, "queue.death") { runs.onDeath(this, death.first, death.second) }
        }
    }

    private companion object {
        const val JAD_HEALER_THRESHOLD = 150
        const val HEALER = "npc.tzhaar_fightcave_swarm_boss_cleric"

        val DEATHS =
            mapOf(
                "npc.tzhaar_fightcave_swarm_1a" to ("seq.firebat_death" to 3),
                "npc.tzhaar_fightcave_swarm_1b" to ("seq.firebat_death" to 3),
                "npc.tzhaar_fightcave_swarm_2a" to ("seq.lavabeast_death" to 3),
                "npc.tzhaar_fightcave_swarm_2b" to ("seq.lavabeast_death" to 3),
                "npc.tzhaar_fightcave_swarm_2spawn" to ("seq.lavabeast_death" to 3),
                "npc.tzhaar_fightcave_swarm_3a" to ("seq.magmaquris_death" to 3),
                "npc.tzhaar_fightcave_swarm_3b" to ("seq.magmaquris_death" to 3),
                "npc.tzhaar_fightcave_swarm_4a" to ("seq.lizard_cleric_death" to 3),
                "npc.tzhaar_fightcave_swarm_4b" to ("seq.lizard_cleric_death" to 3),
                "npc.tzhaar_fightcave_swarm_5a" to ("seq.igniferum_death" to 4),
                "npc.tzhaar_fightcave_swarm_5b" to ("seq.igniferum_death" to 4),
                "npc.tzhaar_fightcave_swarm_boss" to ("seq.lordmagmus_death" to 5),
                HEALER to ("seq.lizard_cleric_death" to 3),
            )
    }
}
