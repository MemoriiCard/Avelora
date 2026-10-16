package org.rsmod.content.skills.prayer.items.bonecrusher

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType

internal sealed class BonecrusherUnchargeResult {
    data object WrongItem : BonecrusherUnchargeResult()
    data object NoCharges : BonecrusherUnchargeResult()
    data object CannotRedeemEcto : BonecrusherUnchargeResult()
    data object NoInvSpace : BonecrusherUnchargeResult()
    data class Success(
        val ectoTokens: Int,
        val chargesRemoved: Int,
        val remainingCharges: Int,
    ) : BonecrusherUnchargeResult()
}

internal fun Player.chargeCrusherItemWithEcto(
    crusherSlot: Int,
    tokenSlot: Int,
): Int? {
    val room = (MAX_CHARGES - bonecrusherCharges) / CHARGES_PER_TOKEN
    val tokens = minOf(inv.count("obj.ectotoken"), room)
    if (tokens <= 0) {
        return null
    }
    if (invDel(inv, "obj.ectotoken", count = tokens, slot = tokenSlot).failure) {
        return null
    }
    bonecrusherCharges += tokens * CHARGES_PER_TOKEN
    return bonecrusherCharges
}

private const val MAX_CHARGES = 60_000
private const val CHARGES_PER_TOKEN = 25

internal fun Player.tryUnchargeBonecrusher(
    inv: Inventory,
    slot: Int,
    crusherInternal: String,
): BonecrusherUnchargeResult {
    inv[slot]?.takeIf { it.isType(crusherInternal) } ?: return BonecrusherUnchargeResult.WrongItem

    val storedCharges = bonecrusherCharges
    if (storedCharges == 0) {
        return BonecrusherUnchargeResult.NoCharges
    }
    if (storedCharges < 25) {
        return BonecrusherUnchargeResult.CannotRedeemEcto
    }

    val redeemableTokens = storedCharges / 25
    val chargesToRemove = redeemableTokens * 25

    val added = invAdd(inv, "obj.ectotoken", count = redeemableTokens)
    if (added.failure) {
        return BonecrusherUnchargeResult.NoInvSpace
    }

    bonecrusherCharges = (storedCharges - chargesToRemove).coerceAtLeast(0)

    val remaining = storedCharges - chargesToRemove
    return BonecrusherUnchargeResult.Success(redeemableTokens, chargesToRemove, remaining)
}

private var Player.bonecrusherCharges by intVarBit("varbit.charges_bonecrusher_quantity")
