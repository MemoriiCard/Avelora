package org.rsmod.content.raids.tob.verzik

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.plugin.scripts.ScriptContext

class VerzikScript @Inject constructor(deps: BossDeps, private val raids: TobRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val room = raids.at(npc.coords)?.controller as? VerzikRoom
                if (room != null) hit.damage = room.incoming(npc, hit.damage)
            },
        )
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss(*NPCS) {
            stats(attackRate = 7)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "tob.verzik_idle"
        val NPCS =
            listOf("", "_story", "_hard")
                .flatMap { suffix -> (1..3).map { "npc.verzik_phase$it$suffix" } }
                .toTypedArray()
    }
}
