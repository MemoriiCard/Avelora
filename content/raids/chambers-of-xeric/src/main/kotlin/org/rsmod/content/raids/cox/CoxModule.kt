package org.rsmod.content.raids.cox

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.content.raids.cox.raid.CoxDeathPenaltyHook
import org.rsmod.content.raids.cox.raid.CoxKillHook
import org.rsmod.content.raids.cox.raid.CoxRespawnHook
import org.rsmod.content.raids.cox.raid.CoxSafeDeathHook
import org.rsmod.plugin.module.PluginModule

class CoxModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(CoxSafeDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(CoxRespawnHook::class.java)
        addSetBinding<PlayerDeathCleanupHook>(CoxDeathPenaltyHook::class.java)
        addSetBinding<NpcDeathKillHook>(CoxKillHook::class.java)
    }
}
