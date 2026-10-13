package org.rsmod.content.minigames.colosseum

import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PlayerSafeDeathHook
import org.rsmod.plugin.module.PluginModule

class ColosseumModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(ColosseumRespawnHook::class.java)
        addSetBinding<PlayerSafeDeathHook>(ColosseumSafeDeathHook::class.java)
    }
}
