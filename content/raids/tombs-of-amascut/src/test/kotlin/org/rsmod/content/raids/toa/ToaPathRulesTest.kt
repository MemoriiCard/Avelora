package org.rsmod.content.raids.toa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.toa.boss.ToaCombat
import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.kephri.KephriRules
import org.rsmod.content.raids.toa.layout.ToaPath
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.puzzle.CrondisPuzzleRules
import org.rsmod.content.raids.toa.puzzle.ScabarasPuzzleRules
import org.rsmod.content.raids.toa.zebak.ZebakRules
import org.rsmod.map.CoordGrid

class ToaPathRulesTest {
    @Test
    fun `prayer blocks fully unless quiet prayers is on`() {
        assertEquals(30, ToaCombat.afterPrayer(30, false, emptySet()))
        assertEquals(0, ToaCombat.afterPrayer(30, true, emptySet()))
        assertEquals(3, ToaCombat.afterPrayer(30, true, setOf(ToaInvocation.QuietPrayers)))
    }

    @Test
    fun `deadly prayers drains a fifth of damage`() {
        assertEquals(10, ToaCombat.prayerDrain(50, setOf(ToaInvocation.DeadlyPrayers)))
        assertEquals(0, ToaCombat.prayerDrain(50, emptySet()))
    }

    @Test
    fun `zebak specials fire once each at their thresholds and alternate`() {
        assertNull(ZebakRules.nextSpecial(90, 0))
        assertEquals(0, ZebakRules.nextSpecial(85, 0))
        assertEquals(1, ZebakRules.nextSpecial(60, 1))
        assertNull(ZebakRules.nextSpecial(40, 4))
        assertTrue(ZebakRules.isRoar(0))
        assertFalse(ZebakRules.isRoar(1))
    }

    @Test
    fun `zebak enrages and picks styles by range`() {
        assertEquals(7, ZebakRules.attackRate(100))
        assertEquals(4, ZebakRules.attackRate(25))
        assertEquals(ToaStyle.Melee, ZebakRules.style(0, true))
        assertEquals(ToaStyle.Ranged, ZebakRules.style(0, false))
        assertEquals(ToaStyle.Magic, ZebakRules.style(1, false))
        assertEquals(38, ZebakRules.maxHit(ToaStyle.Melee))
    }

    @Test
    fun `upset stomach shrinks the safe radius and waves leave a gap`() {
        assertEquals(2, ZebakRules.safeRadius(false))
        assertEquals(1, ZebakRules.safeRadius(true))
        assertTrue(ZebakRules.inGap(5400, 5401))
        assertFalse(ZebakRules.inGap(5400, 5404))
    }

    @Test
    fun `kephri shields at the start and again at two thirds`() {
        assertEquals(0, KephriRules.nextShield(100, 0))
        assertNull(KephriRules.nextShield(90, 1))
        assertEquals(1, KephriRules.nextShield(66, 1))
        assertNull(KephriRules.nextShield(10, 2))
    }

    @Test
    fun `kephri scales scarabs and eggs`() {
        assertEquals(6, KephriRules.shieldScarabs(1))
        assertEquals(20, KephriRules.shieldScarabs(8))
        assertEquals(2, KephriRules.eggCount(false))
        assertEquals(4, KephriRules.eggCount(true))
        assertEquals(4, KephriRules.attackRate(33))
    }

    @Test
    fun `puzzle rules`() {
        assertEquals(6, ScabarasPuzzleRules.waveSize(1, 1))
        assertEquals(15, ScabarasPuzzleRules.waveSize(3, 6))
        assertFalse(CrondisPuzzleRules.complete(setOf(0, 1, 2)))
        assertTrue(CrondisPuzzleRules.complete(setOf(0, 1, 2, 3)))
        assertEquals("npc.toa_crondis_tree_1", CrondisPuzzleRules.treeName(0))
    }

    @Test
    fun `every path room has its own slot and the paths map back`() {
        val slots = ToaRoom.entries.map { Triple(it.slotColumn, it.slotRow, it.plane) }
        assertEquals(slots.size, slots.toSet().size)
        assertEquals(ToaPath.Crondis, ToaPath.forRoom(ToaRoom.Zebak))
        assertEquals(ToaPath.Scabaras, ToaPath.forRoom(ToaRoom.ScabarasPuzzle))
        assertNull(ToaPath.forRoom(ToaRoom.Nexus))
    }

    @Test
    fun `room coordinates round trip through the instance`() {
        val base = CoordGrid(6400, 6400, 0)
        for (room in ToaRoom.entries) {
            val instance = room.toInstance(base, room.arrival)
            assertTrue(room.owns(base, instance))
            assertEquals(room.arrival.x, room.toSource(base, instance).x)
            assertEquals(room.arrival.z, room.toSource(base, instance).z)
        }
    }
}
