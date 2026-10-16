package org.rsmod.content.minigames.tempoross

object TemporossLoot {
    const val FIRST_PERMIT_POINTS = 2000
    const val PERMIT_POINTS = 700
    const val MAX_PERMITS = 50

    class Drop(val obj: String, val min: Int, val max: Int, val weight: Int)

    val table =
        listOf(
            Drop("obj.tempoross_reward_casket", 1, 1, 400),
            Drop("obj.soaked_page", 5, 25, 149),
            Drop("obj.fish_barrel_closed", 1, 1, 20),
            Drop("obj.tackle_box", 1, 1, 20),
            Drop("obj.tome_of_water_uncharged", 1, 1, 5),
            Drop("obj.dragon_harpoon", 1, 1, 1),
            Drop("obj.temporosspet", 1, 1, 1),
        )

    private val flakes = Drop("obj.spirit_flakes", 32, 64, 0)

    private const val DENOMINATOR = 8000

    fun permits(points: Int, roll: (Int) -> Int): Int {
        if (points < FIRST_PERMIT_POINTS) return 0
        val beyond = points - FIRST_PERMIT_POINTS
        var permits = 1 + beyond / PERMIT_POINTS
        if (roll(PERMIT_POINTS) < beyond % PERMIT_POINTS) permits++
        return permits.coerceAtMost(MAX_PERMITS)
    }

    fun roll(roll: (Int) -> Int): Pair<Drop, Int> {
        var pick = roll(DENOMINATOR)
        for (drop in table) {
            if (pick < drop.weight) return drop to drop.min + roll(drop.max - drop.min + 1)
            pick -= drop.weight
        }
        return flakes to flakes.min + roll(flakes.max - flakes.min + 1)
    }
}
