package org.rsmod.content.bosses.hydra

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AlchemicalHydra
@Inject
internal constructor(
    private val deps: BossDeps,
    private val fights: HydraFights,
    private val npcDeath: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, hydraSpec(), deps, onModifyHit = { fights.onIncomingHit(npc, hit) })
        deps.extensionRegistry.register(HYDRA_NEXT_ACTION) { _, npc, target, _ -> fights.nextAction(npc, target) }
        npcType(HydraPhase.Enraged.npc)?.let { onNpcQueue(it, DEATH) { hydraDeath(this) } }
    }

    private suspend fun hydraDeath(access: StandardNpcAccess) {
        val npc = access.npc
        val fight = fights.end(npc)
        access.noneMode()
        access.hideAllOps()
        npcType(FINAL_DEATH)?.let { npc.transmog(it, Int.MAX_VALUE) }
        access.anim(DEATH_SEQ)
        access.delay(DEATH_TICKS)
        npcDeath.spawnDrops(access, fight?.player?.coords ?: npc.coords)
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        fight?.let { fights.respawnLater(it) }
    }

    private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))

    private companion object {
        const val DEATH = "queue.death"
        const val FINAL_DEATH = "npc.hydraboss_finaldeath"
        const val DEATH_SEQ = "seq.hydra_stage_4_death"
        const val DEATH_TICKS = 5
    }
}
