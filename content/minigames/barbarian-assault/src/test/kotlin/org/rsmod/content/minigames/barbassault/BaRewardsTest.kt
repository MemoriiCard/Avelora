package org.rsmod.content.minigames.barbassault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaRewardsTest {
    @Test
    fun `too short or no waves pays nothing`() {
        assertEquals(0, BaRewards.tokens(50, 3, false, 0.0, false))
        assertEquals(0, BaRewards.tokens(500, 0, false, 0.0, false))
        assertFalse(BaRewards.eligible(50, 3))
        assertTrue(BaRewards.eligible(100, 1))
    }

    @Test
    fun `clearing pays more than failing`() {
        assertTrue(BaRewards.tokens(900, 10, true, 0.0, false) > BaRewards.tokens(900, 10, false, 0.0, false))
    }

    @Test
    fun `first clear of the day pays more`() {
        assertTrue(BaRewards.tokens(900, 10, true, 0.0, true) > BaRewards.tokens(900, 10, true, 0.0, false))
    }
}
