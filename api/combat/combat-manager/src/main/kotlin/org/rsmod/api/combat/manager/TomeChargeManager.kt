package org.rsmod.api.combat.manager

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellChecks
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.lefthand
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

public class TomeChargeManager @Inject constructor(private val charges: ObjChargeManager) {
    /** Spends one charge of a worn Tome of Water when [spell] is one of the spells it boosts. */
    public fun consume(player: Player, spell: MagicSpell) {
        val boosted =
            MagicSpellChecks.isWaterSpell(spell.obj) ||
                MagicSpellChecks.isBindSpell(spell.obj) ||
                MagicSpellChecks.isCurseSpell(spell.obj)
        if (!boosted || !player.lefthand.isType("obj.tome_of_water")) return
        charges.reduceWornCharges(player, Wearpos.LeftHand, VAROBJ, 1)
    }

    public companion object {
        public const val VAROBJ: String = "varobj.charges_16383"
    }
}
