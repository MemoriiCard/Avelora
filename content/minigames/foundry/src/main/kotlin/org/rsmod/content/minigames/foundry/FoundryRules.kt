package org.rsmod.content.minigames.foundry

import kotlin.math.floor

internal enum class Metal(val tier: Int, val level: Int, val bar: String, val label: String) {
    Bronze(1, 15, "obj.bronze_bar", "bronze"),
    Iron(2, 15, "obj.iron_bar", "iron"),
    Steel(3, 30, "obj.steel_bar", "steel"),
    Mithril(4, 50, "obj.mithril_bar", "mithril"),
    Adamant(5, 70, "obj.adamantite_bar", "adamant"),
    Rune(6, 85, "obj.runite_bar", "rune"),
}

internal enum class MouldType { Forte, Blade, Tip }

internal enum class CommissionWord(val label: String, val trait: Int) {
    Broad("Broad", 0),
    Heavy("Heavy", 2),
    Light("Light", 3),
    Narrow("Narrow", 1),
}

internal enum class CommissionStyle(val label: String, val trait: Int) {
    Flat("Flat", 5),
    Heavy("Heavy", 2),
    Light("Light", 3),
    Spiked("Spiked", 4),
}

internal class Mould(
    val name: String,
    val type: MouldType,
    val id: Int,
    val level: Int,
    val cost: Int,
    private val traits: IntArray,
) {
    val purchasable: Boolean
        get() = id >= FoundryRules.FIRST_PURCHASED_ID

    fun score(word: CommissionWord, style: CommissionStyle): Int =
        traits[word.trait] + traits[style.trait]
}

internal enum class Tool(
    val label: String,
    val interval: Int,
    val progress: Int,
    val heat: Double,
    val xp: Double,
    val animation: String,
) {
    TripHammer("trip hammer", 5, 2, -2.5, 4.5, "seq.giants_foundry_hammer01"),
    Grindstone("grindstone", 2, 1, 1.5, 3.2, "seq.giants_foundry_work_preform_loop"),
    PolishingWheel("polishing wheel", 2, 1, -1.7, 3.3, "seq.giants_foundry_polishing_wheel_spin"),
}

internal enum class Heat(val label: String) {
    Cold("cold"),
    Medium("medium"),
    Hot("hot"),
}

internal object FoundryRules {
    const val CRUCIBLE_CAPACITY = 28
    const val MIN_SMITHING = 15
    const val FIRST_PURCHASED_ID = 7
    const val ERROR_PENALTY = 10
    const val MAX_SECTIONS = 7
    const val RUINED_QUALITY = 0

    private fun m(name: String, type: MouldType, id: Int, vararg traits: Int) =
        Mould(name, type, id, 1, 0, traits)

    private fun p(name: String, type: MouldType, id: Int, level: Int, cost: Int, vararg t: Int) =
        Mould(name, type, id, level, cost, t)

