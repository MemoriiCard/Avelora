package org.rsmod.content.raids.cox.reward

import kotlin.math.ceil

data class CoxItem(val obj: String, val count: Int, val noted: Boolean = false)

/** The Chambers of Xeric loot tables and the roll maths behind the ancient chest. */
object CoxLoot {
    const val POINTS_PER_ROLL = 570_000
    const val MAX_ROLLS = 6
    const val UNIQUE_POINTS_PER_PERCENT = 8_675
    const val POINT_CAP = 131_071
    const val OLMLET_ODDS = 53
    const val CLUE_ODDS = 12
    const val TABLET_ODDS = 10
    const val KIT_ODDS = 75
    const val DUST_ODDS = 400
    const val SEED_PAYOUT_PERCENT = 33

    const val CLUE = "obj.trail_elite_emote_exp1"
    const val TABLET = "obj.raids_spellunlock_tablet"
    const val JOURNAL = "obj.raids_reward_book"
    const val OLMLET = "obj.olmpet"
    const val KIT = "obj.ancestral_robes_twisted_kit"
    const val DUST = "obj.raids_challenge_morph"

    class Unique(val obj: String, val normalWeight: Int, val cmWeight: Int)

    val UNIQUES =
        listOf(
            Unique("obj.raids_prayerscroll", 14, 12),
            Unique("obj.raids_prayerscroll_augury", 14, 12),
            Unique("obj.twisted_buckler", 4, 4),
            Unique("obj.dragonhunter_xbow", 4, 4),
            Unique("obj.dinhs_bulwark", 3, 3),
            Unique("obj.ancestral_hat", 4, 4),
            Unique("obj.ancestral_robe_top", 4, 4),
            Unique("obj.ancestral_robe_bottom", 4, 4),
            Unique("obj.dragon_claws", 3, 3),
            Unique("obj.elder_maul", 2, 2),
            Unique("obj.kodai_insignia", 2, 2),
            Unique("obj.twisted_bow", 2, 2),
        )

    class Common(
        val obj: String,
        val divisor: Int,
        val weight: Int,
        val noted: Boolean = false,
        val fixedCount: Int? = null,
        val seed: Common? = null,
    )

    private val HERBS =
        listOf(
            Triple("ranarr", 946, 6622),
            Triple("toadflax", 624, 4992),
            Triple("irit", 194, 1552),
            Triple("avantoe", 389, 3112),
            Triple("kwuarm", 454, 3632),
            Triple("snapdragon", 1560, 10920),
            Triple("cadantine", 396, 3168),
            Triple("lantadyme", 297, 2376),
            Triple("dwarf_weed", 240, 1920),
            Triple("torstol", 972, 6804),
        )

    val COMMONS: List<Common> =
        buildList {
            add(Common("obj.deathrune", 36, 3))
            add(Common("obj.bloodrune", 32, 3))
            add(Common("obj.soulrune", 20, 3))
            add(Common("obj.rune_arrow", 14, 3))
            add(Common("obj.dragon_arrow", 202, 3))
            for ((herb, herbDivisor, seedDivisor) in HERBS) {
                val seed = Common("obj.${herb}_seed", seedDivisor, 1)
                add(Common("obj.unidentified_$herb", herbDivisor, 2, noted = true, seed = seed))
            }
            for ((herb, _, seedDivisor) in HERBS) add(Common("obj.${herb}_seed", seedDivisor, 1))
            add(Common("obj.silver_ore", 20, 3, noted = true))
            add(Common("obj.coal", 20, 3, noted = true))
            add(Common("obj.gold_ore", 44, 3, noted = true))
            add(Common("obj.mithril_ore", 32, 3, noted = true))
            add(Common("obj.adamantite_ore", 166, 3, noted = true))
            add(Common("obj.runite_ore", 2000, 3, noted = true))
            add(Common("obj.sapphire", 188, 3, noted = true))
            add(Common("obj.emerald", 142, 3, noted = true))
            add(Common("obj.ruby", 242, 3, noted = true))
            add(Common("obj.diamond", 508, 3, noted = true))
            add(Common("obj.lizardman_fang", 28, 3, noted = true))
            add(Common("obj.blankrune_high", 2, 3, noted = true))
            add(Common("obj.hosidius_saltpetre", 28, 3, noted = true))
            add(Common("obj.plank_teak", 96, 3, noted = true))
            add(Common("obj.plank_mahogany", 238, 3, noted = true))
            add(Common("obj.lovakengj_dynamite_fused", 54, 3, noted = true))
            add(Common("obj.raids_prayerscroll_preserve", 1, 3, fixedCount = 1))
            add(Common("obj.raids_ancient_relic", 1, 3, fixedCount = 1))
        }

