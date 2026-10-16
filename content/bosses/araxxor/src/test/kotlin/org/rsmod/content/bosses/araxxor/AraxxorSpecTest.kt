package org.rsmod.content.bosses.araxxor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class AraxxorSpecTest {
    private val spec = AraxxorFight.spec()

    @Test
    fun `spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `enrage phase is entered once at low hp`() {
        assertEquals(AraxxorFight.PHASE_OPENING, spec.phases.keys.first())
        assertEquals(AraxxorFight.ENRAGE_HP, spec.phases.getValue(AraxxorFight.PHASE_ENRAGED).entryHp)
    }
}
