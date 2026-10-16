package org.rsmod.content.minigames.soulwars

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SoulWarsShieldTest {
    @Test
    fun `avatars start fully shielded`() {
        val shield = AvatarShield()
        assertFalse(shield.vulnerable(SwTeam.Blue))
        assertFalse(shield.vulnerable(SwTeam.Red))
        assertEquals(100.0, shield.reduction(SwTeam.Blue))
    }

    @Test
    fun `fragments lower the enemy avatar and the controller gets full value`() {
        val shield = AvatarShield()
        shield.sacrifice(SwTeam.Blue, 10, controller = SwTeam.Blue)
        assertEquals(90.0, shield.reduction(SwTeam.Red), 1e-9)
        assertTrue(shield.vulnerable(SwTeam.Red))
        assertFalse(shield.vulnerable(SwTeam.Blue))
    }

    @Test
    fun `fragments from the team that does not hold the obelisk count a third`() {
        val shield = AvatarShield()
        shield.sacrifice(SwTeam.Blue, 30, controller = SwTeam.Red)
        assertEquals(90.0, shield.reduction(SwTeam.Red), 1e-9)
    }

    @Test
    fun `a contested obelisk gives full value to both`() {
        val shield = AvatarShield()
        shield.sacrifice(SwTeam.Red, 5, controller = null)
        assertEquals(95.0, shield.reduction(SwTeam.Blue), 1e-9)
    }

    @Test
    fun `bones raise reduction but never above full and reduction never goes negative`() {
        val shield = AvatarShield()
        shield.bury(SwTeam.Red, 50)
        assertEquals(100.0, shield.reduction(SwTeam.Red))
        shield.sacrifice(SwTeam.Blue, 500, controller = SwTeam.Blue)
        assertEquals(0.0, shield.reduction(SwTeam.Red))
        shield.bury(SwTeam.Red, 4)
        assertEquals(4.0, shield.reduction(SwTeam.Red))
        shield.reset()
        assertEquals(100.0, shield.reduction(SwTeam.Red))
    }

    @Test
    fun `the team with more players near the obelisk controls it`() {
        assertEquals(SwTeam.Blue, ObeliskControl.controller(mapOf(SwTeam.Blue to 3, SwTeam.Red to 2)))
        assertEquals(SwTeam.Red, ObeliskControl.controller(mapOf(SwTeam.Red to 1)))
        assertNull(ObeliskControl.controller(mapOf(SwTeam.Blue to 2, SwTeam.Red to 2)))
        assertNull(ObeliskControl.controller(emptyMap()))
    }
}
