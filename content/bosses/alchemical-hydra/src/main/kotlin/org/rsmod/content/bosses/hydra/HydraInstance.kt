package org.rsmod.content.bosses.hydra

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.plugin.scripts.ScriptContext

class HydraInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val fights: HydraFights) :
    InstanceScript(registry) {

    override fun settingsRow(): String = SETTINGS_ROW

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun destroyWhenEmpty(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            enter()
            fights.spawnHydra(session, player)
        }
        val door: suspend ProtectedAccess.() -> Unit = {
            when {
                manager.sessionForPlayer(player) != null -> defaultLeaveFlow()
                canEnter() -> defaultInstanceEntry()
            }
        }
        onEnterObject(door)
        onOpLoc1(DOOR_M) { door() }
        onInstancePlayerLeave { fights.endFor(player) }
        onOpLoc1(CLIMB) {
            telejump(if (player.coords.z < CLIMB_Z) HydraLair.CLIMB_NORTH else HydraLair.CLIMB_SOUTH)
        }
    }

    private fun ProtectedAccess.canEnter(): Boolean {
        if (player.statBase("stat.slayer") < HydraLair.SLAYER_LEVEL) {
            mes("You need a Slayer level of ${HydraLair.SLAYER_LEVEL} to enter.")
            return false
        }
        val task = SlayerTaskManager.getCurrentSlayerTask(this)
        val onTask = task != null && TASK_TYPES.any { SlayerTaskManager.isTaskNpcType(it, task.id) }
        if (!onTask) {
            mes("You need to be on a Hydra Slayer task to enter.")
            return false
        }
        return true
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_alchemical_hydra"
        private const val DOOR_M = "loc.karuulm_hydra_room_door_m"
        private const val CLIMB = "loc.karuulm_hydra_room_entrance_climb"
        private const val CLIMB_Z = 10251

        private val TASK_TYPES by lazy {
            listOf("npc.hydra", HydraPhase.Serpentine.npc).mapNotNull {
                ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC))
            }
        }

        private val INSTANCE = InstanceArea.copyRegions(regionIds = listOf(HydraLair.REGION))
    }
}
