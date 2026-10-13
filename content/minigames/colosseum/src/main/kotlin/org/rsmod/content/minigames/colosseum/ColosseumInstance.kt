package org.rsmod.content.minigames.colosseum

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.plugin.scripts.ScriptContext

class ColosseumInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val runs: ColosseumRuns) :
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
            runs.start(player, session)
        }
        onEnterObject { defaultInstanceEntry() }
        onExitObject { leave() }
        onInstanceEnded { runs.end(instanceId) }
        onInstancePlayerLeave { runs.end(instanceId) }
    }

    private suspend fun ProtectedAccess.leave() {
        val session = manager.sessionForPlayer(player) ?: return
        val run = runs.runFor(session)
        if (run != null && run.cleared > 0) {
            val sure =
                choice2(
                    "Yes, leave the Colosseum.", true,
                    "No, keep fighting.", false,
                    title = "Really leave? Unclaimed rewards will be lost.",
                )
            if (!sure) return
        }
        if (run != null) runs.forfeit(player, run)
        defaultLeaveFlow()
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_colosseum"

        private val INSTANCE =
            InstanceArea.copyRegions(
                centerRegionId = ColosseumArena.REGION,
                npcSpawns = listOf(InstanceNpc(MINIMUS_NPC, ColosseumArena.MINIMUS)),
            )
    }
}

internal const val MINIMUS_NPC = "npc.colosseum_master"
