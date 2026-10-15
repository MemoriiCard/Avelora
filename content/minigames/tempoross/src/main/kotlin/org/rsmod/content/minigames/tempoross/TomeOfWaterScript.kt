package org.rsmod.content.minigames.tempoross

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.manager.TomeChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TomeOfWaterScript @Inject constructor(private val charges: ObjChargeManager) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tome in listOf(EMPTY, CHARGED)) {
            onOpHeldU(PAGE, tome) { charge(it.secondSlot) }
        }
    }

    private fun ProtectedAccess.charge(slot: Int) {
        val current = charges.getCharges(inv[slot], TomeChargeManager.VAROBJ)
        val pages = min(invTotal(inv, PAGE), (MAX_CHARGES - current) / CHARGES_PER_PAGE)
        if (pages <= 0) {
            mes(if (current >= MAX_CHARGES - CHARGES_PER_PAGE + 1) "Your tome is already full." else "You have no soaked pages.")
            return
        }
        if (invDel(inv, PAGE, pages).failure) return
        charges.addCharges(inv, slot, pages * CHARGES_PER_PAGE, TomeChargeManager.VAROBJ, MAX_CHARGES)
        mes("You add $pages soaked pages to your tome. It now holds ${current + pages * CHARGES_PER_PAGE} charges.")
    }

    companion object {
        const val PAGE = "obj.soaked_page"
        const val EMPTY = "obj.tome_of_water_uncharged"
        const val CHARGED = "obj.tome_of_water"
        const val CHARGES_PER_PAGE = 20
        const val MAX_CHARGES = 16_383
    }
}
