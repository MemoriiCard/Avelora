package org.rsmod.content.minigames.barbassault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaGambleTest {
    @Test
    fun `every tier has thirty two entries`() {
        for (tier in GambleTier.entries) assertEquals(32, BaGamble.table(tier).size, tier.name)
    }

    @Test
    fun `quantity ranges are valid and coins match the wiki`() {
        for (tier in GambleTier.entries) {
            assertTrue(BaGamble.table(tier).all { it.min in 1..it.max })
        }
        val coins = BaGamble.table(GambleTier.High).single { it.obj == "obj.coins" }
        assertEquals(50_000, coins.min)
        assertEquals(75_000, coins.max)
    }

    @Test
    fun `requirements gate each tier`() {
        assertNotNull(BaGamble.available(GambleTier.Low, 199, 5, 0))
        assertNotNull(BaGamble.available(GambleTier.Low, 200, 2, 0))
        assertNull(BaGamble.available(GambleTier.Low, 200, 3, 0))
        assertNotNull(BaGamble.available(GambleTier.Medium, 400, 5, 0))
        assertNull(BaGamble.available(GambleTier.Medium, 400, 6, 0))
        assertNotNull(BaGamble.available(GambleTier.High, 500, 9, 0))
        assertNull(BaGamble.available(GambleTier.High, 500, 0, 1))
    }

    @Test
    fun `a roll picks one entry and a quantity inside its range`() {
        repeat(300) { seed ->
            var step = seed
            val result = BaGamble.roll(GambleTier.Low) { bound -> (step++ * 13 + 5) % bound }
            assertFalse(result.rare)
            assertTrue(result.quantity in result.item.min..result.item.max)
        }
    }

    @Test
    fun `the rare roll only exists on the high tier`() {
        assertTrue(BaGamble.roll(GambleTier.High) { 0 }.rare)
        assertFalse(BaGamble.roll(GambleTier.Medium) { 0 }.rare)
        assertFalse(BaGamble.roll(GambleTier.High) { bound -> bound - 1 }.rare)
    }
}
