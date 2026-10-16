package org.rsmod.content.bosses.vorkath

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.plugin.scripts.ScriptContext

class VorkathInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val fights: VorkathFights) :
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
            fights.spawnSleeping(session)
        }
        onEnterObject {
            if (manager.sessionForPlayer(player) != null) defaultLeaveFlow() else defaultInstanceEntry()
        }
        onInstancePlayerLeave { fights.endFor(player) }
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_vorkath"

        private val INSTANCE = InstanceArea.copyRegions(regionIds = listOf(VorkathArena.REGION))
    }
}
