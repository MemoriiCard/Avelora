package org.rsmod.content.bosses.zulrah

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZulrahRotationsTest {
    @Test
    fun `rotations match the wiki phase counts`() {
        assertEquals(listOf(10, 10, 11, 12), ZulrahRotations.ROTATIONS.map { it.size })
    }

    @Test
    fun `every rotation ends on the middle green phase`() {
        for (rotation in ZulrahRotations.ROTATIONS) {
            val last = rotation.last()
            assertEquals(ZulrahForm.Serpentine, last.form)
            assertEquals(ZulrahPosition.Middle, last.position)
        }
    }

    @Test
    fun `melee only happens in the magma form`() {
        for (phase in ZulrahRotations.ROTATIONS.flatten()) {
            if (ZulrahAction.Melee in phase.actions) assertEquals(ZulrahForm.Magma, phase.form, "$phase")
        }
    }

    @Test
    fun `every phase does something before diving`() {
        for (phase in ZulrahRotations.ROTATIONS.flatten() + ZulrahRotations.OPENING) {
            assertTrue(phase.actions.isNotEmpty(), "$phase")
        }
    }

    @Test
    fun `the opening barrage has a cloud spot for every orb`() {
        val orbs = ZulrahRotations.OPENING.actions.count { it == ZulrahAction.Clouds } * 2
        assertEquals(ZulrahShrine.OPENING_CLOUDS.size, orbs)
    }
}
