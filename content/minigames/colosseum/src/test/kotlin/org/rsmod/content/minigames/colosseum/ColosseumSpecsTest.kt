package org.rsmod.content.minigames.colosseum

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class ColosseumSpecsTest {
    @Test
    fun `every monster spec is valid`() {
        for (spec in colosseumSpecs()) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `every colosseum npc has a spec`() {
        val covered = colosseumSpecs().flatMap { it.npcTypes }.toSet()
        for (monster in ColosseumMonster.entries) assertTrue(monster.npc in covered, monster.npc)
    }
}
