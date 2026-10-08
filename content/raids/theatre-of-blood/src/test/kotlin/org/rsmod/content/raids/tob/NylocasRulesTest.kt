package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.nylocas.NylocasRules
import org.rsmod.content.raids.tob.nylocas.NylocasStyle

class NylocasRulesTest {
    @Test
    fun `alive cap grows with team size`() {
        assertEquals(12, NylocasRules.aliveCap(1))
        assertEquals(12, NylocasRules.aliveCap(3))
        assertEquals(15, NylocasRules.aliveCap(4))
        assertEquals(18, NylocasRules.aliveCap(5))
    }

    @Test
    fun `waves get bigger and every fifth wave from the fifth brings a big one`() {
        assertTrue(NylocasRules.waveSize(30) > NylocasRules.waveSize(0))
        assertTrue(NylocasRules.hasBig(4))
        assertTrue(NylocasRules.hasBig(9))
        assertFalse(NylocasRules.hasBig(5))
        val wave = NylocasRules.wave(4, { 0 })
        assertEquals(1, wave.count { it.big })
        assertEquals(NylocasRules.waveSize(4), wave.size)
    }

    @Test
    fun `the boss never repeats a style back to back`() {
        for (current in NylocasStyle.entries) {
            for (roll in 0..1) {
                assertNotEquals(current, NylocasRules.nextStyle(current, { roll }))
            }
        }
        assertEquals(NylocasStyle.Melee, NylocasRules.nextStyle(null, { 0 }))
    }

    @Test
    fun `the boss only arrives after every wave has spawned and few nylocas remain`() {
        assertFalse(NylocasRules.readyForBoss(30, 0))
        assertFalse(NylocasRules.readyForBoss(31, 10))
        assertTrue(NylocasRules.readyForBoss(31, 3))
    }

    @Test
    fun `big nylocas hit pillars harder`() {
        assertEquals(1, NylocasRules.pillarDamage(false, { 0 }))
        assertEquals(NylocasRules.PILLAR_BIG_MAX, NylocasRules.pillarDamage(true, { NylocasRules.PILLAR_BIG_MAX - 1 }))
    }
}
