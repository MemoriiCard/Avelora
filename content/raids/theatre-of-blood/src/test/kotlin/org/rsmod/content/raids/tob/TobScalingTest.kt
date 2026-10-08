package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling

class TobScalingTest {
    @Test
    fun `team size sets the boss health percentage`() {
        assertEquals(75, TobScaling.teamHpPercent(1))
        assertEquals(75, TobScaling.teamHpPercent(3))
        assertEquals(88, TobScaling.teamHpPercent(4))
        assertEquals(100, TobScaling.teamHpPercent(5))
    }

    @Test
    fun `mode multiplies on top of team scaling`() {
        assertEquals(750, TobScaling.hitpoints(1000, 3, TobMode.Normal))
        assertEquals(1000, TobScaling.hitpoints(1000, 5, TobMode.Normal))
        assertTrue(TobScaling.hitpoints(1000, 5, TobMode.Hard) > 1000)
        assertTrue(TobScaling.hitpoints(1000, 5, TobMode.Entry) < 1000)
    }

    @Test
    fun `only regular and hard modes award uniques`() {
        assertEquals(false, TobMode.Entry.awardsUniques)
        assertEquals(true, TobMode.Normal.awardsUniques)
        assertEquals(true, TobMode.Hard.awardsUniques)
    }
}
