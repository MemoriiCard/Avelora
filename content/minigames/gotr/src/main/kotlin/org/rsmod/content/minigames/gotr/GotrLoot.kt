package org.rsmod.content.minigames.gotr

object GotrLoot {
    class Prize(val obj: String, val min: Int, val max: Int, val weight: Int, val rune: GotrRune? = null)

    private val talismans =
        listOf(
            GotrRune.Air to 48,
            GotrRune.Water to 48,
            GotrRune.Earth to 48,
            GotrRune.Fire to 48,
            GotrRune.Mind to 64,
            GotrRune.Body to 64,
            GotrRune.Chaos to 64,
            GotrRune.Cosmic to 64,
            GotrRune.Nature to 64,
        )

    val table =
        listOf(
            Prize("obj.airrune", 400, 500, 10, GotrRune.Air),
            Prize("obj.waterrune", 400, 500, 10, GotrRune.Water),
            Prize("obj.earthrune", 400, 500, 10, GotrRune.Earth),
            Prize("obj.firerune", 400, 500, 10, GotrRune.Fire),
            Prize("obj.mindrune", 250, 400, 10, GotrRune.Mind),
            Prize("obj.bodyrune", 80, 150, 10, GotrRune.Body),
            Prize("obj.chaosrune", 61, 150, 4, GotrRune.Chaos),
            Prize("obj.cosmicrune", 20, 30, 4, GotrRune.Cosmic),
            Prize("obj.naturerune", 28, 150, 4, GotrRune.Nature),
            Prize("obj.lawrune", 5, 120, 4, GotrRune.Law),
            Prize("obj.deathrune", 5, 120, 4, GotrRune.Death),
            Prize("obj.bloodrune", 5, 120, 4, GotrRune.Blood),
            Prize("obj.abyssal_pearl", 14, 16, 18),
            Prize("obj.gotr_intricate_pouch", 1, 1, 5),
            Prize("obj.abyssal_needle", 1, 1, 1),
        )

    const val TALISMAN_WEIGHT = 16
    const val LANTERN_ODDS = 700
    const val NEEDLE_ODDS = 300
    const val PEARL_SEARCH_COST = 25
    const val MAX_POINTS = 65_535
    private const val RUNE_LEVEL_FACTOR = 5

    private val total = table.sumOf { it.weight } + TALISMAN_WEIGHT

    class Result(val obj: String, val amount: Int)

    fun roll(runecraft: Int, roll: (Int) -> Int): Result {
        if (roll(LANTERN_ODDS) == 0) return Result("obj.abyssal_lantern", 1)
        var pick = roll(total)
        for (prize in table) {
            if (pick < prize.weight) return result(prize, runecraft, roll)
            pick -= prize.weight
        }
        return talisman(roll)
    }

    private fun talisman(roll: (Int) -> Int): Result {
        var pick = roll(talismans.sumOf { it.second })
        for ((rune, weight) in talismans) {
            if (pick < weight) return Result(rune.talismanObj, 1)
            pick -= weight
        }
        return Result(GotrRune.Air.talismanObj, 1)
    }

    private fun result(prize: Prize, runecraft: Int, roll: (Int) -> Int): Result {
        val amount = prize.min + roll(prize.max - prize.min + 1)
        val rune = prize.rune
        val scaled = if (rune != null && runecraft < rune.level) (amount / RUNE_LEVEL_FACTOR).coerceAtLeast(1) else amount
        return Result(prize.obj, scaled)
    }
}
