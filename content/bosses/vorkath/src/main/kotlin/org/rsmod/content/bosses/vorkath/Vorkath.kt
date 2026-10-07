package org.rsmod.content.bosses.vorkath

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Vorkath
@Inject
internal constructor(
    private val deps: BossDeps,
    private val fights: VorkathFights,
    private val npcDeath: NpcDeath,
    private val instances: InstanceManager,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            vorkathSpec(),
            deps,
            onModifyHit = { if (fights.fightFor(npc)?.acidPhase == true) hit.damage /= 2 },
        )
        BossCombat.register(this, zombifiedSpawnSpec(), deps)

        deps.extensionRegistry.register(VORKATH_NEXT_ACTION) { _, npc, target, _ ->
            fights.nextAction(npc, target)
        }
        deps.extensionRegistry.register(VORKATH_VENOM) { _, _, target, _ -> PlayerVenom.tryVenom(target) }
        deps.extensionRegistry.register(SPAWN_EXPLODE) { _, npc, target, _ -> fights.explodeSpawn(npc, target) }

        onOpNpc1(SLEEPING) { fights.wake(it.npc, player) }

        type(AWAKE)?.let { onNpcQueue(it, "queue.death") { vorkathDeath(this) } }
        type(SPAWN)?.let { onNpcQueue(it, "queue.death") { spawnDeath(this) } }
    }

    private suspend fun vorkathDeath(access: StandardNpcAccess) {
        val npc = access.npc
        val fight = fights.end(npc)
        access.noneMode()
        access.hideAllOps()
        access.anim(DEATH_SEQ)
        access.delay(DEATH_TICKS)
        if (fight != null) {
            val drop = instances.resolveCoord(fight.session, VorkathArena.DROP_TILE) ?: fight.player.coords
            npcDeath.spawnDrops(access, drop)
        }
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        fight?.let { fights.respawnSleeping(it) }
    }

    private suspend fun spawnDeath(access: StandardNpcAccess) {
        fights.onSpawnDeath(access.npc)
        access.noneMode()
        access.hideAllOps()
        access.delay(1)
        deps.npcRepo.del(access.npc, Int.MAX_VALUE)
    }

    private fun type(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))

    private companion object {
        const val SLEEPING = "npc.vorkath_sleeping"
        const val AWAKE = "npc.vorkath"
        const val SPAWN = "npc.vorkath_spawn"
        const val DEATH_SEQ = "seq.ds2_vorkath_death"
        const val DEATH_TICKS = 5
    }
}
