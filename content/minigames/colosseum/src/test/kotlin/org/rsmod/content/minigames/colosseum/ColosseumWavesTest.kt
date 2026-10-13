package org.rsmod.content.minigames.colosseum

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColosseumWavesTest {
    @Test
    fun `eleven waves then Sol`() {
        for (wave in 1..ColosseumWaves.FINAL_REGULAR_WAVE) {
            assertTrue(ColosseumWaves.main(wave).isNotEmpty(), "wave $wave")
        }
        assertEquals(listOf(ColosseumMonster.Sol), ColosseumWaves.main(ColosseumWaves.SOL_WAVE))
        assertTrue(ColosseumWaves.reinforcements(ColosseumWaves.SOL_WAVE).isEmpty())
    }

    @Test
    fun `warband trio appears every regular wave`() {
        for (wave in 1..ColosseumWaves.FINAL_REGULAR_WAVE) {
            assertTrue(ColosseumWaves.main(wave).containsAll(ColosseumMonster.warband), "wave $wave")
        }
    }

    @Test
    fun `quartet adds one warbander and duo doubles shockwaves`() {
        val base = ColosseumWaves.main(7)
        assertEquals(base.size + 1, ColosseumWaves.main(7, quartet = true).size)
        val shockwaves = { list: List<ColosseumMonster> -> list.count { it == ColosseumMonster.Shockwave } }
        assertEquals(shockwaves(base) * 2, shockwaves(ColosseumWaves.main(7, dynamicDuo = true)))
    }

    @Test
    fun `first wave is warband shaman and jaguar reinforcement`() {
        assertEquals(ColosseumMonster.warband + ColosseumMonster.Shaman, ColosseumWaves.main(1))
        assertEquals(listOf(ColosseumMonster.Jaguar), ColosseumWaves.reinforcements(1))
    }

    @Test
    fun `glory grows with waves and modifiers`() {
        val none = emptySet<ColosseumModifier>()
        assertTrue(ColosseumRewards.glory(5, none) > ColosseumRewards.glory(1, none))
        assertTrue(ColosseumRewards.glory(5, setOf(ColosseumModifier.Quartet)) > ColosseumRewards.glory(5, none))
    }

    @Test
    fun `wave one pays splinters only and later waves can drop uniques`() {
        val first = ColosseumRewards.forWave(1) { 1 }
        assertEquals(listOf(ColosseumRewards.SPLINTERS), first.map { it.obj })
        val lucky = ColosseumRewards.forWave(11) { 0 }
        assertTrue(lucky.size == 2)
    }

    @Test
    fun `modifier offers never repeat`() {
        val offers = ColosseumModifier.offers(setOf(ColosseumModifier.Quartet)) { 0 }
        assertEquals(2, offers.size)
        assertTrue(ColosseumModifier.Quartet !in offers)
    }
}
