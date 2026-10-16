package org.rsmod.content.minigames.colosseum

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ColosseumMonsters
@Inject
internal constructor(private val deps: BossDeps, private val runs: ColosseumRuns) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spec in colosseumSpecs()) BossCombat.register(this, spec, deps)

        for (monster in ColosseumMonster.entries) {
            val npcType = ServerCacheManager.getNpc(monster.npc.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(npcType, "queue.death") { runs.onDeath(this, monster) }
        }
    }
}
