package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class PcMapTest {
    private val southWest = CoordGrid(10240, 3200, 0)

    @Test
    fun `source coordinates keep their offsets inside the instance`() {
        val knight = PcMap.toInstance(southWest, PcMap.KNIGHT)
        assertEquals(southWest.x + (PcMap.KNIGHT.x - 2624), knight.x)
        assertEquals(southWest.z + (PcMap.KNIGHT.z - 2560), knight.z)
    }

    @Test
    fun `every arena coordinate lands inside the copied block`() {
        val all = PcMap.PORTALS + PcMap.PEST_SPAWNS + PcMap.START + PcMap.KNIGHT + PcMap.SQUIRE
        for (source in all) assertTrue(PcMap.inInstance(southWest, PcMap.toInstance(southWest, source)), "$source")
    }

    @Test
    fun `the outpost is outside the arena`() {
        assertFalse(PcMap.inArena(PcMap.OUTPOST))
        assertFalse(PcMap.inInstance(southWest, PcMap.OUTPOST))
    }
}
