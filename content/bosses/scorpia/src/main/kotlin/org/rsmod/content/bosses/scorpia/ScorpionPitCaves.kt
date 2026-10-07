package org.rsmod.content.bosses.scorpia

import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScorpionPitCaves : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ENTRANCE) { enter() }
        onOpLoc1(EXIT) { leave() }
    }

    private suspend fun ProtectedAccess.enter() {
        arriveDelay()
        telejump(ScorpionPit.nearestCave(player.coords).cave, TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.leave() {
        arriveDelay()
        telejump(ScorpionPit.nearestSurface(player.coords).surface, TeleportType.Exempt)
    }

    private companion object {
        const val ENTRANCE = "loc.wilderness_scorpion_entrance"
        const val EXIT = "loc.wilderness_scorpion_exit"
    }
}
