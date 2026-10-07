package org.rsmod.content.bosses.kalphitequeen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class KalphiteQueenSpecTest {
    private val spec = kalphiteQueenSpec()

    @Test
    fun `spec is valid`() {
        val errors = SpecValidator.validate(spec)
        assertTrue(errors.isEmpty(), errors.toString())
    }

    @Test
    fun `fight opens in the crawling form`() {
        assertEquals(CRAWLING, spec.phases.keys.first())
    }

    @Test
    fun `airborne form transmogs into the flying queen`() {
        assertEquals(AIRBORNE_NPC, spec.phases.getValue(AIRBORNE).transmog)
    }

    @Test
    fun `both forms are registered so hits on either reach the encounter`() {
        assertEquals(listOf(CRAWLING_NPC, AIRBORNE_NPC), spec.npcTypes)
    }
}
