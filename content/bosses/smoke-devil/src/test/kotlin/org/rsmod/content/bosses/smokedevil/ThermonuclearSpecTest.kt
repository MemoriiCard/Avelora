package org.rsmod.content.bosses.smokedevil

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class ThermonuclearSpecTest {
    @Test
    fun `thermy spec is valid`() {
        val errors = SpecValidator.validate(thermySpec())
        assertTrue(errors.isEmpty(), "$errors")
    }

    @Test
    fun `thermy spawns inside the lair and the lair door sits on its edge`() {
        assertTrue(SmokeDevilDungeon.inLair(SmokeDevilDungeon.THERMY_SPAWN))
        assertTrue(SmokeDevilDungeon.inLair(SmokeDevilDungeon.INSIDE_LAIR))
        assertFalse(SmokeDevilDungeon.inLair(SmokeDevilDungeon.OUTSIDE_LAIR))
    }
}
