package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRaidScript @Inject constructor(private val raids: ToaRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        for (door in DOORS) onOpLoc1(door) { path() }
        onOpLoc1("loc.toa_nexus_wardens_door") { wardens() }
    }

    private fun ProtectedAccess.path() {
        if (raids.containing(player) == null) return
        mes("This path has not been opened yet.")
    }

    private fun ProtectedAccess.wardens() {
        if (raids.containing(player) == null) return
        mes("The way to the Wardens is sealed until every path is cleared.")
    }

    private companion object {
        val DOORS =
            listOf(
                "loc.toa_nexus_crondis_door",
                "loc.toa_nexus_het_door",
                "loc.toa_nexus_scabaras_door",
                "loc.toa_nexus_apmeken_door",
            )
    }
}
