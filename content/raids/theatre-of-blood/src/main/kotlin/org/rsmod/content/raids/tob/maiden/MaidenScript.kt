package org.rsmod.content.raids.tob.maiden

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.ScriptContext

class MaidenScript @Inject constructor(deps: BossDeps, private val raids: TobRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onHit = {
                if (hit.type == HitType.Magic) {
                    (raids.at(npc.coords)?.controller as? MaidenRoom)?.freeze(npc)
                }
            },
        )
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss(*NPCS) {
            stats(attackRate = 10)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "tob.maiden_idle"
        val NPCS =
            listOf("", "_story", "_hard")
                .flatMap { suffix ->
                    listOf("100", "70", "50", "30", "dying_a", "dying_b").map {
                        "npc.tob_maiden_$it$suffix"
                    } + listOf("npc.maiden_elemental$suffix", "npc.maiden_blood_slug$suffix")
                }
                .toTypedArray()
    }
}
