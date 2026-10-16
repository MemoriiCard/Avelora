package org.rsmod.content.bosses.phosani

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.pools by intVarBit("varbit.nightmare_challenge_enabled")

class Phosani @Inject constructor(private val deps: BossDeps) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, phosaniSpec(), deps)
        BossCombat.register(
            this,
            phosaniHuskSpec(HUSK_MAGIC, "spotanim.nightmare_husk_magic_travel", Magic),
            deps,
        )
        BossCombat.register(
            this,
            phosaniHuskSpec(HUSK_RANGED, "spotanim.nightmare_husk_ranged_travel", Ranged),
            deps,
        )
        onPlayerLogin { if (player.pools == 0) player.pools = 1 }
    }
}
