package org.rsmod.content.bosses.nightmare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class NightmareSpecTest {
    @Test
    fun `specs are valid`() {
        assertTrue(SpecValidator.validate(nightmareSpec()).isEmpty(), "${SpecValidator.validate(nightmareSpec())}")
        val husk = SpecValidator.validate(huskSpec("npc.nightmare_husk_magic", "spotanim.nightmare_husk_magic_travel", org.rsmod.api.bosses.dsl.Magic))
        assertTrue(husk.isEmpty(), "$husk")
    }

    @Test
    fun `every phase has its own selector`() {
        val phases = nightmareSpec().phases
        for (phase in NightmarePhase.entries) assertTrue(phase.key in phases, phase.key)
        assertEquals(null, NightmarePhase.Three.next)
        assertEquals(NightmarePhase.Two, NightmarePhase.One.next)
    }

    @Test
    fun `spec covers every form the fight transmogs into`() {
        val types = nightmareSpec().npcTypes.toSet()
        for (phase in NightmarePhase.entries) {
            assertTrue(phase.shielded in types)
            assertTrue(phase.weak in types)
        }
        assertTrue(NIGHTMARE in types && NIGHTMARE_BLAST in types)
    }

    @Test
    fun `shield grows with the group`() {
        assertEquals(400, NightmareFight.shieldFor(1))
        assertEquals(1200, NightmareFight.shieldFor(5))
    }

    @Test
    fun `surge path spans the arena`() {
        val path = NightmareArena.surgePath(NightmareArena.SURGE_WEST, NightmareArena.SURGE_EAST)
        assertTrue(path.all { NightmareArena.contains(it) })
        assertEquals((NightmareArena.MAX_X - NightmareArena.MIN_X + 1) * NIGHTMARE_SIZE, path.size)
    }
}
