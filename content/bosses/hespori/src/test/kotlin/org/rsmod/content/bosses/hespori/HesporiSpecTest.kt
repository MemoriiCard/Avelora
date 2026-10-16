package org.rsmod.content.bosses.hespori

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class HesporiSpecTest {
    private val spec = HesporiFight.spec()

    @Test
    fun `spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `single phase with all three attacks`() {
        assertEquals(listOf(HesporiFight.PHASE), spec.phases.keys.toList())
        assertTrue(spec.abilities.keys.containsAll(listOf("ranged", "magic", "vines")))
    }
}
