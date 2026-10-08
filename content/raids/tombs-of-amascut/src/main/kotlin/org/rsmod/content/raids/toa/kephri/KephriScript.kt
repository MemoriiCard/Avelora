package org.rsmod.content.raids.toa.kephri

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.toa.raid.ToaRaids
import org.rsmod.plugin.scripts.ScriptContext

class KephriScript @Inject constructor(deps: BossDeps, private val raids: ToaRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val room = raids.at(npc.coords)?.controller as? KephriRoom
                if (room != null) hit.damage = room.incoming(hit.damage)
            },
        )
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss("npc.toa_kephri_boss_shielded", "npc.toa_kephri_boss_enrage") {
            stats(attackRate = 6)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "toa.kephri_idle"
    }
}
