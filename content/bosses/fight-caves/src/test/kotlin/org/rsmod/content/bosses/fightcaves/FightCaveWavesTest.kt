package org.rsmod.content.bosses.fightcaves

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.content.bosses.fightcaves.FightCaveMonster.KetZek
import org.rsmod.content.bosses.fightcaves.FightCaveMonster.TokXil
import org.rsmod.content.bosses.fightcaves.FightCaveMonster.TzKek
import org.rsmod.content.bosses.fightcaves.FightCaveMonster.TzKih
import org.rsmod.content.bosses.fightcaves.FightCaveMonster.TzTokJad
import org.rsmod.content.bosses.fightcaves.FightCaveMonster.YtMejKot

class FightCaveWavesTest {
    @Test
    fun `waves follow the wiki table`() {
        assertEquals(listOf(TzKih), FightCaveWaves.wave(1))
        assertEquals(listOf(TzKih, TzKih), FightCaveWaves.wave(2))
        assertEquals(listOf(TzKek), FightCaveWaves.wave(3))
        assertEquals(listOf(TzKek, TzKih, TzKih), FightCaveWaves.wave(5))
        assertEquals(listOf(TzKek, TzKek), FightCaveWaves.wave(6))
        assertEquals(listOf(TokXil), FightCaveWaves.wave(7))
        assertEquals(listOf(TokXil, TokXil), FightCaveWaves.wave(14))
        assertEquals(listOf(YtMejKot), FightCaveWaves.wave(15))
        assertEquals(listOf(YtMejKot, YtMejKot), FightCaveWaves.wave(30))
        assertEquals(listOf(KetZek), FightCaveWaves.wave(31))
        assertEquals(listOf(KetZek, YtMejKot, TokXil, TokXil), FightCaveWaves.wave(60))
        assertEquals(listOf(KetZek, YtMejKot, YtMejKot), FightCaveWaves.wave(61))
        assertEquals(listOf(KetZek, KetZek), FightCaveWaves.wave(62))
        assertEquals(listOf(TzTokJad), FightCaveWaves.wave(FightCaveWaves.FINAL_WAVE))
    }

    @Test
    fun `strongest monster spawns first in every wave`() {
        for (wave in 1..FightCaveWaves.FINAL_WAVE) {
            val monsters = FightCaveWaves.wave(wave)
            assertEquals(monsters.sortedByDescending { it.ordinal }, monsters, "wave $wave")
        }
    }

    @Test
    fun `tokkul matches the wiki`() {
        assertEquals(2, FightCaveWaves.tokkulFor(1))
        assertEquals(4, FightCaveWaves.tokkulFor(2) - FightCaveWaves.tokkulFor(1))
        assertEquals(8032, FightCaveWaves.tokkulFor(FightCaveWaves.FINAL_WAVE))
        assertEquals(0, FightCaveWaves.tokkulFor(0))
    }

    @Test
    fun `jad spawns where the wave three tz-kek did`() {
        for (rotation in FightCaveArena.ROTATION.indices) {
            val kek = FightCaveArena.spawnPoints(3, rotation, 1).single()
            val jad = FightCaveArena.spawnPoints(FightCaveWaves.FINAL_WAVE, rotation, 1).single()
            assertEquals(kek, jad)
        }
    }
}
