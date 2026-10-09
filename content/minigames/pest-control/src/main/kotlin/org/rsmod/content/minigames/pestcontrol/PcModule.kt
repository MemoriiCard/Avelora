package org.rsmod.content.minigames.pestcontrol

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class PcModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(PcDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(PcRespawnHook::class.java)
        addSetBinding<NpcAttackValidateHook>(PcNpcHook::class.java)
        addSetBinding<NpcDeathKillHook>(PcNpcHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(PcTeleportHook::class.java)
    }
}
