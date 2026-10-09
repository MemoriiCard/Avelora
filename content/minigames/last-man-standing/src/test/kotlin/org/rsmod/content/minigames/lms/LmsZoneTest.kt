package org.rsmod.content.minigames.lms

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LmsZoneTest {
    @Test
    fun `radius shrinks in stages`() {
        assertEquals(250, LmsZone.radius(0))
        assertEquals(150, LmsZone.radius(300))
        assertEquals(80, LmsZone.radius(600))
        assertEquals(40, LmsZone.radius(1000))
    }

    @Test
    fun `centre is safe and far tiles are outside`() {
        assertFalse(LmsZone.isOutside(LmsZone.CENTER_X, LmsZone.CENTER_Z, 1000))
        assertTrue(LmsZone.isOutside(LmsZone.CENTER_X + 100, LmsZone.CENTER_Z, 1000))
        assertFalse(LmsZone.isOutside(LmsZone.CENTER_X + 100, LmsZone.CENTER_Z, 0))
    }

    @Test
    fun `damage grows with stage`() {
        assertEquals(1, LmsZone.damage(0))
        assertEquals(3, LmsZone.damage(300))
        assertEquals(7, LmsZone.damage(750))
    }

    @Test
    fun `loot roll always returns a table entry`() {
        val random = Random(7)
        repeat(500) { assertTrue(LmsLoot.roll { random.nextInt(it) } in LmsLoot.TABLE) }
    }

    @Test
    fun `rewards pay kill points on top and respect minimum participation`() {
        assertEquals(LmsReward.NONE, LmsRewards.reward(50, 3, true, 0.0, true))
        assertEquals(LmsReward(9 + 6, 3), LmsRewards.reward(500, 2, true, 0.0, false))
        assertEquals(LmsReward(5, 2), LmsRewards.reward(500, 0, false, 0.0, false))
    }
}
