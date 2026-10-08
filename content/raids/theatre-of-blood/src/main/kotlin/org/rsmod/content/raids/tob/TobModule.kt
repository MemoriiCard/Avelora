package org.rsmod.content.raids.tob

import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.content.raids.tob.raid.TobDeathCleanupHook
import org.rsmod.content.raids.tob.raid.TobRespawnHook
import org.rsmod.content.raids.tob.raid.TobRoomFactory
import org.rsmod.content.raids.tob.raid.TobSafeDeathHook
import org.rsmod.plugin.module.PluginModule

class TobModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(TobSafeDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(TobRespawnHook::class.java)
        addSetBinding<PlayerDeathCleanupHook>(TobDeathCleanupHook::class.java)
        newSetBinding<TobRoomFactory>()
    }
}
