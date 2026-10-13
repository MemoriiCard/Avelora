package org.rsmod.content.minigames.foundry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FoundryRulesTest {
    private fun counts(vararg pairs: Pair<Metal, Int>) =
        IntArray(Metal.entries.size).also { c -> pairs.forEach { c[it.first.ordinal] = it.second } }

    @Test
    fun `pure rune crucible scores 60`() {
        assertEquals(60, FoundryRules.metalScore(counts(Metal.Rune to 28)))
    }

    @Test
    fun `half rune half adamant reaches the 130 cap`() {
        assertEquals(130, FoundryRules.metalScore(counts(Metal.Rune to 14, Metal.Adamant to 14)))
    }

    @Test
    fun `sections follow difficulty tiers`() {
        assertEquals(3, FoundryRules.sections(15))
        assertEquals(4, FoundryRules.sections(20))
        assertEquals(5, FoundryRules.sections(60))
        assertEquals(6, FoundryRules.sections(90))
        assertEquals(7, FoundryRules.sections(120))
    }

    @Test
    fun `temperature bands shrink to half size at 130`() {
        assertEquals(33.0, FoundryRules.bandWidth(0), 0.001)
        assertEquals(16.5, FoundryRules.bandWidth(130), 0.001)
        assertEquals(Heat.Hot, FoundryRules.heatOf(100.0, 130))
        assertEquals(Heat.Cold, FoundryRules.heatOf(0.0, 130))
        assertEquals(Heat.Medium, FoundryRules.heatOf(50.0, 130))
        assertEquals(null, FoundryRules.heatOf(75.0, 130))
    }

    @Test
    fun `hand in rewards match the documented formula`() {
        assertEquals(30, FoundryRules.handInXp(0))
        assertEquals(60, FoundryRules.handInCoins(0))
        assertEquals((136 + 150 + 1) * 30, FoundryRules.handInXp(100))
    }

    @Test
    fun `mould tables hold three default and five purchasable moulds per type`() {
        for (type in MouldType.entries) {
            val all = FoundryRules.moulds(type)
            assertEquals(6, all.count { !it.purchasable })
            assertEquals(5, all.count { it.purchasable })
            assertEquals((1..11).toList(), all.map { it.id })
        }
    }

    @Test
    fun `mould score adds the two commission traits`() {
        val medusa = FoundryRules.mould(MouldType.Forte, 4)!!
        assertEquals(8 + 8, medusa.score(CommissionWord.Broad, CommissionStyle.Flat))
        assertEquals(6 + 0, medusa.score(CommissionWord.Heavy, CommissionStyle.Spiked))
    }

    @Test
    fun `best optimal default set averages near the wiki figure`() {
        var best = 0
        for (word in CommissionWord.entries) for (style in CommissionStyle.entries) {
            val score =
                MouldType.entries.sumOf { type ->
                    FoundryRules.moulds(type).filter { !it.purchasable }.maxOf { it.score(word, style) }
                }
            best = maxOf(best, score)
        }
        assertTrue(best in 30..48, "best=$best")
    }

    @Test
    fun `tools demand their documented temperatures`() {
        assertEquals(Heat.Hot, FoundryRules.requiredHeat(Tool.TripHammer))
        assertEquals(Heat.Medium, FoundryRules.requiredHeat(Tool.Grindstone))
        assertEquals(Heat.Cold, FoundryRules.requiredHeat(Tool.PolishingWheel))
    }
}
