package org.rsmod.content.bosses.vorkath

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class VorkathSpecTest {
    @Test
    fun `every spec is valid`() {
        for (spec in listOf(vorkathSpec(), zombifiedSpawnSpec())) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `every ability the fight runs exists`() {
        val abilities = vorkathSpec().abilities.keys
        val used =
            listOf(
                MELEE_ABILITY, MAGIC_ABILITY, RANGED_ABILITY, DRAGONFIRE_ABILITY, VENOM_FIRE_ABILITY,
                PRAYER_FIRE_ABILITY, ICE_BREATH_ABILITY, FIREBALL_DIRECT_ABILITY,
                FIREBALL_SPLASH_ABILITY, RAPID_FIRE_ABILITY, ACID_ABILITY,
            )
        for (ability in used) assertTrue(ability in abilities, "$ability is missing")
        assertTrue(BLAST_ABILITY in zombifiedSpawnSpec().abilities.keys)
    }
}
