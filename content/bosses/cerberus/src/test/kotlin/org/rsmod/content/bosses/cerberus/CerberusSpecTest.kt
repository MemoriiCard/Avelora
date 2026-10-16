package org.rsmod.content.bosses.cerberus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class CerberusSpecTest {
    @Test
    fun `every spec is valid`() {
        for (spec in soulSpecs() + cerberusSpec()) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `one spec covers sitting and attacking cerberus`() {
        assertEquals(setOf(CERBERUS_SITTING, CERBERUS), cerberusSpec().npcTypes.toSet())
    }

    @Test
    fun `each arena contains its own landing, spawn and souls only`() {
        for (arena in CerberusArena.entries) {
            for (tile in listOf(arena.landing, arena.spawn) + arena.soulTiles) {
                assertEquals(arena, CerberusArena.containing(tile))
            }
            assertNull(CerberusArena.containing(arena.hubReturn))
            assertEquals(arena, CerberusArena.byWinch(arena.winch))
        }
    }
}
