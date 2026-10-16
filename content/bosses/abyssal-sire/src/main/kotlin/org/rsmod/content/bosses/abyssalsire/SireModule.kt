package org.rsmod.content.bosses.abyssalsire

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.plugin.module.PluginModule

class SireModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(SireAttackHook::class.java)
    }
}
