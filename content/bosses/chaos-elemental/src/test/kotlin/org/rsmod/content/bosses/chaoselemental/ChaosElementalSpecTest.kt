package org.rsmod.content.bosses.chaoselemental

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class ChaosElementalSpecTest {
    private val spec = chaosElementalSpec()

    @Test
    fun `spec is valid`() {
        val errors = SpecValidator.validate(spec)
        assertTrue(errors.isEmpty(), errors.toString())
    }

    @Test
    fun `has the three wiki attacks`() {
        assertEquals(setOf("discord", "confusion", "madness"), spec.abilities.keys)
    }
}
