package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.xarpus.Quadrant
import org.rsmod.content.raids.tob.xarpus.XarpusRules
import org.rsmod.map.CoordGrid

class XarpusRulesTest {
    @Test
    fun `absorbing every remains restores the missing quarter of health`() {
        assertEquals(1250, XarpusRules.healEach(5000) * XarpusRules.REMAINS)
    }

    @Test
    fun `quadrants follow the dominant axis`() {
        val centre = CoordGrid(3170, 4386, 1)
        assertEquals(Quadrant.East, XarpusRules.quadrantOf(centre, CoordGrid(3175, 4387, 1)))
        assertEquals(Quadrant.West, XarpusRules.quadrantOf(centre, CoordGrid(3165, 4385, 1)))
        assertEquals(Quadrant.North, XarpusRules.quadrantOf(centre, CoordGrid(3171, 4391, 1)))
        assertEquals(Quadrant.South, XarpusRules.quadrantOf(centre, CoordGrid(3169, 4380, 1)))
    }

    @Test
    fun `stacks make acid and counters hurt more`() {
        assertTrue(XarpusRules.acidDamage(0, 10) > XarpusRules.acidDamage(0, 0))
        assertTrue(XarpusRules.counterDamage(0, 10) > XarpusRules.counterDamage(0, 0))
        assertEquals(50, XarpusRules.counterDamage(0, 0))
    }

    @Test
    fun `acid splashes a three by three and the body covers five by five`() {
        assertEquals(9, XarpusRules.acidTiles(CoordGrid(3170, 4386, 1)).size)
        val origin = CoordGrid(3168, 4384, 1)
        assertTrue(XarpusRules.under(origin, CoordGrid(3172, 4388, 1)))
        assertFalse(XarpusRules.under(origin, CoordGrid(3173, 4388, 1)))
    }
}
