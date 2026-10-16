package org.rsmod.content.raids.cox.raid

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook

class CoxKillHook @Inject constructor(private val raids: CoxRaids) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val raid = raids.at(context.npc.coords) ?: return
        raid.controllerOf(context.npc)?.killed(context.npc, context.hero, context.dropCoords)
    }
}
