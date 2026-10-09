package org.rsmod.content.bosses.hueycoatl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class HueycoatlSpecTest {
    private val spec = HueycoatlFight.spec()

    @Test
    fun `spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `enraged phase attacks faster`() {
        val enraged = spec.phases.getValue(HueycoatlFight.PHASE_ENRAGED)
        assertEquals(HueycoatlFight.ENRAGE_HP, enraged.entryHp)
        assertTrue(enraged.attackRate!! < HueycoatlFight.ATTACK_RATE)
    }
}
