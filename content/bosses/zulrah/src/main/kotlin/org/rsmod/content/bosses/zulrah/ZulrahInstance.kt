package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.ScriptContext

class ZulrahInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val fights: ZulrahFights) :
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
            fights.spawn(player, session)
        }
        onEnterObject { defaultInstanceEntry() }
        onOpLoc1(QUICK_BOAT) { defaultInstanceEntry() }
        onOpLoc2(QUICK_BOAT) { defaultInstanceEntry() }
        onExitObject { defaultLeaveFlow() }
        onInstancePlayerLeave { fights.endFor(player) }
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_zulrah"
        private const val QUICK_BOAT = "loc.snakeboss_boat_2ops"

        private val INSTANCE = InstanceArea.copyRegions(regionIds = ZulrahShrine.REGIONS)
    }
}
