package org.rsmod.content.bosses.cerberus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Cerberus
@Inject
internal constructor(
    private val deps: BossDeps,
    private val fights: CerberusFights,
    private val npcDeath: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, cerberusSpec(), deps)
        for (spec in soulSpecs()) BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register(CERBERUS_NEXT_ACTION) { _, npc, target, _ -> fights.nextAction(npc, target) }
        ServerCacheManager.getNpc(CERBERUS.asRSCM(RSCMType.NPC))?.let { onNpcQueue(it, DEATH) { cerberusDeath(this) } }
    }

    private suspend fun cerberusDeath(access: StandardNpcAccess) {
        val npc = access.npc
        val looter = deps.encounter(npc).lastTarget
        fights.end(npc)
        access.noneMode()
        access.hideAllOps()
        access.anim(DEATH_SEQ)
        access.delay(DEATH_TICKS)
        npcDeath.spawnDrops(access, looter?.coords ?: npc.coords)
        npc.resetMode()
        npc.resetTransmog()
        npc.hitpoints = npc.baseHitpointsLvl
        if (npc.respawns) deps.npcRepo.despawn(npc, RESPAWN_TICKS) else deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private companion object {
        const val DEATH = "queue.death"
        const val DEATH_SEQ = "seq.cerberus_death"
        const val DEATH_TICKS = 5
        const val RESPAWN_TICKS = 15
    }
}
