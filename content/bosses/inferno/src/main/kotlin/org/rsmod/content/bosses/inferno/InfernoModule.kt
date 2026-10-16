package org.rsmod.content.bosses.inferno

import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PlayerSafeDeathHook
import org.rsmod.plugin.module.PluginModule

class InfernoModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(InfernoRespawnHook::class.java)
        addSetBinding<PlayerSafeDeathHook>(InfernoSafeDeathHook::class.java)
    }
}
