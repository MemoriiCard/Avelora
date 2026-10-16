package org.rsmod.content.raids.toa.boss

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.script.onEvent
import org.rsmod.content.raids.toa.raid.ToaRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaKillHook @Inject constructor(private val raids: ToaRaids) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val room = raids.bossRoomOf(context.npc) ?: return
        room.killed(context.npc, context.hero)
    }
}

class ToaNpcEventsScript @Inject constructor(private val raids: ToaRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Delete> { raids.bossRoomOf(npc)?.removed(npc) }
    }
}

internal fun ToaRaids.bossRoomOf(npc: Npc): ToaBossRoom? =
    (at(npc.coords)?.controller as? ToaBossRoom)?.takeIf { it.owns(npc) }
