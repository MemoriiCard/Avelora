package org.rsmod.content.raids.cox.olm

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.content.raids.cox.room.CoxDamagePoints
import org.rsmod.plugin.scripts.ScriptContext

class OlmScript
@Inject
constructor(deps: BossDeps, private val points: CoxDamagePoints, private val raids: CoxRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val room = raids.at(npc.coords)?.olm
                hit.damage = room?.incoming(npc, hit.type, hit.damage) ?: 0
            },
            onHit = { points.award(this) },
        )
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss(OlmRoom.HEAD, OlmRoom.LEFT_HAND, OlmRoom.RIGHT_HAND) {
            stats(attackRate = 4)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "cox.olm_idle"
    }
}
