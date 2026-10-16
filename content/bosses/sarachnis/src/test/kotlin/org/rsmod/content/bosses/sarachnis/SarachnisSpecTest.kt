package org.rsmod.content.bosses.sarachnis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class SarachnisSpecTest {
    private val spec = SarachnisFight.spec()

    @Test
    fun `spec validates`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `fight opens without spawns and adds one minion per hp threshold`() {
        assertEquals(SarachnisFight.PHASE_OPENING, spec.phases.keys.first())
        assertEquals(
            listOf(SarachnisFight.MELEE_SPAWN_HP, SarachnisFight.MAGE_SPAWN_HP),
            spec.phases.values.mapNotNull { it.entryHp },
        )
    }

    @Test
    fun `minions are bound to the encounter`() {
        assertTrue(spec.abilities.keys.containsAll(listOf("summon_melee", "summon_mage")))
    }
}
