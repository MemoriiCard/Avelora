package org.rsmod.content.minigames.soulwars

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class SoulWarsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(SoulWarsDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(SoulWarsRespawnHook::class.java)
        addSetBinding<PvPAttackValidateHook>(SoulWarsPvPHook::class.java)
        addSetBinding<NpcAttackValidateHook>(SoulWarsAvatarHook::class.java)
        addSetBinding<NpcDeathKillHook>(SoulWarsAvatarHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(SoulWarsTeleportHook::class.java)
    }
}
