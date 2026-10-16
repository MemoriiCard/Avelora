package org.rsmod.content.bosses.inferno

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class InfernoSpecsTest {
    @Test
    fun `every monster spec is valid`() {
        for (spec in infernoSpecs()) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `every inferno npc has a spec and a death animation`() {
        val covered = infernoSpecs().flatMap { it.npcTypes }.toSet()
        for (monster in InfernoMonster.entries) {
            assertTrue(monster.npc in covered, "${monster.npc} has no spec")
            assertTrue(monster.npc in InfernoMonsters.DEATHS, "${monster.npc} has no death")
        }
        for (mini in listOf(BLOB_MELEE, BLOB_RANGE, BLOB_MAGE)) {
            assertTrue(mini in covered && mini in InfernoMonsters.DEATHS, mini)
        }
    }
}
