package org.rsmod.content.bosses.giantmole

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.runtime.BossEncounter
import org.rsmod.api.bosses.runtime.HitContext
import org.rsmod.api.bosses.validation.SpecValidator
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class GiantMoleSpecTest {
    private val spec = giantMoleSpec { it.hitpoints > BURROW_MIN_HP }

    @Test
    fun `spec is valid`() {
        assertTrue(SpecValidator.validateAll(listOf(spec)).isEmpty())
    }

    @Test
    fun `burrowing is a single hit reaction`() {
        assertEquals(1, spec.hitReactions.size)
    }

    @Test
    fun `burrows only between half and ten hitpoints`() {
        assertFalse(canBurrowAt(hp = 101))
        assertTrue(canBurrowAt(hp = 100))
        assertTrue(canBurrowAt(hp = 11))
        assertFalse(canBurrowAt(hp = 10))
        assertFalse(canBurrowAt(hp = 1))
    }

    private fun canBurrowAt(hp: Int): Boolean {
        val type = NpcServerType(id = 1, name = "Giant Mole", size = 3, hitpoints = 200)
        val npc = Npc(type, CoordGrid(0, 27, 81, 31, 5)).apply { hitpoints = hp }
        val encounter = BossEncounter(npc, spec, MapClock())
        val hit = HitContext(HitType.Melee, damage = 5, righthand = null, secondary = null)
        return encounter.evaluate(spec.hitReactions.single().requires, hit = hit)
    }
}
