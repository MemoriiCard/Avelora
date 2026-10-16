package org.rsmod.content.raids.toa.wardens

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.plugin.scripts.ScriptContext

class WardensScript @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss(
            "npc.toa_wardens_p1_obelisk_npc",
            "npc.toa_warden_elidinis_phase2_mage",
            "npc.toa_warden_elidinis_phase2_range",
            "npc.toa_warden_elidinis_core",
            "npc.toa_warden_elidinis_phase3",
        ) {
            stats(attackRate = 7)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "toa.wardens_idle"
    }
}
