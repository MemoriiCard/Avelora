package org.rsmod.content.bosses.dagannothkings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class DagannothKingSpecsTest {
    private val specs = listOf(rexSpec(), primeSpec(), supremeSpec())

    @Test
    fun `all three kings build valid specs`() {
        for (spec in specs) {
            val errors = SpecValidator.validateAll(listOf(spec))
            assertTrue(errors.isEmpty(), errors.toString())
        }
    }

    @Test
    fun `each king has one attack`() {
        specs.forEach { assertEquals(1, it.abilities.size) }
    }

    @Test
    fun `each king is a different npc`() {
        assertEquals(3, specs.flatMap { it.npcTypes }.toSet().size)
    }
}
