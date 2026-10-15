package org.rsmod.content.minigames.pestcontrol

enum class PcLander(
    val label: String,
    val minCombat: Int,
    val portalHitpoints: Int,
    val basePoints: Int,
    val gangplank: String,
    val squire: String,
) {
    Novice("Novice", 40, 200, 3, "loc.pest_lander_gangplank", "npc.pest_squire_1"),
    Intermediate("Intermediate", 70, 250, 4, "loc.pest_lander_gangplank_2", "npc.pest_squire_2"),
    Veteran("Veteran", 100, 250, 5, "loc.pest_lander_gangplank_3", "npc.pest_squire_3"),
    ;

    companion object {
        fun ofGangplank(symbol: String): PcLander? = entries.firstOrNull { it.gangplank == symbol }
    }
}
