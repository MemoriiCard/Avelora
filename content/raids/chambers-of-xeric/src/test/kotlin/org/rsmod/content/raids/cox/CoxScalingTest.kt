package org.rsmod.content.raids.cox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.cox.party.CoxScaling

class CoxScalingTest {
    private fun snapshot(size: Int, cm: Boolean = false, combat: Int = 126, hp: Int = 99) =
        CoxScaling.Snapshot(size, combat, hp, cm, averageMining = 99)

    @Test
    fun `a maxed solo party sees the infobox stats`() {
        val solo = snapshot(1)
        assertEquals(300, solo.hitpoints(300))
        assertEquals(390, solo.offence(390))
        assertEquals(205, solo.defence(205))
        assertEquals(800, solo.olmHeadHitpoints)
        assertEquals(600, solo.olmHandHitpoints)
        assertEquals(4, solo.olmPhases)
    }

    @Test
    fun `challenge mode adds half to stats`() {
        val solo = snapshot(1, cm = true)
        assertEquals(450, solo.hitpoints(300))
        assertEquals(240, solo.defence(200, tekton = true))
        assertEquals(200, solo.defence(200, crystal = true))
        assertEquals(800, solo.olmHeadHitpoints)
    }

    @Test
    fun `bigger parties scale hitpoints by half the party size`() {
        assertEquals(300 * 3, snapshot(5).hitpoints(300))
        assertEquals(5, snapshot(8).olmPhases)
        assertEquals(800 + 400 * (7 - 3), snapshot(8).olmHeadHitpoints)
    }

    @Test
    fun `low level parties get weaker monsters`() {
        assertEquals(300 * 90 / 126, snapshot(1, combat = 90).hitpoints(300))
        assertEquals(390 * 77 / 99, snapshot(1, hp = 50).offence(390))
    }
}
