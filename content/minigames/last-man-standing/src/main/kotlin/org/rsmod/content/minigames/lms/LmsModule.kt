package org.rsmod.content.minigames.lms

import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class LmsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(LmsDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(LmsRespawnHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(LmsTeleportHook::class.java)
    }
}
