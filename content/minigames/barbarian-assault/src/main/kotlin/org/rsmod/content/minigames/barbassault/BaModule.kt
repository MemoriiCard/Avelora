package org.rsmod.content.minigames.barbassault

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class BaModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(BaDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(BaRespawnHook::class.java)
        addSetBinding<NpcAttackValidateHook>(BaMonsterHook::class.java)
        addSetBinding<NpcDeathKillHook>(BaMonsterHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(BaTeleportHook::class.java)
    }
}
