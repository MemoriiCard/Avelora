package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.script.onEvent
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoxRoomEventsScript @Inject constructor(private val raids: CoxRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Delete> {
            raids.at(npc.coords)?.controllerOf(npc)?.npcRemoved(npc)
        }
    }
}
