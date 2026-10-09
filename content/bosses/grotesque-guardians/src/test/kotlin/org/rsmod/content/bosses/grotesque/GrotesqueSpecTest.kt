package org.rsmod.content.bosses.grotesque

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class GrotesqueSpecTest {
    @Test
    fun `dawn spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(GrotesqueFight.dawnSpec())).isEmpty())
    }

    @Test
    fun `dusk spec validates and has a sweep special`() {
        val spec = GrotesqueFight.duskSpec()
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
        assertTrue("sweep" in spec.abilities)
    }
}
