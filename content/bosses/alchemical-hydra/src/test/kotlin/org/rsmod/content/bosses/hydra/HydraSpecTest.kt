package org.rsmod.content.bosses.hydra

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class HydraSpecTest {
    @Test
    fun `hydra spec is valid`() {
        val errors = SpecValidator.validate(hydraSpec())
        assertTrue(errors.isEmpty(), "$errors")
    }

    @Test
    fun `the spec covers every phase and transition form`() {
        val types = hydraSpec().npcTypes.toSet()
        for (phase in HydraPhase.entries) {
            assertTrue(phase.npc in types, phase.npc)
            phase.transition?.let { assertTrue(it in types, it) }
        }
    }

    @Test
    fun `each phase is weakened by the wiki vent`() {
        assertEquals(HydraVent.Red, HydraPhase.Serpentine.weakness)
        assertEquals(HydraVent.Green, HydraPhase.Electric.weakness)
        assertEquals(HydraVent.Blue, HydraPhase.Flame.weakness)
        assertNull(HydraPhase.Enraged.weakness)
        assertNull(HydraPhase.Enraged.next)
    }
}
