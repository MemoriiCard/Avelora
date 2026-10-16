package org.rsmod.content.bosses.corp

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CorporealBeast
@Inject
internal constructor(private val deps: BossDeps, private val fights: CorpFights) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            corpSpec(),
            deps,
            onCombatTick = { fights.stompCheck(npc) },
            onHit = { if (hit.isFromPlayer && hit.damage >= BIG_HIT) fights.rollCore(npc) },
        )
        deps.extensionRegistry.register(CORE_ROLL) { _, npc, _, _ -> fights.rollCoreOnAttack(npc) }
        deps.extensionRegistry.register(SPLIT_SHOT) { _, npc, target, _ -> fights.splitShot(npc, target) }
        onEvent<NpcStateEvents.Delete> { if (npc.type.isType(CORP)) fights.clear(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.isType(CORP)) fights.clear(npc) }
    }

    private companion object {
        const val BIG_HIT = 32
    }
}
