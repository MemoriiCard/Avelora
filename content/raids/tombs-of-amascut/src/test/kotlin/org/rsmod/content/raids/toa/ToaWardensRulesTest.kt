package org.rsmod.content.raids.toa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.wardens.WardensRules
import org.rsmod.map.CoordGrid

class ToaWardensRulesTest {
    @Test
    fun `core window grows with each ejection`() {
        assertEquals(8, WardensRules.coreWindow(0))
        assertEquals(12, WardensRules.coreWindow(1))
        assertEquals(16, WardensRules.coreWindow(2))
    }

    @Test
    fun `phase two alternates styles and calls lightning every fourth attack`() {
        assertEquals(ToaStyle.Magic, WardensRules.p2Style(0))
        assertEquals(ToaStyle.Ranged, WardensRules.p2Style(1))
        assertFalse(WardensRules.isP2Lightning(0))
        assertTrue(WardensRules.isP2Lightning(4))
    }

    @Test
    fun `phase three cycles all styles and speeds up when enraged`() {
        assertEquals(ToaStyle.Melee, WardensRules.p3Style(0))
        assertEquals(ToaStyle.Magic, WardensRules.p3Style(2))
        assertFalse(WardensRules.isP3Special(4, false))
        assertTrue(WardensRules.isP3Special(5, false))
        assertTrue(WardensRules.isP3Special(4, true))
    }

    @Test
    fun `phase three specials rotate and enrage is lightning only`() {
        assertEquals(WardensRules.Special.Lightning, WardensRules.p3Special(0, false))
        assertEquals(WardensRules.Special.Phantoms, WardensRules.p3Special(1, false))
        assertEquals(WardensRules.Special.Siphon, WardensRules.p3Special(2, false))
        assertEquals(WardensRules.Special.Lightning, WardensRules.p3Special(1, true))
    }

    @Test
    fun `enrage triggers once and heals a fifth`() {
        assertFalse(WardensRules.shouldEnrage(6, false))
        assertTrue(WardensRules.shouldEnrage(5, false))
        assertFalse(WardensRules.shouldEnrage(1, true))
        assertEquals(176, WardensRules.enrageHeal(880))
        assertEquals(26, WardensRules.siphonHeal(880, 3))
    }

    @Test
    fun `strike radius and beam targets`() {
        val centre = CoordGrid(3000, 3000, 2)
        assertTrue(WardensRules.inStrike(CoordGrid(3001, 3001, 2), centre))
        assertFalse(WardensRules.inStrike(CoordGrid(3002, 3000, 2), centre))
        assertFalse(WardensRules.inStrike(CoordGrid(3000, 3000, 1), centre))
        assertEquals(1, WardensRules.beamTargets(1))
        assertEquals(4, WardensRules.beamTargets(8))
    }

    @Test
    fun `wardens rooms use free distinct slots`() {
        val slots = ToaRoom.entries.map { Triple(it.slotColumn, it.slotRow, it.plane) }
        assertEquals(slots.size, slots.toSet().size)
        assertEquals(1, ToaRoom.WardensOne.level)
    }
}
