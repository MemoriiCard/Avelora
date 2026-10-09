package org.rsmod.content.bosses.inferno

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class InfernoMonsters
@Inject
internal constructor(private val deps: BossDeps, private val runs: InfernoRuns) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spec in infernoSpecs()) BossCombat.register(this, spec, deps)

        for ((type, death) in DEATHS) {
            val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(npcType, "queue.death") { runs.onDeath(this, death.first, death.second) }
        }
    }

    internal companion object {
        val DEATHS =
            mapOf(
                InfernoMonster.Nibbler.npc to ("seq.jalnib_death" to 3),
                InfernoMonster.Bat.npc to ("seq.jalmejrah_death" to 3),
                InfernoMonster.Blob.npc to ("seq.jalak_death" to 3),
                InfernoMonster.Meleer.npc to ("seq.jalimkot_death" to 4),
                InfernoMonster.Ranger.npc to ("seq.jalxil_death" to 4),
                InfernoMonster.Mager.npc to ("seq.jalakxil_death" to 4),
                InfernoMonster.Jad.npc to ("seq.jaltokjad_death" to 5),
                BLOB_MELEE to ("seq.jalak_death" to 3),
                BLOB_RANGE to ("seq.jalak_death" to 3),
                BLOB_MAGE to ("seq.jalak_death" to 3),
            )
    }
}
