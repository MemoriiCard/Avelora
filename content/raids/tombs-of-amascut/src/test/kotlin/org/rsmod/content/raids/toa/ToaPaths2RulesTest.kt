package org.rsmod.content.raids.toa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.toa.akkha.AkkhaRules
import org.rsmod.content.raids.toa.baba.BabaRules
import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.content.raids.toa.layout.ToaPath
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.puzzle.ApmekenPuzzleRules
import org.rsmod.content.raids.toa.puzzle.HetPuzzleRules
import org.rsmod.map.CoordGrid

class ToaPaths2RulesTest {
    @Test
    fun `akkha cycles styles and uses a special every seventh attack`() {
        assertEquals(ToaStyle.Melee, AkkhaRules.style(0))
        assertEquals(ToaStyle.Ranged, AkkhaRules.style(1))
        assertEquals(ToaStyle.Magic, AkkhaRules.style(2))
        assertEquals(ToaStyle.Melee, AkkhaRules.style(3))
        assertFalse(AkkhaRules.isSpecialTurn(0))
        assertFalse(AkkhaRules.isSpecialTurn(6))
        assertTrue(AkkhaRules.isSpecialTurn(7))
        assertTrue(AkkhaRules.isSpecialTurn(14))
    }

    @Test
    fun `akkha solo never rolls the detonate special`() {
        val solo = (0..5).map { AkkhaRules.special(it, 1) }
        assertFalse(AkkhaRules.Special.Detonate in solo)
        val team = (0..5).map { AkkhaRules.special(it, 3) }
        assertTrue(AkkhaRules.Special.Detonate in team)
    }

    @Test
    fun `akkha detonate needs a shared line within reach`() {
        val a = CoordGrid(3680, 5400, 0)
        assertTrue(AkkhaRules.sharesLine(a, CoordGrid(3680, 5403, 0)))
        assertFalse(AkkhaRules.sharesLine(a, CoordGrid(3680, 5409, 0)))
        assertFalse(AkkhaRules.sharesLine(a, CoordGrid(3681, 5401, 0)))
        assertFalse(AkkhaRules.sharesLine(a, a))
    }

    @Test
    fun `akkha enrages once at twenty percent`() {
        assertFalse(AkkhaRules.shouldEnrage(21, false))
        assertTrue(AkkhaRules.shouldEnrage(20, false))
        assertFalse(AkkhaRules.shouldEnrage(10, true))
        assertEquals(0, AkkhaRules.quadrant(1, 1, 5, 5))
        assertEquals(3, AkkhaRules.quadrant(9, 9, 5, 5))
    }

    @Test
    fun `baba alternates styles and schedules slams and rockfalls`() {
        assertEquals(ToaStyle.Melee, BabaRules.style(0))
        assertEquals(ToaStyle.Ranged, BabaRules.style(1))
        assertFalse(BabaRules.isSlam(0))
        assertTrue(BabaRules.isSlam(4))
        assertTrue(BabaRules.isRockfall(5))
        assertFalse(BabaRules.isRockfall(0))
    }

    @Test
    fun `baba phases fire in order and only once`() {
        assertNull(BabaRules.nextPhase(80, 0))
        assertEquals(0, BabaRules.nextPhase(66, 0))
        assertEquals(1, BabaRules.nextPhase(30, 1))
        assertNull(BabaRules.nextPhase(30, 2))
    }

    @Test
    fun `baba slam cross and damage splitting`() {
        val centre = CoordGrid(3806, 5406, 0)
        assertTrue(BabaRules.inCross(CoordGrid(3808, 5406, 0), centre, 2))
        assertFalse(BabaRules.inCross(CoordGrid(3809, 5406, 0), centre, 2))
        assertTrue(BabaRules.inCross(CoordGrid(3809, 5406, 0), centre, BabaRules.slamArm(true)))
        assertFalse(BabaRules.inCross(CoordGrid(3808, 5408, 0), centre, 2))
        assertEquals(30, BabaRules.splitDamage(30, 0))
        assertEquals(10, BabaRules.splitDamage(30, 2))
        assertEquals(1, BabaRules.baboonCount(1))
        assertEquals(4, BabaRules.baboonCount(8))
    }

    @Test
    fun `puzzle completion rules`() {
        assertFalse(HetPuzzleRules.destroyed(2))
        assertTrue(HetPuzzleRules.destroyed(3))
        assertFalse(ApmekenPuzzleRules.complete(setOf(0, 1, 2)))
        assertTrue(ApmekenPuzzleRules.complete(setOf(0, 1, 2, 3)))
    }

    @Test
    fun `new paths map their rooms and room slots stay unique`() {
        assertEquals(ToaPath.Het, ToaPath.forRoom(ToaRoom.HetPuzzle))
        assertEquals(ToaPath.Het, ToaPath.forRoom(ToaRoom.Akkha))
        assertEquals(ToaPath.Apmeken, ToaPath.forRoom(ToaRoom.ApmekenPuzzle))
        assertEquals(ToaPath.Apmeken, ToaPath.forRoom(ToaRoom.Baba))
        assertEquals(ToaRoom.entries.size, ToaRoom.entries.map { Triple(it.slotColumn, it.slotRow, it.plane) }.toSet().size)
    }
}
