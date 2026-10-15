package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PcPestsTest {
    @Test
    fun `spawn weights add up to one hundred`() {
        assertEquals(100, PcPest.TOTAL_WEIGHT)
    }

    @Test
    fun `rolling walks the table in order`() {
        assertEquals(PcPest.Splatter, PcPest.roll(0))
        assertEquals(PcPest.Splatter, PcPest.roll(22))
        assertEquals(PcPest.Defiler, PcPest.roll(23))
        assertEquals(PcPest.Shifter, PcPest.roll(43))
        assertEquals(PcPest.Torcher, PcPest.roll(63))
        assertEquals(PcPest.Spinner, PcPest.roll(83))
        assertEquals(PcPest.Brawler, PcPest.roll(90))
        assertEquals(PcPest.Ravager, PcPest.roll(99))
    }

    @Test
    fun `an out of range roll is clamped`() {
        assertEquals(PcPest.Splatter, PcPest.roll(-5))
        assertEquals(PcPest.Ravager, PcPest.roll(500))
    }

    @Test
    fun `only the wiki's knight attackers hurt the knight`() {
        val hurting = PcPest.entries.filter { it.knightDamage > 0 }.toSet()
        assertEquals(
            setOf(PcPest.Defiler, PcPest.Shifter, PcPest.Torcher, PcPest.Spinner, PcPest.Ravager),
            hurting,
        )
        assertEquals(0, PcPest.Brawler.knightDamage)
        assertEquals(0, PcPest.Splatter.knightDamage)
    }

    @Test
    fun `shifters hit hardest and ranged pests reach furthest`() {
        assertEquals(PcPest.Shifter, PcPest.entries.maxBy { it.knightDamage })
        assertEquals(setOf(PcPest.Defiler, PcPest.Torcher), PcPest.entries.filter { it.knightReach > 2 }.toSet())
    }

    @Test
    fun `symbols resolve back to their kind`() {
        for (symbol in PcNpcs.PESTS) assertEquals(true, PcPest.kindOf(symbol) != null, symbol)
    }
}
