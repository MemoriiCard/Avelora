package org.rsmod.content.bosses.fightcaves

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.ScriptContext

class FightCaveInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val runs: FightCaveRuns) :
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
        onExitObject { leave(confirm = true) }
        onOpLoc2(EXIT_LOC) { leave(confirm = false) }
        onInstanceEnded { runs.end(instanceId) }
        onInstancePlayerLeave { runs.end(instanceId) }
    }

    private suspend fun ProtectedAccess.leave(confirm: Boolean) {
        val session = manager.sessionForPlayer(player) ?: return
        val run = runs.runFor(session)
        if (confirm && run != null && run.wave > 0) {
            val sure =
                choice2(
                    "Yes, leave the cave.", true,
                    "No, keep fighting.", false,
                    title = "Really leave? Your run will end.",
                )
            if (!sure) return
        }
        if (run != null) runs.forfeit(player, run, FightCaveArena.OUTSIDE)
        defaultLeaveFlow()
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_fight_caves"
        private const val EXIT_LOC = "loc.tzhaar_fightcave_wall_exit"

        private val INSTANCE = InstanceArea.copyRegions(centerRegionId = FightCaveArena.REGION)
    }
}
