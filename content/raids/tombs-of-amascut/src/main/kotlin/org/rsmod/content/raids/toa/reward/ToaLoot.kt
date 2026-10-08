package org.rsmod.content.raids.toa.reward

import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.party.ToaMode

/** House loot tables for the Osmumten's sarcophagus; Jagex does not publish the real weights. */
object ToaLoot {
    const val MIN_POINTS = 1_500
    const val COMMON_ROLLS = 3
    const val THREAD_ODDS = 25
    const val JEWEL_ODDS = 50
    const val PET_ODDS = 350
    const val BASE_UNIQUE_PERCENT = 2
    const val MAX_UNIQUE_PERCENT = 14
    const val WEIGHT_DROP_START = 350
    const val WEIGHT_DROP_STEP = 50
    const val MIN_ROYAL_WEIGHT = 3

    const val DUNG = "obj.toa_loot_poo"
    const val THREAD = "obj.thread_of_elidinis"
    val JEWELS = listOf("obj.breach_of_the_scarab", "obj.eye_of_the_corruptor", "obj.jewel_of_the_sun")
    const val JEWEL_FINAL = "obj.jewel_of_amascut"
    val PETS = listOf("obj.wardenpet_tumeken", "obj.wardenpet_elidinis")

    const val FANG = "obj.osmumtens_fang"
    const val LIGHTBEARER = "obj.lightbearer"

    class Unique(val obj: String, val weight: Int)

    class Common(val obj: String, val min: Int, val max: Int, val weight: Int, val noted: Boolean = false)

    private val BASE_UNIQUES =
        listOf(
            Unique(FANG, 7),
            Unique(LIGHTBEARER, 7),
            Unique("obj.elidinis_ward", 2),
            Unique("obj.masori_mask", 2),
            Unique("obj.masori_body", 2),
            Unique("obj.masori_chaps", 2),
            Unique("obj.tumekens_shadow_uncharged", 1),
        )

    val UNIQUE_OBJS: List<String> = BASE_UNIQUES.map { it.obj }

    val COMMONS =
        listOf(
            Common("obj.bloodrune", 120, 360, 7),
            Common("obj.deathrune", 150, 450, 8),
            Common("obj.soulrune", 150, 450, 6),
            Common("obj.coins", 40_000, 110_000, 8),
            Common("obj.diamond", 10, 30, 4, noted = true),
            Common("obj.dragon_arrow", 100, 300, 4),
            Common("obj.runite_ore", 10, 25, 4, noted = true),
        )

    val SHROUDS =
        listOf(
            100 to "obj.icthlarins_shroud_1",
            500 to "obj.icthlarins_shroud_2",
            1000 to "obj.icthlarins_shroud_3",
            1500 to "obj.icthlarins_shroud_4",
            2000 to "obj.icthlarins_shroud_5",
        )

    fun uniques(raidLevel: Int): List<Unique> {
        val drop = ((raidLevel - WEIGHT_DROP_START) / WEIGHT_DROP_STEP + 1).coerceAtLeast(0)
        val royal = (7 - drop).coerceAtLeast(MIN_ROYAL_WEIGHT)
        return BASE_UNIQUES.map { if (it.obj == FANG || it.obj == LIGHTBEARER) Unique(it.obj, royal) else it }
    }

    fun uniquePercent(raidLevel: Int): Int =
        if (ToaMode.of(raidLevel) == ToaMode.Entry) 0
        else (BASE_UNIQUE_PERCENT + raidLevel / 50).coerceAtMost(MAX_UNIQUE_PERCENT)

    fun uniquePermille(raidLevel: Int, points: Int, totalPoints: Int, teamSize: Int): Int {
        if (points < MIN_POINTS || totalPoints <= 0) return 0
        val share = (points.toLong() * teamSize * 100 / totalPoints).toInt().coerceIn(50, 150)
        return uniquePercent(raidLevel) * 10 * share / 100
    }

    fun pointsFor(room: ToaRoom, raidLevel: Int): Int {
        val base =
            when (room) {
                ToaRoom.CrondisPuzzle, ToaRoom.ScabarasPuzzle, ToaRoom.HetPuzzle, ToaRoom.ApmekenPuzzle -> 300
                ToaRoom.Zebak, ToaRoom.Kephri, ToaRoom.Akkha, ToaRoom.Baba -> 1_500
                ToaRoom.WardensOne -> 2_500
                ToaRoom.WardensTwo -> 3_500
                ToaRoom.Nexus, ToaRoom.Vault -> 0
            }
        return base * (300 + raidLevel) / 300
    }

    fun shroudFor(completions: Int): String? = SHROUDS.lastOrNull { completions >= it.first }?.second

    fun hoodUnlocked(completions: Int): Boolean = completions >= SHROUDS.last().first

    fun pickUnique(raidLevel: Int, below: (Int) -> Int): Unique {
        val pool = uniques(raidLevel)
        var roll = below(pool.sumOf { it.weight })
        for (unique in pool) {
            if (roll < unique.weight) return unique
            roll -= unique.weight
        }
        return pool.last()
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

    fun nextJewel(owns: (String) -> Boolean): String? {
        JEWELS.firstOrNull { !owns(it) }?.let { return it }
        return JEWEL_FINAL.takeUnless(owns)
    }

    fun roll(
        raidLevel: Int,
        points: Int,
        unique: Boolean,
        owns: (String) -> Boolean,
        below: (Int) -> Int,
    ): List<CoxItem> {
        if (points < MIN_POINTS) return listOf(CoxItem(DUNG, 1))
        val loot = mutableListOf<CoxItem>()
        val bonus = raidLevel / 10
        if (unique) {
            loot += CoxItem(pickUnique(raidLevel, below).obj, 1)
        } else {
            var previous: Common? = null
            repeat(COMMON_ROLLS) {
                val common = pickCommon(previous, below)
                previous = common
                val amount = common.min + below(common.max - common.min + 1)
                loot += CoxItem(common.obj, amount + amount * bonus / 100, common.noted)
            }
        }
        if (ToaMode.of(raidLevel) == ToaMode.Entry) return loot
        if (below(THREAD_ODDS) == 0) loot += CoxItem(THREAD, 1)
        if (below(JEWEL_ODDS) == 0) nextJewel(owns)?.let { loot += CoxItem(it, 1) }
        if (below(PET_ODDS) == 0) loot += CoxItem(PETS[below(PETS.size)], 1)
        return loot
    }
}
