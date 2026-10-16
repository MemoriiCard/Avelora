package org.rsmod.content.bosses.corp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class CorpSpecTest {
    @Test
    fun `corp spec is valid`() {
        val errors = SpecValidator.validate(corpSpec())
        assertTrue(errors.isEmpty(), "$errors")
    }

    @Test
    fun `every scripted ability exists`() {
        val abilities = corpSpec().abilities.keys
        for (ability in listOf(STOMP_ABILITY, CORE_LEECH_ABILITY, SPLIT_DIRECT_ABILITY, SPLIT_NEAR_ABILITY, SPLINTER_DIRECT_ABILITY, SPLINTER_NEAR_ABILITY)) {
            assertTrue(ability in abilities, "$ability is missing")
        }
    }
}
