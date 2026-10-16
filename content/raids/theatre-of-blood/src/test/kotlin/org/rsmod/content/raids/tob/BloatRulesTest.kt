package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.bloat.BloatRules
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.map.CoordGrid

class BloatRulesTest {
    @Test
    fun `walk and stop durations stay in the wiki ranges`() {
        assertEquals(39, BloatRules.walkTicks(0))
        assertEquals(47, BloatRules.walkTicks(8))
        assertEquals(34, BloatRules.stopTicks(0))
        assertEquals(42, BloatRules.stopTicks(8))
    }

    @Test
    fun `moving halves damage taken`() {
        assertEquals(50, BloatRules.incomingPercent(true))
        assertEquals(100, BloatRules.incomingPercent(false))
    }

    @Test
    fun `falling flesh only starts at 90 percent in regular mode but is constant in hard mode`() {
        assertFalse(BloatRules.fleshActive(95, true, TobMode.Normal))
        assertTrue(BloatRules.fleshActive(90, true, TobMode.Normal))
        assertFalse(BloatRules.fleshActive(50, false, TobMode.Normal))
        assertTrue(BloatRules.fleshActive(100, false, TobMode.Hard))
    }

    @Test
    fun `stomp and flesh damage stay in range`() {
        assertEquals(40, BloatRules.stomp(0, TobMode.Normal))
        assertEquals(80, BloatRules.stomp(99, TobMode.Normal))
        assertEquals(30, BloatRules.flesh(0, TobMode.Normal))
        assertEquals(50, BloatRules.flesh(99, TobMode.Normal))
    }

    @Test
    fun `patrol ring keeps the five by five body inside the arena and off the centre block`() {
        for (corner in BloatRules.RING) {
            assertTrue(corner.x in BloatRules.ARENA_X && corner.x + 4 in BloatRules.ARENA_X)
            val overlapsCentre = corner.x + 4 >= 3293 && corner.x <= 3298 && corner.z + 4 >= 4445 && corner.z <= 4450
            assertFalse(overlapsCentre, "corner $corner")
        }
        assertEquals(0, BloatRules.nextCorner(BloatRules.RING.size - 1))
    }

    @Test
    fun `body coverage is a five by five footprint`() {
        val origin = CoordGrid(3293, 4445, 0)
        assertTrue(BloatRules.covers(origin, CoordGrid(3297, 4449, 0)))
        assertFalse(BloatRules.covers(origin, CoordGrid(3298, 4449, 0)))
    }
}
