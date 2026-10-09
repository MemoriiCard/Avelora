package org.rsmod.content.minigames.pestcontrol

internal object PcNpcs {
    private val KINDS = listOf("brawler", "ravager", "spinner", "splatter")
    const val TIERS = 5

    fun portal(index: Int): String = "npc.pest_portal_${index + 1}_active"

    fun knight(): String = "npc.pest_voidknight_1"

    fun tier(averageCombat: Int): Int = (averageCombat / 20 + 1).coerceIn(1, TIERS)

    fun pest(tier: Int, index: Int): String = "npc.pest_${KINDS[index % KINDS.size]}_$tier"

    val ALL: List<String> =
        (0 until PcMatch.PORTALS).map { portal(it) } +
            knight() +
            KINDS.flatMap { kind -> (1..TIERS).map { "npc.pest_${kind}_$it" } }
}
