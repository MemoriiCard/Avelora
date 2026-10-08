package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.verzik.VerzikRules
import org.rsmod.content.raids.tob.verzik.VerzikSpecial
import org.rsmod.map.CoordGrid

class VerzikRulesTest {
    private val pillar = CoordGrid(3167, 4311, 0)

    @Test
    fun `a pillar between Verzik and a player blocks the line`() {
        val verzik = CoordGrid(3168, 4320, 0)
        assertTrue(VerzikRules.blocked(verzik, CoordGrid(3168, 4306, 0), listOf(pillar)))
        assertFalse(VerzikRules.blocked(verzik, CoordGrid(3180, 4306, 0), listOf(pillar)))
        assertFalse(VerzikRules.blocked(verzik, CoordGrid(3168, 4306, 0), emptyList()))
    }

    @Test
    fun `protect from magic halves the first phase attack`() {
        assertEquals(68, VerzikRules.protectedDamage(137, true))
        assertEquals(137, VerzikRules.protectedDamage(137, false))
    }

    @Test
    fun `specials come after every fourth auto in a fixed order`() {
        assertNull(VerzikRules.special(3, 0))
        assertEquals(VerzikSpecial.Nylocas, VerzikRules.special(4, 0))
        assertEquals(VerzikSpecial.Webs, VerzikRules.special(8, 1))
        assertEquals(VerzikSpecial.Ball, VerzikRules.special(12, 2))
        assertEquals(VerzikSpecial.Nylocas, VerzikRules.special(16, 3))
    }

    @Test
    fun `enrage speeds up the third phase`() {
        assertEquals(7, VerzikRules.phase3Rate(50))
        assertEquals(5, VerzikRules.phase3Rate(20))
    }

    @Test
    fun `every pillar sits inside the arena without overlapping another`() {
        for (tile in VerzikRules.PILLARS) {
            assertTrue(tile.x in VerzikRules.ARENA_X && tile.x + 2 in VerzikRules.ARENA_X)
            assertTrue(tile.z in VerzikRules.ARENA_Z && tile.z + 2 in VerzikRules.ARENA_Z)
        }
        for (a in VerzikRules.PILLARS) {
            for (b in VerzikRules.PILLARS) {
                if (a == b) continue
                val overlap = kotlin.math.abs(a.x - b.x) < 3 && kotlin.math.abs(a.z - b.z) < 3
                assertFalse(overlap, "$a overlaps $b")
            }
        }
    }

    @Test
    fun `explosion damage is capped`() {
        assertEquals(63, VerzikRules.explode(1000))
        assertEquals(0, VerzikRules.explode(-5))
    }
}
