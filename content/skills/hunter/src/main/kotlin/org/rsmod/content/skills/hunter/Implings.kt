package org.rsmod.content.skills.hunter

data class Impling(
    val npcs: List<String>,
    val jar: String,
    val level: Int,
    val xp: Double,
    val catchLow: Int,
    val catchHigh: Int,
    val emptyJar: String = Implings.EMPTY_JAR,
    val noun: String = "impling",
)

object Implings {
    const val EMPTY_JAR = "obj.ii_impling_jar"
    const val NET = "obj.hunting_butterfly_net"
    const val BUTTERFLY_JAR = "obj.butterfly_jar"
    const val MAGIC_NET = "obj.ii_magic_butterfly_net"

    private val crystalNames =
        listOf(
            "johnny", "junior", "andy", "joey", "trouble", "hingy", "zolty", "neil",
            "yanny", "matty", "stace", "ian", "jamie", "damo", "xander", "steveo", "stewie",
        )

    private fun typed(type: Int, jar: String, level: Int, xp: Double, low: Int, high: Int) =
        Impling(
            npcs = listOf("npc.ii_impling_type_$type", "npc.ii_impling_type_${type}_maze"),
            jar = jar,
            level = level,
            xp = xp,
            catchLow = low,
            catchHigh = high,
        )

    private fun captured(index: Int) = "obj.ii_captured_impling_$index"

    val all: List<Impling> =
        listOf(
            typed(1, captured(1), 17, 20.0, 140, 255),
            typed(2, captured(2), 22, 32.0, 130, 255),
            typed(3, captured(3), 28, 36.0, 120, 250),
            typed(4, captured(4), 36, 48.0, 110, 245),
            typed(5, captured(5), 42, 54.0, 100, 240),
            typed(6, captured(6), 50, 60.0, 90, 235),
            typed(7, captured(7), 58, 66.0, 80, 230),
            typed(8, captured(8), 65, 216.0, 70, 225),
            typed(9, captured(9), 74, 240.0, 60, 215),
            typed(10, captured(10), 83, 300.0, 45, 200),
            typed(11, captured(11), 89, 380.0, 40, 190),
            Impling(
                npcs = crystalNames.map { "npc.ii_impling_type_12_$it" },
                jar = captured(12),
                level = 80,
                xp = 280.0,
                catchLow = 50,
                catchHigh = 205,
            ),
        )

    private fun butterfly(npc: String, jar: String, level: Int, xp: Double, low: Int, high: Int) =
        Impling(listOf(npc), jar, level, xp, low, high, BUTTERFLY_JAR, "butterfly")

    val butterflies: List<Impling> =
        listOf(
            butterfly("npc.butterfly_ruby", "obj.butterfly_jar_ruby", 15, 24.0, 150, 255),
            butterfly("npc.butterfly_glacialis", "obj.butterfly_jar_glacialis", 25, 34.0, 130, 250),
            butterfly("npc.butterfly_snowy", "obj.butterfly_jar_snowy", 35, 44.0, 110, 245),
            butterfly("npc.butterfly_warlock", "obj.butterfly_jar_warlock", 45, 54.0, 90, 240),
        )
}
