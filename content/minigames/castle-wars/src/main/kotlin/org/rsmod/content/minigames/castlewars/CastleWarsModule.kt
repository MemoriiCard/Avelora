package org.rsmod.content.minigames.castlewars

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class CastleWarsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(CastleWarsDeathHook::class.java)
        addSetBinding<PlayerDeathCleanupHook>(CastleWarsDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(CastleWarsRespawnHook::class.java)
        addSetBinding<PvPAttackValidateHook>(CastleWarsPvPHook::class.java)
        addSetBinding<NpcAttackValidateHook>(CastleWarsBotHook::class.java)
        addSetBinding<NpcDeathKillHook>(CastleWarsBotHook::class.java)
        addSetBinding<NpcDeathDropHook>(CastleWarsBotHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(CastleWarsTeleportHook::class.java)
    }
}
