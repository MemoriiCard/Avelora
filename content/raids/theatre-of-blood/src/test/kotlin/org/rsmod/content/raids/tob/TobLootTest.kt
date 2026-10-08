package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.reward.TobLoot

class TobLootTest {
    private fun fixed(vararg values: Int): (Int) -> Int {
        var i = 0
        return { bound -> values[i++ % values.size].coerceIn(0, bound - 1) }
    }

    @Test
    fun `entry mode never awards uniques`() {
        assertEquals(0, TobLoot.uniquePercent(TobMode.Entry))
        val loot = TobLoot.roll(TobMode.Entry) { 0 }
        assertTrue(loot.none { entry -> TobLoot.UNIQUES.any { it.obj == entry.obj } })
        assertEquals(TobLoot.COMMON_ROLLS, loot.size)
    }

    @Test
    fun `no unique gives three distinct neighbours`() {
        val loot = TobLoot.roll(TobMode.Normal, fixed(99, 0, 0, 0, 0, 0, 0, 0, 0, 1))
        val commons = loot.take(TobLoot.COMMON_ROLLS)
        assertEquals(TobLoot.COMMON_ROLLS, commons.size)
        assertTrue(commons.zipWithNext().all { (a, b) -> a.obj != b.obj })
    }

    @Test
    fun `low roll awards a unique and one common`() {
        val loot = TobLoot.roll(TobMode.Normal) { 0 }
        assertEquals("obj.infernal_defender_hilt", loot.first().obj)
        assertEquals(2, loot.take(2).size)
        assertTrue(loot.count { entry -> TobLoot.UNIQUES.any { it.obj == entry.obj } } == 1)
    }

    @Test
    fun `hard mode raises unique chance and common amounts`() {
        assertTrue(TobLoot.uniquePercent(TobMode.Hard) > TobLoot.uniquePercent(TobMode.Normal))
        val normal = TobLoot.roll(TobMode.Normal) { 99.coerceAtMost(it - 1) }
        val hard = TobLoot.roll(TobMode.Hard) { 99.coerceAtMost(it - 1) }
        assertTrue(hard.first().count > normal.first().count)
    }

    @Test
    fun `unique weights pick every entry`() {
        val total = TobLoot.UNIQUES.sumOf { it.weight }
        val seen = (0 until total).map { roll -> TobLoot.pickUnique { roll }.obj }.toSet()
        assertEquals(TobLoot.UNIQUES.map { it.obj }.toSet(), seen)
    }

    @Test
    fun `shrouds unlock at completion tiers`() {
        assertNull(TobLoot.shroudFor(99))
        assertEquals("obj.sinhaza_shroud_tier1", TobLoot.shroudFor(100))
        assertEquals("obj.sinhaza_shroud_tier5", TobLoot.shroudFor(5000))
        assertEquals(500, TobLoot.nextShroud(100))
        assertNull(TobLoot.nextShroud(2000))
        assertNotNull(TobLoot.SHROUDS.firstOrNull())
    }
}
