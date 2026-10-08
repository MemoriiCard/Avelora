package org.rsmod.content.raids.cox.room

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Tool spawns left lying around the puzzle rooms: take one whenever you need it. */
class CoxSuppliesScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.raids_icedemon_tinderbox") { take("obj.tinderbox") }
        onOpLoc1("loc.raids_icedemon_axe") { take("obj.bronze_axe") }
        onOpLoc1("loc.raids_lasercrabs_hammer") { take("obj.hammer") }
    }

    private fun ProtectedAccess.take(obj: String) {
        if (playerContainsObj(obj)) {
            mes("You already have one of those.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim("seq.human_pickupfloor")
        invAdd(inv, obj)
    }
}
