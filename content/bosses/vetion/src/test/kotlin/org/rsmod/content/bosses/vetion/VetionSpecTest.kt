package org.rsmod.content.bosses.vetion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class VetionSpecTest {
    @Test
    fun `specs are valid`() {
        for (lair in VETION_LAIRS) {
            val errors = SpecValidator.validate(vetionSpec(lair))
            assertTrue(errors.isEmpty(), "${lair.key}: $errors")
        }
    }

    @Test
    fun `enraged phases attack faster and only the first one transforms`() {
        val phases = vetionSpec(VETION_REST).phases
        assertEquals(ATTACK_RATE, phases.getValue(PHASE_NORMAL).attackRate)
        assertEquals(ENRAGED_ATTACK_RATE, phases.getValue(PHASE_ENRAGED).attackRate)
        assertEquals(ENRAGED_ATTACK_RATE, phases.getValue(PHASE_ENRAGED_HOUNDS).attackRate)
        assertEquals("npc.vetion_2", phases.getValue(PHASE_ENRAGED).transmog)
        assertEquals(null, phases.getValue(PHASE_ENRAGED_HOUNDS).transmog)
    }

    @Test
    fun `lair entrances lead into their own arena`() {
        for (lair in VETION_LAIRS) {
            assertTrue(VETION_LAIRS.none { it.contains(lair.surface) })
            assertEquals(1, VETION_LAIRS.count { it.arrival.chebyshevDistance(lair.arrival) <= 8 })
        }
    }
}
