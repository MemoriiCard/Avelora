package org.rsmod.content.bosses.fightcaves

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.validation.SpecValidator

class FightCaveSpecsTest {
    @Test
    fun `every monster spec is valid`() {
        for (spec in fightCaveSpecs(Condition.Always)) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `every fight cave npc has a spec`() {
        val covered = fightCaveSpecs(Condition.Always).flatMap { it.npcTypes }.toSet()
        for (monster in FightCaveMonster.entries) {
            assertTrue(monster.npc in covered, "${monster.npc} has no spec")
        }
        assertTrue("npc.tzhaar_fightcave_swarm_2spawn" in covered)
        assertTrue("npc.tzhaar_fightcave_swarm_boss_cleric" in covered)
    }
}
