package org.rsmod.content.raids.tob.reward

import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.tob.party.TobMode

/** House loot tables for the Monumental chest; the weights are not documented by Jagex. */
object TobLoot {
    const val UNIQUE_PERCENT = 11
    const val HARD_UNIQUE_PERCENT = 13
    const val COMMON_ROLLS = 3
    const val HARD_BONUS_PERCENT = 15
    const val PET_ODDS = 650
    const val HARD_PET_ODDS = 500
    const val KIT_ODDS = 100
    const val DUST_ODDS = 50

    const val PET = "obj.verzikpet"
    const val HARD_KIT = "obj.tob_hardmode_kit"
    const val HARD_KIT_BLOOD = "obj.tob_hardmode_kit_blood"
    const val HARD_DUST = "obj.tob_hardmode_dust"

    class Unique(val obj: String, val weight: Int)

    class Common(val obj: String, val min: Int, val max: Int, val weight: Int, val noted: Boolean = false)

    val UNIQUES =
        listOf(
            Unique("obj.infernal_defender_hilt", 8),
            Unique("obj.ghrazi_rapier", 2),
            Unique("obj.sanguinesti_staff_uncharged", 2),
            Unique("obj.justiciar_faceguard", 2),
            Unique("obj.justiciar_chestguard", 2),
            Unique("obj.justiciar_leg_guards", 2),
            Unique("obj.scythe_of_vitur_uncharged", 1),
        )

    val COMMONS =
        listOf(
            Common("obj.bloodrune", 150, 450, 8),
            Common("obj.deathrune", 150, 450, 8),
            Common("obj.soulrune", 150, 450, 6),
            Common("obj.vial_blood", 10, 30, 6),
            Common("obj.coins", 40_000, 120_000, 8),
            Common("obj.diamond", 10, 30, 4, noted = true),
            Common("obj.dragon_arrow", 100, 300, 4),
            Common("obj.runite_ore", 10, 25, 4, noted = true),
        )

    val SHROUDS =
        listOf(
            100 to "obj.sinhaza_shroud_tier1",
            500 to "obj.sinhaza_shroud_tier2",
            1000 to "obj.sinhaza_shroud_tier3",
            1500 to "obj.sinhaza_shroud_tier4",
            2000 to "obj.sinhaza_shroud_tier5",
        )

    fun uniquePercent(mode: TobMode): Int =
        when (mode) {
            TobMode.Entry -> 0
            TobMode.Normal -> UNIQUE_PERCENT
            TobMode.Hard -> HARD_UNIQUE_PERCENT
        }

    fun shroudFor(completions: Int): String? = SHROUDS.lastOrNull { completions >= it.first }?.second

    fun nextShroud(completions: Int): Int? = SHROUDS.firstOrNull { completions < it.first }?.first

    fun pickUnique(below: (Int) -> Int): Unique {
        var roll = below(UNIQUES.sumOf { it.weight })
        for (unique in UNIQUES) {
            if (roll < unique.weight) return unique
            roll -= unique.weight
        }
        return UNIQUES.last()
    }

    fun pickCommon(exclude: Common?, below: (Int) -> Int): Common {
        val pool = COMMONS.filter { it !== exclude }
        var roll = below(pool.sumOf { it.weight })
        for (common in pool) {
            if (roll < common.weight) return common
            roll -= common.weight
        }
        return pool.last()
    }

    fun roll(mode: TobMode, below: (Int) -> Int): List<CoxItem> {
        val loot = mutableListOf<CoxItem>()
        val unique = below(100) < uniquePercent(mode)
        val rolls = if (unique) 1 else COMMON_ROLLS
        if (unique) loot += CoxItem(pickUnique(below).obj, 1)
        var previous: Common? = null
        repeat(rolls) {
            val common = pickCommon(previous, below)
            previous = common
            var amount = common.min + below(common.max - common.min + 1)
            if (mode == TobMode.Hard) amount += amount * HARD_BONUS_PERCENT / 100
            loot += CoxItem(common.obj, amount, common.noted)
        }
        if (mode == TobMode.Hard) {
            if (below(KIT_ODDS) == 0) {
                loot += CoxItem(if (below(2) == 0) HARD_KIT else HARD_KIT_BLOOD, 1)
            }
            if (below(DUST_ODDS) == 0) loot += CoxItem(HARD_DUST, 1)
        }
        val petOdds = if (mode == TobMode.Hard) HARD_PET_ODDS else PET_ODDS
        if (mode != TobMode.Entry && below(petOdds) == 0) loot += CoxItem(PET, 1)
        return loot
    }
}
