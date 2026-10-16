package org.rsmod.content.minigames.wintertodt

class Tier(val obj: String, val level: Int, val min: Int, val max: Int = min)

class Ladder(val stat: String, val weight: Int, val tiers: List<Tier>)

fun interface Levels {
    fun of(stat: String): Int
}

class Drop(val obj: String, val count: Int)

object WintertodtLoot {
    const val UNIQUE_ODDS = 45
    const val MAX_ROLLS = 8000
    const val PET_ODDS = 5000
    const val PET = "obj.phoenixpet"

    val ladders =
        listOf(
            Ladder(
                "stat.herblore",
                20,
                listOf(
                    Tier("obj.unidentified_guam", 1, 3, 6),
                    Tier("obj.unidentified_harralander", 20, 3, 6),
                    Tier("obj.unidentified_ranarr", 30, 2, 5),
                    Tier("obj.unidentified_irit", 40, 2, 5),
                    Tier("obj.unidentified_kwuarm", 55, 2, 4),
                    Tier("obj.unidentified_cadantine", 66, 2, 4),
                    Tier("obj.unidentified_dwarf_weed", 70, 1, 3),
                    Tier("obj.unidentified_torstol", 85, 1, 3),
                ),
            ),
            Ladder(
                "stat.mining",
                20,
                listOf(
                    Tier("obj.tin_ore", 1, 8, 20),
                    Tier("obj.iron_ore", 15, 8, 20),
                    Tier("obj.coal", 30, 10, 25),
                    Tier("obj.mithril_ore", 55, 4, 10),
                    Tier("obj.adamantite_ore", 70, 3, 7),
                    Tier("obj.runite_ore", 85, 1, 2),
                ),
            ),
            Ladder(
                "stat.fishing",
                15,
                listOf(
                    Tier("obj.raw_trout", 20, 8, 20),
                    Tier("obj.raw_lobster", 40, 6, 15),
                    Tier("obj.raw_swordfish", 50, 6, 12),
                    Tier("obj.raw_shark", 76, 4, 8),
                    Tier("obj.raw_anglerfish", 82, 3, 6),
                ),
            ),
            Ladder(
                "stat.crafting",
                10,
                listOf(
                    Tier("obj.uncut_sapphire", 20, 2, 4),
                    Tier("obj.uncut_emerald", 27, 2, 4),
                    Tier("obj.uncut_ruby", 34, 1, 3),
                    Tier("obj.uncut_diamond", 43, 1, 2),
                ),
            ),
            Ladder(
                "stat.woodcutting",
                15,
                listOf(
                    Tier("obj.logs", 1, 10, 25),
                    Tier("obj.maple_logs", 45, 8, 20),
                    Tier("obj.yew_logs", 60, 6, 15),
                    Tier("obj.magic_logs", 75, 4, 10),
                ),
            ),
            Ladder(
                "stat.farming",
                10,
                listOf(
                    Tier("obj.ranarr_seed", 32, 1, 2),
                    Tier("obj.snapdragon_seed", 62, 1, 2),
                    Tier("obj.torstol_seed", 85, 1),
                ),
            ),
            Ladder("stat.hitpoints", 10, listOf(Tier("obj.coins", 1, 800, 2500))),
        )

    val uniques =
        listOf(
            Tier("obj.pyromancer_hood", 1, 1) to 10,
            Tier("obj.pyromancer_top", 1, 1) to 10,
            Tier("obj.pyromancer_bottom", 1, 1) to 10,
            Tier("obj.pyromancer_boots", 1, 1) to 10,
            Tier("obj.pyromancer_gloves", 1, 1) to 10,
            Tier("obj.wint_torch", 1, 1) to 20,
            Tier("obj.tome_of_fire_uncharged", 1, 1) to 6,
            Tier("obj.wint_burnt_page", 1, 20, 60) to 25,
            Tier("obj.dragon_axe", 1, 1) to 1,
        )

    fun rolls(points: Int, nextInt: (Int) -> Int): Int {
        if (points < WintertodtRound.MIN_POINTS) return 0
        val extra = points - WintertodtRound.MIN_POINTS
        val chance = extra % WintertodtRound.MIN_POINTS
        val bonus = if (nextInt(WintertodtRound.MIN_POINTS) < chance) 1 else 0
        return (BASE_ROLLS + extra / WintertodtRound.MIN_POINTS + bonus).coerceAtMost(MAX_ROLLS)
    }

    fun roll(levels: Levels, nextInt: (Int) -> Int): Drop {
        if (nextInt(UNIQUE_ODDS) == 0) {
            val total = uniques.sumOf { it.second }
            var pick = nextInt(total)
            for ((tier, weight) in uniques) {
                if (pick < weight) return Drop(tier.obj, count(tier, nextInt))
                pick -= weight
            }
        }
        val ladder = pickLadder(nextInt)
        val level = levels.of(ladder.stat)
        val top = ladder.tiers.indexOfLast { it.level <= level }.coerceAtLeast(0)
        val tier = ladder.tiers[(top - nextInt(TIERS_BACK + 1)).coerceAtLeast(0)]
        return Drop(tier.obj, count(tier, nextInt))
    }

    private fun pickLadder(nextInt: (Int) -> Int): Ladder {
        var pick = nextInt(ladders.sumOf { it.weight })
        for (ladder in ladders) {
            if (pick < ladder.weight) return ladder
            pick -= ladder.weight
        }
        return ladders.last()
    }

    private fun count(tier: Tier, nextInt: (Int) -> Int): Int =
        tier.min + if (tier.max > tier.min) nextInt(tier.max - tier.min + 1) else 0

    private const val BASE_ROLLS = 2
    private const val TIERS_BACK = 2
}
