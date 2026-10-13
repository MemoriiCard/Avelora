package org.rsmod.content.minigames.foundry

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.crucibleState by intVarBit("varbit.giants_foundry_crucible_state")
internal var Player.jigState by intVarBit("varbit.giants_foundry_mould_state")
internal var Player.preformStored by intVarBit("varbit.giants_foundry_preform_stored")
internal var Player.preformQuality by intVarBit("varbit.giants_foundry_preform_quality")
internal var Player.preformStartQuality by intVarBit("varbit.giants_foundry_preform_start_quality")
internal var Player.preformMetalScore by intVarBit("varbit.giants_foundry_preform_dificulty")
internal var Player.preformTemperature by intVarBit("varbit.giants_foundry_preform_temperature")
internal var Player.preformCompletion by intVarBit("varbit.giants_foundry_preform_completion")
internal var Player.commissionWordId by intVarBit("varbit.giants_foundry_commission_word_1")
internal var Player.commissionStyleId by intVarBit("varbit.giants_foundry_commission_word_2")
internal var Player.reputation by intVarp("varp.giants_foundry_reward_shop_points")

private var Player.selectedForte by intVarBit("varbit.giants_foundry_mould_selected_ricasso")
private var Player.selectedBlade by intVarBit("varbit.giants_foundry_mould_selected_blade")
private var Player.selectedTip by intVarBit("varbit.giants_foundry_mould_selected_tip")

private var Player.bronzeCount by intVarBit("varbit.giants_foundry_bronze_count")
private var Player.ironCount by intVarBit("varbit.giants_foundry_iron_count")
private var Player.steelCount by intVarBit("varbit.giants_foundry_steel_count")
private var Player.mithrilCount by intVarBit("varbit.giants_foundry_mithril_count")
private var Player.adamantCount by intVarBit("varbit.giants_foundry_adamant_count")
private var Player.runeCount by intVarBit("varbit.giants_foundry_rune_count")

private var Player.seed0 by intVarBit("varbit.giants_foundry_seed_0")
private var Player.seed1 by intVarBit("varbit.giants_foundry_seed_1")
private var Player.seed2 by intVarBit("varbit.giants_foundry_seed_2")
private var Player.seed3 by intVarBit("varbit.giants_foundry_seed_3")
private var Player.seed4 by intVarBit("varbit.giants_foundry_seed_4")
private var Player.seed5 by intVarBit("varbit.giants_foundry_seed_5")
private var Player.seed6 by intVarBit("varbit.giants_foundry_seed_6")

private var Player.forte7 by intVarBit("varbit.giants_foundry_unlocked_mould_ricasso_7")
private var Player.forte8 by intVarBit("varbit.giants_foundry_unlocked_mould_ricasso_8")
private var Player.forte9 by intVarBit("varbit.giants_foundry_unlocked_mould_ricasso_9")
private var Player.forte10 by intVarBit("varbit.giants_foundry_unlocked_mould_ricasso_10")
private var Player.forte11 by intVarBit("varbit.giants_foundry_unlocked_mould_ricasso_11")
private var Player.blade7 by intVarBit("varbit.giants_foundry_unlocked_mould_blade_7")
private var Player.blade8 by intVarBit("varbit.giants_foundry_unlocked_mould_blade_8")
private var Player.blade9 by intVarBit("varbit.giants_foundry_unlocked_mould_blade_9")
private var Player.blade10 by intVarBit("varbit.giants_foundry_unlocked_mould_blade_10")
private var Player.blade11 by intVarBit("varbit.giants_foundry_unlocked_mould_blade_11")
private var Player.tip7 by intVarBit("varbit.giants_foundry_unlocked_mould_tip_7")
private var Player.tip8 by intVarBit("varbit.giants_foundry_unlocked_mould_tip_8")
private var Player.tip9 by intVarBit("varbit.giants_foundry_unlocked_mould_tip_9")
private var Player.tip10 by intVarBit("varbit.giants_foundry_unlocked_mould_tip_10")
private var Player.tip11 by intVarBit("varbit.giants_foundry_unlocked_mould_tip_11")

internal fun Player.metalCount(metal: Metal): Int =
    when (metal) {
        Metal.Bronze -> bronzeCount
        Metal.Iron -> ironCount
        Metal.Steel -> steelCount
        Metal.Mithril -> mithrilCount
        Metal.Adamant -> adamantCount
        Metal.Rune -> runeCount
    }

internal fun Player.setMetalCount(metal: Metal, count: Int) {
    when (metal) {
        Metal.Bronze -> bronzeCount = count
        Metal.Iron -> ironCount = count
        Metal.Steel -> steelCount = count
        Metal.Mithril -> mithrilCount = count
        Metal.Adamant -> adamantCount = count
        Metal.Rune -> runeCount = count
    }
}

internal fun Player.crucibleCounts(): IntArray = IntArray(Metal.entries.size) { metalCount(Metal.entries[it]) }

internal fun Player.crucibleTotal(): Int = crucibleCounts().sum()

internal fun Player.selectedMouldId(type: MouldType): Int =
    when (type) {
        MouldType.Forte -> selectedForte
        MouldType.Blade -> selectedBlade
        MouldType.Tip -> selectedTip
    }

internal fun Player.selectMould(type: MouldType, id: Int) {
    when (type) {
        MouldType.Forte -> selectedForte = id
        MouldType.Blade -> selectedBlade = id
        MouldType.Tip -> selectedTip = id
    }
}

internal fun Player.hasMould(mould: Mould): Boolean =
    !mould.purchasable ||
        when (mould.type) {
            MouldType.Forte ->
                when (mould.id) {
                    7 -> forte7
                    8 -> forte8
                    9 -> forte9
                    10 -> forte10
                    else -> forte11
                }
            MouldType.Blade ->
                when (mould.id) {
                    7 -> blade7
                    8 -> blade8
                    9 -> blade9
                    10 -> blade10
                    else -> blade11
                }
            MouldType.Tip ->
                when (mould.id) {
                    7 -> tip7
                    8 -> tip8
                    9 -> tip9
                    10 -> tip10
                    else -> tip11
                }
        } == 1

internal fun Player.unlockMould(mould: Mould) {
    when (mould.type) {
        MouldType.Forte ->
            when (mould.id) {
                7 -> forte7 = 1
                8 -> forte8 = 1
                9 -> forte9 = 1
                10 -> forte10 = 1
                else -> forte11 = 1
            }
        MouldType.Blade ->
            when (mould.id) {
                7 -> blade7 = 1
                8 -> blade8 = 1
                9 -> blade9 = 1
                10 -> blade10 = 1
                else -> blade11 = 1
            }
        MouldType.Tip ->
            when (mould.id) {
                7 -> tip7 = 1
                8 -> tip8 = 1
                9 -> tip9 = 1
                10 -> tip10 = 1
                else -> tip11 = 1
            }
    }
}

internal fun Player.sectionSeed(index: Int): Int =
    when (index) {
        0 -> seed0
        1 -> seed1
        2 -> seed2
        3 -> seed3
        4 -> seed4
        5 -> seed5
        else -> seed6
    }

internal fun Player.setSectionSeed(index: Int, value: Int) {
    when (index) {
        0 -> seed0 = value
        1 -> seed1 = value
        2 -> seed2 = value
        3 -> seed3 = value
        4 -> seed4 = value
        5 -> seed5 = value
        else -> seed6 = value
    }
}
