package org.rsmod.content.bosses.abyssalsire

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
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AbyssalSire
@Inject
internal constructor(
    private val deps: BossDeps,
    private val fights: SireFights,
    private val npcDeath: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, sireSpec(), deps, onModifyHit = { fights.onSireHit(npc, hit) })
        for (spec in listOf(tentacleSpec(), spawnSpec(), scionSpec())) BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register(SIRE_NEXT_ACTION) { _, npc, target, _ -> fights.nextAction(npc, target) }

        for (type in SIRE_TYPES) npcType(type)?.let { onNpcQueue(it, DEATH) { sireDeath(this) } }
        npcType(LUNG)?.let { onNpcQueue(it, DEATH) { lungDeath(this) } }
        npcType(SPAWN)?.let { onNpcQueue(it, DEATH) { minionDeath(this, SPAWN_DEATH_SEQ) } }
        npcType(SCION)?.let { onNpcQueue(it, DEATH) { minionDeath(this, SCION_DEATH_SEQ) } }

        onOpLoc1(NEXUS_ENTRANCE) { telejump(AbyssalNexus.NEXUS) }
        onOpLoc1(NEXUS_EXIT) { telejump(AbyssalNexus.ABYSS) }
    }

    private suspend fun sireDeath(access: StandardNpcAccess) {
        val npc = access.npc
        val looter = deps.encounter(npc).lastTarget
        fights.end(npc)
        access.noneMode()
        access.hideAllOps()
        access.anim(SIRE_DEATH_SEQ)
        access.delay(SIRE_DEATH_TICKS)
        npcDeath.spawnDrops(access, looter?.coords ?: npc.coords)
        npc.resetMode()
        npc.resetTransmog()
        npc.hitpoints = npc.baseHitpointsLvl
        npc.movementLocked = false
        npc.apRangeOverride = null
        npc.showAllOps()
        if (npc.respawns) deps.npcRepo.despawn(npc, SIRE_RESPAWN_TICKS) else deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun lungDeath(access: StandardNpcAccess) {
        val npc = access.npc
        access.noneMode()
        access.hideAllOps()
        npcType(LUNG_DYING)?.let { npc.transmog(it, Int.MAX_VALUE) }
        fights.onLungDeath(npc)
    }

    private suspend fun minionDeath(access: StandardNpcAccess, seq: String) {
        access.noneMode()
        access.hideAllOps()
        access.anim(seq)
        access.delay(MINION_DEATH_TICKS)
        deps.npcRepo.del(access.npc, Int.MAX_VALUE)
    }

    private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))

    private companion object {
        const val DEATH = "queue.death"
        const val NEXUS_ENTRANCE = "loc.rcu_abyss_to_overseer"
        const val NEXUS_EXIT = "loc.rcu_overseer_to_abyss"
        const val SIRE_DEATH_SEQ = "seq.sire_death"
        const val SPAWN_DEATH_SEQ = "seq.abyssal_spawn_death"
        const val SCION_DEATH_SEQ = "seq.abyssal_scion_death"
        const val SIRE_DEATH_TICKS = 5
        const val SIRE_RESPAWN_TICKS = 1
        const val MINION_DEATH_TICKS = 2
    }
}
