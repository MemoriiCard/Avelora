package org.rsmod.content.minigames.gotr

enum class GotrKind {
    Elemental,
    Catalytic,
}

enum class GotrTier(val cellEnergy: Int, val guardianEnergy: Int, val cellObj: String) {
    Weak(17, 6, "obj.gotr_cell_tier1"),
    Medium(20, 15, "obj.gotr_cell_tier2"),
    Strong(24, 27, "obj.gotr_cell_tier3"),
    Overcharged(30, 45, "obj.gotr_cell_tier4"),
}

enum class GotrRune(
    val kind: GotrKind,
    val tier: GotrTier,
    val level: Int,
    val xp: Double,
    val multiplier: Int,
) {
    Air(GotrKind.Elemental, GotrTier.Weak, 1, 5.0, 11),
    Water(GotrKind.Elemental, GotrTier.Medium, 5, 6.0, 19),
    Earth(GotrKind.Elemental, GotrTier.Strong, 9, 6.5, 26),
    Fire(GotrKind.Elemental, GotrTier.Overcharged, 14, 7.0, 35),
    Mind(GotrKind.Catalytic, GotrTier.Weak, 2, 5.5, 14),
    Chaos(GotrKind.Catalytic, GotrTier.Medium, 35, 8.5, 74),
    Death(GotrKind.Catalytic, GotrTier.Overcharged, 65, 10.0, 100),
    Blood(GotrKind.Catalytic, GotrTier.Overcharged, 77, 23.8, 100),
    Body(GotrKind.Catalytic, GotrTier.Weak, 20, 7.5, 46),
    Cosmic(GotrKind.Catalytic, GotrTier.Medium, 27, 8.0, 59),
    Nature(GotrKind.Catalytic, GotrTier.Strong, 44, 9.0, 91),
    Law(GotrKind.Catalytic, GotrTier.Strong, 54, 9.5, 100),
    ;

    val symbol: String
        get() = name.lowercase()

    val portalLoc: String
        get() = "loc.gotr_portal_$symbol"

    val runeObj: String
        get() = "obj.${symbol}rune"

    val talismanObj: String
        get() = "obj.${symbol}_talisman"

    val portalTalismanObj: String
        get() = "obj.gotr_portal_talisman_$symbol"

    fun runesPer(level: Int): Int = 1 + level / multiplier

    companion object {
        val elemental = entries.filter { it.kind == GotrKind.Elemental }
        val catalytic = entries.filter { it.kind == GotrKind.Catalytic }
    }
}
