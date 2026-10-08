package org.rsmod.content.raids.tob.sotetseg

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.plugin.scripts.ScriptContext

class SotetsegScript @Inject constructor(deps: BossDeps, private val raids: TobRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register(IDLE) { _ -> }
        onOpLoc1("loc.tob_sotetseg_darkrealm_exit") {
            val raid = raids.containing(player) ?: return@onOpLoc1
            (raid.controller as? SotetsegRoom)?.leaveMaze(player)
        }
    }

    override val spec: BossSpec =
        boss(
            "npc.tob_sotetseg_combat",
            "npc.tob_sotetseg_combat_story",
            "npc.tob_sotetseg_combat_hard",
        ) {
            stats(attackRate = 5)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "tob.sotetseg_idle"
    }
}
