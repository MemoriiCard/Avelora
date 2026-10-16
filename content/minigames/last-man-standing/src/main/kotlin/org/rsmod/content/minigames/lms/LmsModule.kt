package org.rsmod.content.minigames.lms

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class LmsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(LmsDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(LmsRespawnHook::class.java)
        addSetBinding<NpcAttackValidateHook>(LmsBotHook::class.java)
        addSetBinding<NpcDeathKillHook>(LmsBotHook::class.java)
        addSetBinding<NpcDeathDropHook>(LmsBotHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(LmsTeleportHook::class.java)
    }
}
