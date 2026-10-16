package org.rsmod.content.bosses.chaosfanatic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.spec.Selector
import org.rsmod.api.bosses.validation.SpecValidator

class ChaosFanaticSpecTest {
    @Test
    fun `spec is valid`() {
        val errors = SpecValidator.validate(chaosFanaticSpec())
        assertTrue(errors.isEmpty(), "$errors")
    }

    @Test
    fun `attacks are weighted ten to four to one`() {
        val selector = chaosFanaticSpec().phases.getValue("combat").selector as Selector.WeightedRandom
        val weights = selector.entries.associate { it.ability to it.weight }
        assertEquals(mapOf("standard" to 10, "explosion" to 4, "disarm" to 1), weights)
    }
}
