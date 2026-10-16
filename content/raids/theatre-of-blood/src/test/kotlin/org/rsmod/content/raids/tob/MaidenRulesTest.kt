package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.maiden.MaidenRules
import org.rsmod.content.raids.tob.party.TobMode

class MaidenRulesTest {
    @Test
    fun `crab waves trigger at 70, 50 and 30 percent in order`() {
        assertNull(MaidenRules.nextStage(100, 0))
        assertEquals(0, MaidenRules.nextStage(70, 0))
        assertNull(MaidenRules.nextStage(70, 1))
        assertEquals(1, MaidenRules.nextStage(49, 1))
        assertEquals(2, MaidenRules.nextStage(10, 2))
        assertNull(MaidenRules.nextStage(10, 3))
    }

    @Test
    fun `crab count is two per player capped at ten, always ten in hard mode`() {
        assertEquals(2, MaidenRules.crabCount(1, TobMode.Normal))
        assertEquals(10, MaidenRules.crabCount(5, TobMode.Normal))
        assertEquals(10, MaidenRules.crabCount(1, TobMode.Hard))
        assertEquals(1, MaidenRules.crabCount(1, TobMode.Entry))
    }

    @Test
    fun `leaked crabs raise the tornado max hit and heal twice their health`() {
        assertTrue(MaidenRules.maxHit(5, TobMode.Normal) > MaidenRules.maxHit(0, TobMode.Normal))
        assertEquals(200, MaidenRules.crabHeal(100))
    }

    @Test
    fun `hitting someone makes blood spawns likelier`() {
        assertTrue(MaidenRules.bloodSpawnChance(true) > MaidenRules.bloodSpawnChance(false))
    }
}
