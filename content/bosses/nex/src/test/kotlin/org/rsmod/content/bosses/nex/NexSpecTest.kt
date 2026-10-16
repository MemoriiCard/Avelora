package org.rsmod.content.bosses.nex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class NexSpecTest {
    @Test
    fun `specs are valid`() {
        assertTrue(SpecValidator.validate(nexSpec()).isEmpty(), "${SpecValidator.validate(nexSpec())}")
        for (mage in NexMage.entries) {
            val errors = SpecValidator.validate(mageSpec(mage))
            assertTrue(errors.isEmpty(), "${mage.name}: $errors")
        }
    }

    @Test
    fun `every phase has a spec phase and only zaros transforms`() {
        val phases = nexSpec().phases
        for (phase in NexPhase.entries) assertTrue(phase.key in phases, phase.key)
        assertEquals(NEX_SOULSPLIT, phases.getValue(PHASE_ZAROS).transmog)
        assertEquals(null, phases.getValue(PHASE_SMOKE).transmog)
    }

    @Test
    fun `mages guard Nex at every fifth of her health in order`() {
        assertEquals(
            listOf(NexMage.Fumus, NexMage.Umbra, NexMage.Cruor, NexMage.Glacies, null),
            NexPhase.entries.map { it.mage },
        )
        assertEquals(listOf(2720, 2040, 1360, 680, 0), NexPhase.entries.map { it.mageThreshold(3400) })
        assertEquals(null, NexPhase.Zaros.next)
    }

    @Test
    fun `dash paths stay inside the arena`() {
        for (dash in NexArena.DASH_ENDS) {
            assertTrue(dash.path(3).all { NexArena.contains(it) }, "$dash")
        }
    }
}
