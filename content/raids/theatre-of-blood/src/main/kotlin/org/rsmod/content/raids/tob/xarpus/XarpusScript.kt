package org.rsmod.content.raids.tob.xarpus

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.plugin.scripts.ScriptContext

class XarpusScript @Inject constructor(deps: BossDeps, private val raids: TobRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val room = raids.at(npc.coords)?.controller as? XarpusRoom
                val uid = hit.sourceUid
                if (room != null && hit.isFromPlayer && uid != null) {
                    val attacker = PlayerUid(uid).resolve(deps.playerList)
                    hit.damage = if (attacker == null) 0 else room.incoming(attacker, hit.damage)
                }
            },
        )
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss(
            "npc.tob_xarpus_combat",
            "npc.tob_xarpus_combat_story",
            "npc.tob_xarpus_combat_hard",
        ) {
            stats(attackRate = 4)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private companion object {
        const val IDLE = "tob.xarpus_idle"
    }
}
