package org.rsmod.content.bosses.inferno

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InfernoWavesTest {
    @Test
    fun `there are sixty eight waves`() {
        assertEquals(68, InfernoWaves.FINAL_WAVE)
        for (wave in 1..InfernoWaves.FINAL_WAVE) assertTrue(InfernoWaves.wave(wave).isNotEmpty(), "wave $wave")
    }

    @Test
    fun `first wave is three nibblers and a bat`() {
        assertEquals(
            listOf(InfernoMonster.Bat, InfernoMonster.Nibbler, InfernoMonster.Nibbler, InfernoMonster.Nibbler),
            InfernoWaves.wave(1),
        )
    }

    @Test
    fun `six nibbler precursor waves precede each new monster`() {
        for (wave in listOf(3, 8, 17, 34)) {
            assertEquals(List(6) { InfernoMonster.Nibbler }, InfernoWaves.wave(wave), "wave $wave")
        }
    }

    @Test
    fun `monsters are introduced in order`() {
        fun first(m: InfernoMonster) = (1..68).first { m in InfernoWaves.wave(it) }
        assertEquals(1, first(InfernoMonster.Bat))
        assertEquals(4, first(InfernoMonster.Blob))
        assertEquals(9, first(InfernoMonster.Meleer))
        assertEquals(18, first(InfernoMonster.Ranger))
        assertEquals(35, first(InfernoMonster.Mager))
        assertEquals(67, first(InfernoMonster.Jad))
    }

    @Test
    fun `jad waves`() {
        assertEquals(1, InfernoWaves.wave(67).size)
        assertEquals(3, InfernoWaves.wave(68).size)
        assertTrue(InfernoWaves.wave(68).all { it == InfernoMonster.Jad })
    }

    @Test
    fun `strongest monsters spawn first`() {
        for (wave in 1..InfernoWaves.FINAL_WAVE) {
            val ordinals = InfernoWaves.wave(wave).map { it.ordinal }
            assertEquals(ordinals.sortedDescending(), ordinals, "wave $wave")
        }
    }

    @Test
    fun `tokkul grows with waves and tops out near the wiki cap`() {
        assertEquals(0, InfernoWaves.tokkulFor(0))
        assertTrue(InfernoWaves.tokkulFor(68) in 16000..16500)
        assertTrue(InfernoWaves.tokkulFor(40) < InfernoWaves.tokkulFor(41))
    }
}
