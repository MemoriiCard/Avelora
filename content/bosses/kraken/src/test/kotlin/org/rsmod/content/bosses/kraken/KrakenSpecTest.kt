package org.rsmod.content.bosses.kraken

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class KrakenSpecTest {
    @Test
    fun `every spec is valid`() {
        for (spec in listOf(krakenSpec(), tentacleSpec(), caveKrakenSpec())) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `each spec covers both the whirlpool and the emerged form`() {
        assertEquals(setOf(KRAKEN, KRAKEN_WHIRLPOOL), krakenSpec().npcTypes.toSet())
        assertEquals(setOf(TENTACLE, TENTACLE_WHIRLPOOL), tentacleSpec().npcTypes.toSet())
        assertEquals(setOf(CAVE_KRAKEN, CAVE_KRAKEN_WHIRLPOOL), caveKrakenSpec().npcTypes.toSet())
    }

    @Test
    fun `whirlpools sit inside the boss lair`() {
        assertTrue(KrakenCove.inLair(KrakenCove.BOSS_WHIRLPOOL))
        assertTrue(KrakenCove.TENTACLE_WHIRLPOOLS.all(KrakenCove::inLair))
        assertTrue(KrakenCove.inLair(KrakenCove.INSIDE_LAIR))
        assertTrue(!KrakenCove.inLair(KrakenCove.OUTSIDE_LAIR))
    }
}
