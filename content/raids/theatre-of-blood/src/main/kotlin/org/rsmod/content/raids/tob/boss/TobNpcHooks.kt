package org.rsmod.content.raids.tob.boss

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.script.onEvent
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TobKillHook @Inject constructor(private val raids: TobRaids) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val room = raids.bossRoomOf(context.npc) ?: return
        room.killed(context.npc, context.hero)
    }
}

class TobNpcEventsScript @Inject constructor(private val raids: TobRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Delete> { raids.bossRoomOf(npc)?.removed(npc) }
    }
}

internal fun TobRaids.bossRoomOf(npc: Npc): TobBossRoom? =
    (at(npc.coords)?.controller as? TobBossRoom)?.takeIf { it.owns(npc) }
