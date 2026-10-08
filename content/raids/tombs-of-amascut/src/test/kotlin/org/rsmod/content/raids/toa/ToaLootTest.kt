package org.rsmod.content.raids.toa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.party.ToaScaling
import org.rsmod.content.raids.toa.reward.ToaLoot

class ToaLootTest {
    @Test
    fun `entry mode never rolls uniques`() {
        assertEquals(0, ToaLoot.uniquePercent(100))
        assertEquals(5, ToaLoot.uniquePercent(150))
        assertEquals(8, ToaLoot.uniquePercent(300))
        assertEquals(12, ToaLoot.uniquePercent(500))
        assertEquals(14, ToaLoot.uniquePercent(600))
    }

    @Test
    fun `royal unique weights drop at high raid levels`() {
        fun weight(level: Int, obj: String) = ToaLoot.uniques(level).first { it.obj == obj }.weight
        assertEquals(7, weight(300, ToaLoot.FANG))
        assertEquals(6, weight(350, ToaLoot.FANG))
        assertEquals(4, weight(450, ToaLoot.LIGHTBEARER))
        assertEquals(3, weight(600, ToaLoot.FANG))
        assertEquals(2, weight(600, "obj.masori_mask"))
    }

    @Test
    fun `low points mean no unique chance`() {
        assertEquals(0, ToaLoot.uniquePermille(300, 1_000, 10_000, 4))
        assertTrue(ToaLoot.uniquePermille(300, 4_000, 10_000, 4) > 0)
        assertEquals(0, ToaLoot.uniquePermille(100, 9_000, 10_000, 1))
    }

    @Test
    fun `points scale with the raid level`() {
        assertEquals(1_500, ToaLoot.pointsFor(ToaRoom.Zebak, 0))
        assertEquals(3_000, ToaLoot.pointsFor(ToaRoom.Zebak, 300))
        assertEquals(0, ToaLoot.pointsFor(ToaRoom.Vault, 300))
    }

    @Test
    fun `shroud tiers follow completions`() {
        assertNull(ToaLoot.shroudFor(99))
        assertEquals("obj.icthlarins_shroud_1", ToaLoot.shroudFor(100))
        assertEquals("obj.icthlarins_shroud_4", ToaLoot.shroudFor(1_999))
        assertEquals("obj.icthlarins_shroud_5", ToaLoot.shroudFor(2_000))
        assertFalse(ToaLoot.hoodUnlocked(1_999))
        assertTrue(ToaLoot.hoodUnlocked(2_000))
    }

    @Test
    fun `jewels come in order and finish with the amascut jewel`() {
        assertEquals("obj.breach_of_the_scarab", ToaLoot.nextJewel { false })
        assertEquals("obj.eye_of_the_corruptor", ToaLoot.nextJewel { it == "obj.breach_of_the_scarab" })
        assertEquals(ToaLoot.JEWEL_FINAL, ToaLoot.nextJewel { it in ToaLoot.JEWELS })
        assertNull(ToaLoot.nextJewel { true })
    }

    @Test
    fun `rolls give dung below the point floor and three commons otherwise`() {
        val dung = ToaLoot.roll(300, 100, false, { false }) { 0 }
        assertEquals(listOf(ToaLoot.DUNG), dung.map { it.obj })
        val commons = ToaLoot.roll(100, 5_000, false, { false }) { 1 }
        assertEquals(ToaLoot.COMMON_ROLLS, commons.size)
        val unique = ToaLoot.roll(300, 5_000, true, { false }) { 0 }
        assertTrue(unique.first().obj in ToaLoot.UNIQUE_OBJS)
    }

    @Test
    fun `path levels raise scaling after the first path`() {
        assertEquals(0, ToaScaling.pathBonusPercent(0))
        assertEquals(8, ToaScaling.pathBonusPercent(1))
        assertEquals(13, ToaScaling.pathBonusPercent(2))
        assertEquals(23, ToaScaling.pathBonusPercent(4))
        assertEquals(100, ToaScaling.hitpoints(100, 1, 0))
        assertEquals(108, ToaScaling.hitpoints(100, 1, 0, 1))
        assertEquals(113, ToaScaling.damage(100, 0, 2))
    }
}
