package org.rsmod.content.raids.toa

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.content.raids.toa.boss.ToaBossFactory
import org.rsmod.content.raids.toa.boss.ToaKillHook
import org.rsmod.content.raids.toa.raid.ToaDeathCleanupHook
import org.rsmod.content.raids.toa.raid.ToaRespawnHook
import org.rsmod.content.raids.toa.raid.ToaRoomFactory
import org.rsmod.content.raids.toa.raid.ToaSafeDeathHook
import org.rsmod.plugin.module.PluginModule

class ToaModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(ToaSafeDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(ToaRespawnHook::class.java)
        addSetBinding<PlayerDeathCleanupHook>(ToaDeathCleanupHook::class.java)
        newSetBinding<ToaRoomFactory>()
        addSetBinding<ToaRoomFactory>(ToaBossFactory::class.java)
        addSetBinding<NpcDeathKillHook>(ToaKillHook::class.java)
    }
}
