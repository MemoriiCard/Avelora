package org.rsmod.content.minigames.blastfurnace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlastFurnaceRulesTest {
    private fun smelt(ores: Map<Ore, Int>, coal: Int): Bar? =
        Bar.smeltOrder.firstOrNull { BlastFurnaceRules.canSmelt(it, { ore -> ores[ore] ?: 0 }, coal) }

    @Test
    fun `iron ore makes steel only when coal is present`() {
        assertEquals(Bar.Steel, smelt(mapOf(Ore.Iron to 1), coal = 1))
        assertEquals(Bar.Iron, smelt(mapOf(Ore.Iron to 1), coal = 0))
    }

    @Test
    fun `bronze needs both copper and tin`() {
        assertEquals(Bar.Bronze, smelt(mapOf(Ore.Copper to 1, Ore.Tin to 1), 0))
        assertEquals(null, smelt(mapOf(Ore.Copper to 3), 0))
    }

    @Test
    fun `high tier bars need their coal`() {
        assertEquals(null, smelt(mapOf(Ore.Mithril to 1), coal = 1))
        assertEquals(Bar.Mithril, smelt(mapOf(Ore.Mithril to 1), coal = 2))
        assertEquals(null, smelt(mapOf(Ore.Adamant to 1), coal = 2))
        assertEquals(Bar.Adamant, smelt(mapOf(Ore.Adamant to 1), coal = 3))
        assertEquals(Bar.Runite, smelt(mapOf(Ore.Runite to 1), coal = 4))
    }

    @Test
    fun `levels and xp match the wiki table`() {
        val expected =
            mapOf(
                Bar.Bronze to (1 to 6.2),
                Bar.Iron to (15 to 12.5),
                Bar.Silver to (20 to 13.7),
                Bar.Steel to (30 to 17.5),
                Bar.Gold to (40 to 22.5),
                Bar.Mithril to (50 to 30.0),
                Bar.Adamant to (70 to 37.5),
                Bar.Runite to (85 to 50.0),
            )
        for ((bar, pair) in expected) {
            assertEquals(pair.first, bar.level, bar.name)
            assertEquals(pair.second, bar.xp, bar.name)
        }
        assertEquals(56.2, Bar.Gold.goldsmithXp)
    }

    @Test
    fun `gauge bands`() {
        assertEquals("low", BlastFurnaceRules.gauge(0))
        assertEquals("medium", BlastFurnaceRules.gauge(50))
        assertEquals("high", BlastFurnaceRules.gauge(90))
    }

    @Test
    fun `capacities`() {
        assertTrue(BlastFurnaceRules.MAX_COAL == 254 && BlastFurnaceRules.MAX_PRIMARY_ORES == 28)
        assertFalse(Bar.smeltOrder.isEmpty())
        assertEquals(Bar.entries.toSet(), Bar.smeltOrder.toSet())
    }
}
