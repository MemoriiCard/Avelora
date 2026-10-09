package org.rsmod.content.bosses.yama

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class YamaSpecTest {
    private val spec = YamaFight.spec()

    @Test
    fun `spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `enraged phase attacks faster`() {
        val enraged = spec.phases.getValue(YamaFight.PHASE_ENRAGED)
        assertEquals(YamaFight.ENRAGE_HP, enraged.entryHp)
        assertTrue(enraged.attackRate!! < YamaFight.ATTACK_RATE)
    }
}
