package org.rsmod.content.bosses.zalcano

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class ZalcanoSpecTest {
    private val spec = ZalcanoFight.spec()

    @Test
    fun `spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `has attacks and both specials`() {
        assertEquals(listOf(ZalcanoFight.PHASE), spec.phases.keys.toList())
        assertTrue(spec.abilities.keys.containsAll(listOf("melee", "ranged", "magic", "rockfall", "golems")))
    }
}
