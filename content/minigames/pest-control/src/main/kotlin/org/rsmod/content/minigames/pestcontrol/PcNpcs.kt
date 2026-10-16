package org.rsmod.content.minigames.pestcontrol

internal object PcNpcs {
    const val TIERS = 5
    val PORTAL_NAMES = listOf("purple", "blue", "yellow", "red")

    fun portal(index: Int): String = "npc.pest_portal_${index + 1}_active"

    fun knight(): String = "npc.pest_voidknight_1"

    fun tier(averageCombat: Int): Int = (averageCombat / 20 + 1).coerceIn(1, TIERS)

    fun pest(tier: Int, index: Int): String = PcPest.entries[index % PcPest.entries.size].symbol(tier)

    val PESTS: List<String> =
        PcPest.entries.flatMap { kind -> (1..TIERS).flatMap { tier -> variants(kind, tier) } }

    val ALL: List<String> =
        (0 until PcMatch.PORTALS).map { portal(it) } + knight() + PcLander.entries.map { it.squire } + PESTS

    private fun variants(kind: PcPest, tier: Int): List<String> {
        val base = kind.symbol(tier)
        return if (kind.variants) listOf(base, base.dropLast(1) + "b") else listOf(base)
    }
}
