package org.rsmod.content.bosses.scorpia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator
import org.rsmod.map.CoordGrid

class ScorpiaSpecTest {
    @Test
    fun `specs are valid`() {
        for (spec in listOf(scorpiaSpec(), offspringSpec())) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "$errors")
        }
    }

    @Test
    fun `scorpia stings in every phase`() {
        val spec = scorpiaSpec()
        assertEquals(setOf(PHASE_FIGHT, PHASE_WOUNDED, PHASE_GUARDED), spec.phases.keys)
    }

    @Test
    fun `each surface entrance leads to its own cave exit and back`() {
        assertTrue(ScorpionPit.inCave(ScorpionPit.SCORPIA_SPAWN))
        for (cave in PitCave.entries) {
            assertTrue(ScorpionPit.inCave(cave.cave), "$cave cave tile is outside the pit")
            assertEquals(cave, ScorpionPit.nearestCave(cave.surface))
            assertEquals(cave, ScorpionPit.nearestSurface(cave.cave))
        }
        assertEquals(PitCave.NorthEast, ScorpionPit.nearestCave(CoordGrid(3243, 3950, 0)))
    }
}
