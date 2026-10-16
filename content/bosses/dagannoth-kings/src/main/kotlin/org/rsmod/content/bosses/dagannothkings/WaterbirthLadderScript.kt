package org.rsmod.content.bosses.dagannothkings

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WaterbirthLadderScript : PluginScript() {
    override fun ScriptContext.startup() {
        for ((ladder, dest) in WaterbirthLadders.SUBLEVEL_LADDERS) {
            onOpLoc1(ladder) { climb(dest) }
        }
        onOpLoc1(CENTRAL_LADDER) { climb(WaterbirthLadders.SUBLEVEL_ENTRANCE) }
        onOpLoc3(CENTRAL_LADDER) { climb(WaterbirthLadders.SUBLEVEL_ENTRANCE) }
        onOpLoc1("loc.dagexp_entrance_ladder") { climb(WaterbirthLadders.CENTRAL_ROOM) }
        for (ladder in WaterbirthLadders.KINGS_LADDERS) {
            onOpLoc1(ladder) { climb(WaterbirthLadders.KINGS_LAIR) }
        }
        onOpLoc1("loc.dagexp_bossroomladder_up") { climb(WaterbirthLadders.KINGS_LADDER) }
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(dest)
    }

    private companion object {
        const val CENTRAL_LADDER = "loc.dagannoth_ladder_base_ext2"
    }
}
