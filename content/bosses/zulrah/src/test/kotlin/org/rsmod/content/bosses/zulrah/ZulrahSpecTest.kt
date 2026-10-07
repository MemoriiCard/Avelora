package org.rsmod.content.bosses.zulrah

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class ZulrahSpecTest {
    @Test
    fun `every spec is valid`() {
        for (spec in listOf(zulrahSpec(), meleeSnakelingSpec(), magicSnakelingSpec())) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `one spec drives all three forms`() {
        assertEquals(ZulrahForm.entries.map { it.npc }.toSet(), zulrahSpec().npcTypes.toSet())
    }
}
