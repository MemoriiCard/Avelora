package org.rsmod.content.minigames.trawler

internal object TrawlerLoot {
    class Catch(val obj: String, val level: Int, val xp: Double)

    val fish =
        listOf(
            Catch("obj.raw_shrimp", 15, 0.7),
            Catch("obj.raw_sardine", 15, 0.7),
            Catch("obj.raw_anchovies", 15, 0.7),
            Catch("obj.raw_tuna", 35, 48.0),
            Catch("obj.raw_lobster", 40, 54.0),
            Catch("obj.raw_swordfish", 50, 60.0),
            Catch("obj.raw_shark", 76, 66.0),
            Catch("obj.raw_seaturtle", 79, 57.0),
            Catch("obj.raw_mantaray", 81, 69.0),
        )

    val junk = listOf("obj.old_boot", "obj.broken_glass", "obj.pot_empty", "obj.digsitebuttons")
    val angler = listOf("obj.trawler_reward_hat", "obj.trawler_reward_top", "obj.trawler_reward_legs", "obj.trawler_reward_boots")

    const val MIN_FISHING = 15
    const val TIER_RATIO = 4.65
    const val ANGLER_ODDS = 12
    const val PET_ODDS = 5000
    const val PET_ODDS_FULL = 2500
    const val PET = "obj.skillpetfish"
    const val JUNK_ONE = 37
    const val JUNK_TWO = 16

    fun eligible(fishing: Int): List<Catch> = fish.filter { it.level <= fishing }

    fun pick(fishing: Int, roll: (Int) -> Int): Catch? {
        val options = eligible(fishing)
        if (options.isEmpty()) return null
        val weights = options.indices.map { Math.pow(TIER_RATIO, it.toDouble()) }
        val total = weights.sum()
        var target = roll(1_000_000) / 1_000_000.0 * total
        for (index in options.indices) {
            target -= weights[index]
            if (target < 0) return options[index]
        }
        return options.last()
    }

    fun junkCount(roll: (Int) -> Int): Int {
        val value = roll(100)
        return when {
            value < JUNK_TWO -> 2
            value < JUNK_TWO + JUNK_ONE -> 1
            else -> 0
        }
    }

    fun anglerPiece(owned: (String) -> Boolean, roll: (Int) -> Int): String? {
        if (roll(ANGLER_ODDS) != 0) return null
        val missing = angler.filter { !owned(it) }
        val pool = missing.ifEmpty { angler }
        return pool[roll(pool.size)]
    }
}
