package org.rsmod.content.bosses.royaltitans

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class RoyalTitansInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val bossHpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : InstanceScript(registry) {
    override fun settingsRow(): String = "dbrow.instance_royal_titans"

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            val spawnBosses = result.isFreshRun()
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (spawnBosses) {
                deps.worldQueues.add(SPAWN_DELAY_TICKS) {
                    if (manager.sessionForPlayer(player) == session) spawnTitans(player, session)
                }
            }
        }
        onEnterObject { defaultInstanceEntry() }
        onExitObject { defaultLeaveFlow() }
    }

    private fun spawnTitans(player: Player, session: InstanceSession) {
        val branda = spawn(player, session, "npc.rt_fire_queen", BRANDA_TEMPLATE) ?: return
        spawn(player, session, "npc.rt_ice_king", ELDRIC_TEMPLATE)
        bossHpBar.onOpen(player, branda)
    }

    private fun spawn(player: Player, session: InstanceSession, name: String, template: CoordGrid): Npc? {
        val coords = manager.resolveCoord(session, template) ?: return null
        val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: return null
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        manager.registerSessionNpc(player, npc)
        npc.apPlayer2(player, aiPlayerInteractions)
        return npc
    }

    private companion object {
        const val SPAWN_DELAY_TICKS = 3
        val BRANDA_TEMPLATE = CoordGrid(2974, 9557, 0)
        val ELDRIC_TEMPLATE = CoordGrid(2966, 9557, 0)
        val INSTANCE = InstanceArea.copyRegions(centerRegionId = 11925)
    }
}