    private val COMMON_TOTAL = COMMONS.sumOf { it.weight }

    fun rollCount(totalPoints: Int): Int {
        val capped = totalPoints.coerceIn(0, POINTS_PER_ROLL * MAX_ROLLS)
        return ceil(capped / POINTS_PER_ROLL.toDouble()).toInt().coerceIn(0, MAX_ROLLS)
    }

    /** Chance in 1/10,000ths of each unique roll; every roll but the last is capped at 65.7%. */
    fun uniqueChances(totalPoints: Int): List<Int> {
        val capped = totalPoints.coerceIn(0, POINTS_PER_ROLL * MAX_ROLLS)
        return List(rollCount(capped)) { index ->
            val points = (capped - POINTS_PER_ROLL * index).coerceAtMost(POINTS_PER_ROLL)
            (points.toLong() * 100 / UNIQUE_POINTS_PER_PERCENT).toInt()
        }
    }

    fun pickUnique(challengeMode: Boolean, below: (Int) -> Int): Unique {
        val total = UNIQUES.sumOf { if (challengeMode) it.cmWeight else it.normalWeight }
        var roll = below(total)
        for (unique in UNIQUES) {
            roll -= if (challengeMode) unique.cmWeight else unique.normalWeight
            if (roll < 0) return unique
        }
        return UNIQUES.last()
    }

    /** Picks the player who receives a unique, weighted by personal points. */
    fun <T> pickRecipient(points: Map<T, Int>, below: (Int) -> Int): T? {
        val total = points.values.sumOf { it.coerceAtLeast(0) }
        if (total <= 0) return points.keys.firstOrNull()
        var roll = below(total)
        for ((player, amount) in points) {
            roll -= amount.coerceAtLeast(0)
            if (roll < 0) return player
        }
        return points.keys.lastOrNull()
    }

    fun rollCommon(exclude: Common? = null, below: (Int) -> Int): Common {
        var pool = COMMON_TOTAL
        if (exclude != null) pool -= exclude.weight
        var roll = below(pool)
        for (common in COMMONS) {
            if (common === exclude) continue
            roll -= common.weight
            if (roll < 0) return common
        }
        return COMMONS.last()
    }

    fun quantity(common: Common, personalPoints: Int): Int {
        common.fixedCount?.let { return it }
        return (personalPoints.coerceIn(0, POINT_CAP) / common.divisor).coerceAtLeast(1)
    }

    fun toItem(common: Common, personalPoints: Int, below: (Int) -> Int): CoxItem {
        val seed = common.seed
        val source = if (seed != null && below(100) < SEED_PAYOUT_PERCENT) seed else common
        return CoxItem(source.obj, quantity(source, personalPoints), source.noted)
    }

    /** Target time, in ticks, for the kit, the dust and the bonus points in Challenge Mode. */
    fun targetTicks(partySize: Int): Int {
        val minutes =
            when {
                partySize <= 1 -> 70
                partySize == 2 -> 65
                partySize == 3 -> 50
                partySize == 4 -> 45
                partySize <= 10 -> 42
                partySize <= 15 -> 45
                partySize <= 23 -> 60
                else -> 80
            }
        return minutes * 100
    }

    const val UNDER_TARGET_BONUS = 5_000
}