    val moulds: List<Mould> =
        listOf(
            m("Chopper Forte", MouldType.Forte, 1, 4, 0, 0, 4, 0, 4),
            m("Gladius Ricasso", MouldType.Forte, 2, 4, 0, 4, 0, 0, 4),
            m("Disarming Forte", MouldType.Forte, 3, 0, 4, 0, 4, 4, 0),
            m("Medusa Ricasso", MouldType.Forte, 4, 8, 0, 6, 0, 0, 8),
            m("Serpent Ricasso", MouldType.Forte, 5, 0, 6, 0, 8, 0, 8),
            m("Serrated Forte", MouldType.Forte, 6, 0, 8, 8, 0, 6, 0),
            p("Stiletto Forte", MouldType.Forte, 7, 50, 300, 0, 8, 0, 10, 0, 8),
            p("Defender Base", MouldType.Forte, 8, 51, 300, 8, 0, 10, 0, 0, 8),
            p("Juggernaut Forte", MouldType.Forte, 9, 61, 350, 4, 0, 4, 0, 16, 0),
            p("Chopper Forte +1", MouldType.Forte, 10, 70, 400, 3, 0, 0, 4, 0, 18),
            p("Spiker!", MouldType.Forte, 11, 79, 450, 0, 1, 2, 0, 22, 0),
            m("Saw Blade", MouldType.Blade, 1, 4, 0, 0, 4, 4, 0),
            m("Defenders Edge", MouldType.Blade, 2, 4, 0, 4, 0, 4, 0),
            m("Fish Blade", MouldType.Blade, 3, 0, 4, 0, 4, 0, 4),
            m("Medusa Blade", MouldType.Blade, 4, 8, 0, 8, 0, 0, 6),
            m("Stiletto Blade", MouldType.Blade, 5, 0, 8, 0, 6, 0, 8),
            m("Gladius Edge", MouldType.Blade, 6, 0, 6, 8, 0, 0, 8),
            p("Flamberge Blade", MouldType.Blade, 7, 48, 300, 0, 8, 0, 8, 10, 0),
            p("Serpent Blade", MouldType.Blade, 8, 50, 300, 0, 10, 0, 8, 0, 8),
            p("Claymore Blade", MouldType.Blade, 9, 59, 350, 16, 0, 4, 0, 0, 4),
            p("Fleur de Blade", MouldType.Blade, 10, 71, 400, 4, 0, 18, 0, 1, 0),
            p("Choppa!", MouldType.Blade, 11, 80, 450, 1, 0, 0, 22, 0, 2),
            m("People Poker Point", MouldType.Tip, 1, 0, 4, 4, 0, 0, 4),
            m("Chopper Tip", MouldType.Tip, 2, 4, 0, 0, 4, 4, 0),
            m("Medusa's Head", MouldType.Tip, 3, 4, 0, 4, 0, 4, 0),
            m("Serpent's Fang", MouldType.Tip, 4, 0, 8, 0, 6, 8, 0),
            m("Gladius Point", MouldType.Tip, 5, 0, 8, 8, 0, 0, 6),
            m("Saw Tip", MouldType.Tip, 6, 6, 0, 8, 0, 8, 0),
            p("Corrupted Point", MouldType.Tip, 7, 49, 300, 0, 8, 0, 10, 8, 0),
            p("Defenders Tip", MouldType.Tip, 8, 52, 300, 10, 0, 8, 0, 8, 0),
            p("Serrated Tip", MouldType.Tip, 9, 60, 350, 0, 4, 0, 16, 4, 0),
            p("Needle Point", MouldType.Tip, 10, 69, 400, 0, 18, 0, 3, 0, 4),
            p("The Point!", MouldType.Tip, 11, 81, 450, 2, 0, 0, 1, 0, 22),
        )

    fun mould(type: MouldType, id: Int): Mould? = moulds.firstOrNull { it.type == type && it.id == id }

    fun moulds(type: MouldType): List<Mould> = moulds.filter { it.type == type }

    fun metalScore(counts: IntArray): Int {
        val values =
            Metal.entries
                .map { 10.0 * it.tier * counts[it.ordinal] / CRUCIBLE_CAPACITY }
                .sortedDescending()
        val v1 = values[0]
        val v2 = values[1]
        return ((floor(10 * v1) + floor(10 * v2) + floor(v1 * v2)) / 10).toInt()
    }

    fun mouldScore(word: CommissionWord, style: CommissionStyle, picks: List<Mould>): Int =
        picks.sumOf { it.score(word, style) }

    fun sections(metalScore: Int): Int =
        when {
            metalScore >= 120 -> 7
            metalScore >= 90 -> 6
            metalScore >= 60 -> 5
            metalScore >= 20 -> 4
            else -> 3
        }

    fun bandWidth(metalScore: Int): Double = 33.0 * (1.0 - metalScore.coerceIn(0, 130) / 260.0)

    fun heatOf(temperature: Double, metalScore: Int): Heat? {
        val width = bandWidth(metalScore)
        return when {
            temperature <= width -> Heat.Cold
            temperature >= 100 - width -> Heat.Hot
            temperature in (50 - width / 2)..(50 + width / 2) -> Heat.Medium
            else -> null
        }
    }

    fun requiredHeat(tool: Tool): Heat =
        when (tool) {
            Tool.TripHammer -> Heat.Hot
            Tool.Grindstone -> Heat.Medium
            Tool.PolishingWheel -> Heat.Cold
        }

    fun toolFor(seed: Int): Tool = Tool.entries[Math.floorMod(seed, Tool.entries.size)]

    fun sectionOf(completion: Int, sections: Int): Int =
        (completion * sections / 100).coerceAtMost(sections - 1)

    fun handInXp(quality: Int): Int = (quality * quality / 73 + (quality * 3) / 2 + 1) * 30

    fun handInCoins(quality: Int): Int = handInXp(quality) * 2

    fun reputation(quality: Int): Int = quality
}
