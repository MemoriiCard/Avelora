package org.rsmod.content.bosses.crazyarchaeologist

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Selector
import org.rsmod.api.bosses.validation.SpecValidator

class CrazyArchaeologistSpecTest {
    @Test
    fun `spec is valid`() {
        val errors = SpecValidator.validate(crazyArchaeologistSpec())
        assertTrue(errors.isEmpty(), "$errors")
    }

    @Test
    fun `punches only up close`() {
        val selector = crazyArchaeologistSpec().phases.getValue("combat").selector as Selector.WeightedRandom
        val punches = selector.entries.filter { it.ability == "punch" }
        assertEquals(1, punches.size)
        assertEquals(Condition.WithinMeleeRange, punches.single().requires)
    }
}
