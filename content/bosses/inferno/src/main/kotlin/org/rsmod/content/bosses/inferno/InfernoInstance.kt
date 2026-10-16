package org.rsmod.content.bosses.inferno

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

private var Player.infernoUnlocked by intVarp("varp.inferno_unlocked")

class InfernoInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val runs: InfernoRuns) :
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
        onEnterObject { if (unlock()) defaultInstanceEntry() }
        onExitObject { leave(confirm = true) }
        onOpLoc2(EXIT_LOC) { leave(confirm = false) }
        onInstanceEnded { runs.end(instanceId) }
        onInstancePlayerLeave { runs.end(instanceId) }
    }

    private suspend fun ProtectedAccess.unlock(): Boolean {
        if (player.infernoUnlocked != 0) return true
        if (player.inv.count(FIRE_CAPE) == 0) {
            player.mes("TzHaar-Ket-Keh: Bring me a fire cape and I will let you pass.")
            return false
        }
        val give =
            choice2(
                "Give up the fire cape.", true,
                "Keep it.", false,
                title = "Give your fire cape to enter the Inferno?",
            )
        if (!give) return false
        player.invDel(player.inv, FIRE_CAPE, 1)
        player.infernoUnlocked = 1
        player.mes("TzHaar-Ket-Keh: You may enter the Inferno whenever you wish.")
        return true
    }

    private suspend fun ProtectedAccess.leave(confirm: Boolean) {
        val session = manager.sessionForPlayer(player) ?: return
        val run = runs.runFor(session)
        if (confirm && run != null && run.wave > 0) {
            val sure =
                choice2(
                    "Yes, leave the Inferno.", true,
                    "No, keep fighting.", false,
                    title = "Really leave? Your run will end.",
                )
            if (!sure) return
        }
        if (run != null) runs.forfeit(player, run, InfernoArena.OUTSIDE)
        defaultLeaveFlow()
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_inferno"
        private const val EXIT_LOC = "loc.inferno_exit"
        private const val FIRE_CAPE = "obj.tzhaar_cape_fire"

        private val INSTANCE = InstanceArea.copyRegions(centerRegionId = InfernoArena.REGION)
    }
}
