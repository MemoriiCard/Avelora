package org.rsmod.content.minigames.pestcontrol

internal enum class PcPest(
    val weight: Int,
    private val symbol: String,
    val variants: Boolean,
    val knightDamage: Int,
    val knightReach: Int,
) {
    Splatter(23, "splatter", false, 0, 0),
    Defiler(20, "defiler", true, 2, 7),
    Shifter(20, "shifter", true, 3, 2),
    Torcher(20, "torcher", true, 2, 7),
    Spinner(7, "spinner", false, 1, 2),
    Brawler(5, "brawler", false, 0, 0),
    Ravager(5, "ravager", false, 1, 2),
    ;

    fun symbol(tier: Int): String = "npc.pest_${symbol}_$tier" + if (variants) "a" else ""

    companion object {
        val TOTAL_WEIGHT = entries.sumOf { it.weight }

        fun roll(roll: Int): PcPest {
            var remaining = roll.coerceIn(0, TOTAL_WEIGHT - 1)
            for (pest in entries) {
                if (remaining < pest.weight) return pest
                remaining -= pest.weight
            }
            return Splatter
        }

        fun kindOf(symbol: String): PcPest? =
            entries.firstOrNull { symbol.startsWith("npc.pest_${it.symbol}_") }
    }
}
