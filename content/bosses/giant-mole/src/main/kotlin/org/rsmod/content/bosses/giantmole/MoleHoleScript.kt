package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.dig.DigSites
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MoleHoleScript
@Inject
constructor(private val digSites: DigSites, private val darkness: MoleHoleDarkness) :
    PluginScript() {
    override fun ScriptContext.startup() {
        digSites.register(MoleHole.MOLE_HILLS) { enterMoleHole() }
        onOpLoc1("loc.mole_rope_02") { climbRope() }
        onPlayerSoftTimer(MoleHoleDarkness.TIMER) { darkness.tick(player) }
    }

    private fun ProtectedAccess.enterMoleHole() {
        telejump(MoleHole.LANDING)
        mes("You seem to have dropped down into a network of mole tunnels.")
        darkness.update(player)
    }

    private suspend fun ProtectedAccess.climbRope() {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        darkness.brighten(player)
        telejump(MoleHole.EXIT)
    }
}
