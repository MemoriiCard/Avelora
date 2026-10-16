package org.rsmod.content.bosses.inferno

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InfernoPillarsTest {
    @Test
    fun `collapse damage only reaches players close to the pillar and is capped`() {
        assertEquals(0, InfernoPillars.collapseDamage(30, InfernoPillars.COLLAPSE_RADIUS + 1))
        assertEquals(30, InfernoPillars.collapseDamage(30, 1))
        assertEquals(InfernoPillars.COLLAPSE_MAX, InfernoPillars.collapseDamage(999, 0))
    }

    @Test
    fun `only the four melee ranged and bat types can be resurrected once`() {
        val rez = InfernoPillars.RESURRECTABLE
        assertTrue(InfernoMonster.Meleer.npc in rez && InfernoMonster.Ranger.npc in rez)
        assertTrue(InfernoMonster.Bat.npc in rez && InfernoMonster.Blob.npc in rez)
        assertFalse(InfernoMonster.Nibbler.npc in rez || InfernoMonster.Mager.npc in rez || InfernoMonster.Jad.npc in rez)
        assertTrue(InfernoPillars.canRevive(InfernoMonster.Ranger.npc, alreadyRevived = false, magerAlive = true))
        assertFalse(InfernoPillars.canRevive(InfernoMonster.Ranger.npc, alreadyRevived = true, magerAlive = true))
        assertFalse(InfernoPillars.canRevive(InfernoMonster.Ranger.npc, alreadyRevived = false, magerAlive = false))
        assertFalse(InfernoPillars.canRevive(InfernoMonster.Nibbler.npc, alreadyRevived = false, magerAlive = true))
    }

    @Test
    fun `there is one symbol and one tile per pillar`() {
        assertEquals(InfernoPillars.SYMBOLS.size, InfernoPillars.TILES.size)
    }
}
