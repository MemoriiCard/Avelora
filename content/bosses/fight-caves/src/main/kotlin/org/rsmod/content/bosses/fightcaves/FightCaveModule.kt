package org.rsmod.content.bosses.fightcaves

import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PlayerSafeDeathHook
import org.rsmod.plugin.module.PluginModule

class FightCaveModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(FightCaveRespawnHook::class.java)
        addSetBinding<PlayerSafeDeathHook>(FightCaveSafeDeathHook::class.java)
    }
}
