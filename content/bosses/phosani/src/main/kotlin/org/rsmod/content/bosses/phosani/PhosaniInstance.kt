package org.rsmod.content.bosses.phosani

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
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class PhosaniInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val bossHpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val locRepo: LocRepository,
) : InstanceScript(registry) {
    override fun settingsRow(): String = "dbrow.instance_phosani"

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
            val spawnBoss = result.isFreshRun()
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (result is InstanceManager.Result.Created) placeExit(session)
            if (spawnBoss) {
                deps.worldQueues.add(SPAWN_DELAY_TICKS) {
                    if (manager.sessionForPlayer(player) == session) spawn(player, session)
                }
            }
        }
        onEnterObject { enterOrLeave() }
        onExitObject { enterOrLeave() }
    }

    private suspend fun org.rsmod.api.player.protect.ProtectedAccess.enterOrLeave() {
        if (manager.sessionForPlayer(player) == null) defaultInstanceEntry() else defaultLeaveFlow()
    }

    private fun placeExit(session: InstanceSession) {
        val coords = manager.resolveCoord(session, EXIT_TEMPLATE) ?: return
        locRepo.add(coords, EXIT_LOC, Int.MAX_VALUE, LocAngle[0], LocShape.CentrepieceStraight)
    }

    private fun spawn(player: Player, session: InstanceSession) {
        val coords = manager.resolveCoord(session, BOSS_TEMPLATE) ?: return
        val type = ServerCacheManager.getNpc(PHOSANI_FIRST.asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        manager.registerSessionNpc(player, npc)
        bossHpBar.onOpen(player, npc)
        npc.apPlayer2(player, aiPlayerInteractions)
    }

    private companion object {
        const val SPAWN_DELAY_TICKS = 3
        const val EXIT_LOC = "loc.nightmare_challenge_portal_enabled"
        val BOSS_TEMPLATE = CoordGrid(3869, 9948, 3)
        val EXIT_TEMPLATE = CoordGrid(3872, 9943, 3)
        val INSTANCE = InstanceArea.copyRegions(centerRegionId = 15515, level = 3)
    }
}
