package org.rsmod.content.minigames.lms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LmsCompetitiveTest {
    @Test
    fun `placement points follow the wiki table and need a kill`() {
        assertEquals(0, LmsCompetitive.points(1, 0))
        assertEquals(6, LmsCompetitive.points(1, 1))
        assertEquals(5, LmsCompetitive.points(2, 2))
        assertEquals(4, LmsCompetitive.points(4, 1))
        assertEquals(3, LmsCompetitive.points(9, 1))
        assertEquals(1, LmsCompetitive.points(19, 1))
        assertEquals(0, LmsCompetitive.points(20, 1))
    }

    @Test
    fun `kill bonuses do not stack and points cap at nine`() {
        assertEquals(1 + 2, LmsCompetitive.points(15, 3))
        assertEquals(1 + 3, LmsCompetitive.points(15, 5))
        assertEquals(1 + 3, LmsCompetitive.points(15, 12))
        assertEquals(9, LmsCompetitive.points(1, 5))
    }

    @Test
    fun `rating moves with placement`() {
        assertEquals(9, LmsCompetitive.ratingChange(1))
        assertEquals(1, LmsCompetitive.ratingChange(9))
        assertEquals(0, LmsCompetitive.ratingChange(10))
        assertEquals(0, LmsCompetitive.ratingChange(16))
        assertEquals(-1, LmsCompetitive.ratingChange(17))
        assertEquals(-8, LmsCompetitive.ratingChange(24))
    }

    @Test
    fun `titles follow rating bands`() {
        assertEquals("Novice", LmsCompetitive.title(599))
        assertEquals("Veteran", LmsCompetitive.title(600))
        assertEquals("Master", LmsCompetitive.title(750))
        assertEquals("Legend", LmsCompetitive.title(1000))
    }

    @Test
    fun `competitive games need twenty four players and a lobby holds one mode`() {
        val match = LmsMatch(waitTicks = 10)
        assertTrue(match.join(1, 0, LmsMode.Competitive))
        assertFalse(match.join(2, 0, LmsMode.Casual))
        assertTrue(match.join(3, 0, LmsMode.Competitive))
        assertInstanceOf(LmsEvent.WaitExtended::class.java, match.update(10).single())
        for (id in 4..25) match.join(id, 11, LmsMode.Competitive)
        assertInstanceOf(LmsEvent.GameStarted::class.java, match.update(21).single())
        assertEquals(LmsMode.Competitive, match.gameMode)
    }

    @Test
    fun `shop gates tradeable items behind a win`() {
        val tradeable = LmsShop.ALL.first { it.needsWin }
        val untradeable = LmsShop.ALL.first { !it.needsWin }
        assertFalse(LmsShop.canBuy(tradeable, 0))
        assertTrue(LmsShop.canBuy(tradeable, 1))
        assertTrue(LmsShop.canBuy(untradeable, 0))
        assertEquals(450, LmsShop.COSMETICS.single { it.obj == "obj.zaros_halo" }.cost)
        assertEquals(160, LmsShop.COSMETICS.single { it.obj == "obj.deadman_cape" }.cost)
    }
}
