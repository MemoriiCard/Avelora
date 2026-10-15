package org.rsmod.content.minigames.barbassault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaScoringTest {
    private fun stats(kills: Int = 0, escapes: Int = 0, healed: Int = 0, eggs: Int = 0) =
        BaWaveStats().also {
            it.kills = kills
            it.escapes = escapes
            it.healed = healed
            it.eggs = eggs
        }

    @Test
    fun `attacker scores wave per kill`() {
        assertEquals(10 * 4, BaScoring.wavePoints(BaRole.Attacker, 4, stats(kills = 10)))
    }

    @Test
    fun `defender kills are weighted and escapes subtract with a cap`() {
        assertEquals(18, BaScoring.wavePoints(BaRole.Defender, 4, stats(kills = 2)))
        assertEquals(18 * 3 - 10, BaScoring.wavePoints(BaRole.Defender, 4, stats(kills = 6, escapes = 9)))
    }

    @Test
    fun `defender cannot score below zero`() {
        assertEquals(0, BaScoring.wavePoints(BaRole.Defender, 5, stats(escapes = 4)))
    }

    @Test
    fun `healer heal credit is capped`() {
        assertEquals(28, BaScoring.wavePoints(BaRole.Healer, 3, stats(healed = 10_000)))
        assertEquals(2 + 5, BaScoring.wavePoints(BaRole.Healer, 3, stats(kills = 1, healed = 90)))
    }

    @Test
    fun `collector eggs cap at sixty`() {
        assertEquals(13, BaScoring.wavePoints(BaRole.Collector, 5, stats(eggs = 60)))
        assertEquals(13, BaScoring.wavePoints(BaRole.Collector, 5, stats(eggs = 500)))
        assertEquals(0, BaScoring.wavePoints(BaRole.Collector, 5, stats()))
    }

    @Test
    fun `role levels follow the cumulative thresholds`() {
        assertEquals(1, BaLevels.level(0))
        assertEquals(1, BaLevels.level(199))
        assertEquals(2, BaLevels.level(200))
        assertEquals(3, BaLevels.level(500))
        assertEquals(4, BaLevels.level(900))
        assertEquals(5, BaLevels.level(1400))
        assertEquals(35, BaLevels.healAmount(5))
        assertTrue(BaLevels.lureRange(2) > BaLevels.lureRange(1))
    }

    @Test
    fun `waves grow with squad size and the last wave is the queen`() {
        for (wave in 1..9) {
            assertTrue(BaWave.compose(wave, 5).size > BaWave.compose(wave, 1).size, "wave $wave")
        }
        assertTrue(BaWave.compose(1, 1).none { it == BaKind.Healer })
        assertTrue(BaWave.compose(2, 1).any { it == BaKind.Healer })
        val last = BaWave.compose(BaWave.FINAL, 3)
        assertEquals(1, last.count { it == BaKind.Queen })
        assertEquals(3, last.count { it == BaKind.Spawn })
    }

    @Test
    fun `gear gates points and queen kills`() {
        assertEquals(null, BaGear.Boots.blocked(80, 0))
        assertTrue(BaGear.Boots.blocked(79, 0) != null)
        assertTrue(BaGear.Torso.blocked(300, 0) != null)
        assertEquals(null, BaGear.Torso.blocked(300, 1))
        assertTrue(BaGear.graniteBodyBlocked(95_000, 6) != null)
        assertTrue(BaGear.graniteBodyBlocked(94_999, 7) != null)
        assertEquals(null, BaGear.graniteBodyBlocked(95_000, 7))
    }
}
