package org.rsmod.content.bosses.kalphitequeen

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.burrowRope by intVarBit("varbit.kalphite_rope_1")
private var Player.chamberRope by intVarBit("varbit.kalphite_chamber_entrance")

class KalphiteLairScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLocU("loc.kalphite_burrow_entrance_norope", ROPE) { tieRope { player.burrowRope = 1 } }
        onOpLocU("loc.kalphite_chamber_entrance_norope", ROPE) { tieRope { player.chamberRope = 1 } }
        onOpLoc1("loc.kalphite_burrow_entrance_withrope") {
            climbDown(KalphiteLair.LAIR) { player.burrowRope = 0 }
        }
        onOpLoc1("loc.kalphite_chamber_entrance_withrope_normal") {
            climbDown(KalphiteLair.CHAMBER) { player.chamberRope = 0 }
        }
        onOpLoc1("loc.kalphite_chamber_entrance_withrope_private") {
            climbDown(KalphiteLair.CHAMBER) { player.chamberRope = 0 }
        }
        onOpLoc1("loc.kalphite_burrow_exit") { climbUp(KalphiteLair.SURFACE) }
        onOpLoc1("loc.kalphite_chamber_exit") { climbUp(KalphiteLair.CHAMBER_TOP) }
    }

    private suspend fun ProtectedAccess.tieRope(setRope: () -> Unit) {
        arriveDelay()
        if (invDel(inv, ROPE, 1).failure) return
        anim("seq.human_pickupfloor")
        mes("You tie the rope to the rock and lower it down.")
        setRope()
    }

    private suspend fun ProtectedAccess.climbDown(dest: CoordGrid, useRope: () -> Unit) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        useRope()
        telejump(dest)
    }

    private suspend fun ProtectedAccess.climbUp(dest: CoordGrid) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(dest)
    }

    private companion object {
        const val ROPE = "obj.rope"
    }
}
