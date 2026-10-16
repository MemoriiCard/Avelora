package org.rsmod.content.bosses.smokedevil

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ThermonuclearSmokeDevil
@Inject
constructor(private val deps: BossDeps, private val players: PlayerList) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, thermySpec(), deps)
        onOpLoc1(CAVE_ENTRANCE) { telejump(SmokeDevilDungeon.DUNGEON) }
        onOpLoc1(CAVE_EXIT) { telejump(SmokeDevilDungeon.SURFACE) }
        onOpLoc1(LAIR_ENTRANCE) { if (canEnterLair()) telejump(SmokeDevilDungeon.INSIDE_LAIR) }
        onOpLoc2(LAIR_ENTRANCE) { peek() }
        onOpLoc1(LAIR_EXIT) { telejump(SmokeDevilDungeon.OUTSIDE_LAIR) }
    }

    private fun ProtectedAccess.canEnterLair(): Boolean {
        if (player.statBase("stat.slayer") < SmokeDevilDungeon.SLAYER_LEVEL) {
            mes("You need a Slayer level of ${SmokeDevilDungeon.SLAYER_LEVEL} to enter.")
            return false
        }
        val task = SlayerTaskManager.getCurrentSlayerTask(this)
        val onTask = task != null && TASK_TYPES.any { SlayerTaskManager.isTaskNpcType(it, task.id) }
        if (!onTask) {
            mes("You need to be on a Smoke devil Slayer task to enter.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.peek() {
        val inside = players.count { SmokeDevilDungeon.inLair(it.coords) }
        mes(if (inside == 1) "You peek inside and see 1 adventurer." else "You peek inside and see $inside adventurers.")
    }

    private companion object {
        const val CAVE_ENTRANCE = "loc.smokedevil_cave_entrance"
        const val CAVE_EXIT = "loc.slayer_cave_passage_smokedevil_exit"
        const val LAIR_ENTRANCE = "loc.slayer_cave_smokedevil_boss_entrance"
        const val LAIR_EXIT = "loc.slayer_cave_smokedevil_boss_exit"

        val TASK_TYPES by lazy {
            listOf(SMOKE_DEVIL, THERMY).mapNotNull { ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC)) }
        }
    }
}
