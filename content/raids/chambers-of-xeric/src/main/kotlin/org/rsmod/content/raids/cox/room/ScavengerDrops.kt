package org.rsmod.content.raids.cox.room

import org.rsmod.api.random.GameRandom

/** The scavenger beast's 18-weight drop table. Rolling any potion secondary drops all three. */
object ScavengerDrops {
    private val TOOLS =
        listOf(
            "obj.fishing_rod",
            "obj.iron_pickaxe",
            "obj.iron_axe",
            "obj.hunting_butterfly_net",
            "obj.hammer",
            "obj.tinderbox",
        )

    const val TOTAL_WEIGHT = 18

    fun roll(random: GameRandom): List<Pair<String, Int>> {
        val pick = random.of(0, TOTAL_WEIGHT - 1)
        return when {
            pick < TOOLS.size -> listOf(TOOLS[pick] to 1)
            pick < 8 -> listOf("obj.lockpick" to 1)
            pick < 10 -> listOf("obj.raids_fishingbait" to random.of(30, 50))
            pick < 12 -> listOf("obj.raids_plank" to 2)
            else ->
                listOf(
                    "obj.raids_endarkened_juice" to random.of(5, 14),
                    "obj.raids_stinkhorn_mushroom" to random.of(3, 11),
                    "obj.raids_cicely" to random.of(3, 6),
                )
        }
    }
}
