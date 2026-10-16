package org.rsmod.content.raids.toa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.invocation.ToaInvocations
import org.rsmod.content.raids.toa.party.ToaMode
import org.rsmod.content.raids.toa.party.ToaScaling

class ToaRulesTest {
    @Test
    fun `mode follows raid level bands`() {
        assertEquals(ToaMode.Entry, ToaMode.of(0))
        assertEquals(ToaMode.Entry, ToaMode.of(149))
        assertEquals(ToaMode.Normal, ToaMode.of(150))
        assertEquals(ToaMode.Normal, ToaMode.of(299))
        assertEquals(ToaMode.Expert, ToaMode.of(300))
        assertFalse(ToaMode.Entry.awardsUniques)
    }

    @Test
    fun `team hp scales by party size`() {
        assertEquals(100, ToaScaling.teamHpPercent(1))
        assertEquals(190, ToaScaling.teamHpPercent(2))
        assertEquals(280, ToaScaling.teamHpPercent(3))
        assertEquals(340, ToaScaling.teamHpPercent(4))
        assertEquals(580, ToaScaling.teamHpPercent(8))
    }

    @Test
    fun `raid level adds two percent per five levels`() {
        assertEquals(40, ToaScaling.levelBonusPercent(100))
        assertEquals(200, ToaScaling.levelBonusPercent(500))
        assertEquals(150, ToaScaling.damageBonusPercent(500))
        assertEquals(1400, ToaScaling.hitpoints(1000, 1, 100))
        assertEquals(250, ToaScaling.damage(100, 500))
    }

    @Test
    fun `attempts category is exclusive`() {
        var active = ToaInvocations.toggle(emptySet(), ToaInvocation.TryAgain)
        active = ToaInvocations.toggle(active, ToaInvocation.HardcoreRun)
        assertEquals(setOf(ToaInvocation.HardcoreRun), active)
        assertEquals(1, ToaInvocations.attempts(active))
        assertEquals(25, ToaInvocations.raidLevel(active))
    }

    @Test
    fun `prerequisites gate and cascade`() {
        val locked = ToaInvocations.toggle(emptySet(), ToaInvocation.Insanity)
        assertTrue(locked.isEmpty())
        var active = emptySet<ToaInvocation>()
        for (step in listOf(ToaInvocation.Overclocked, ToaInvocation.Overclocked2, ToaInvocation.Insanity)) {
            active = ToaInvocations.toggle(active, step)
        }
        assertEquals(70, ToaInvocations.raidLevel(active))
        active = ToaInvocations.toggle(active, ToaInvocation.Overclocked)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `time limit and supplies read from active set`() {
        val active = setOf(ToaInvocation.RunForIt, ToaInvocation.NeedLessHelp)
        assertEquals(30, ToaInvocations.timeLimit(active)?.minutes)
        assertEquals(33, ToaInvocations.supplyPercent(active))
        assertNull(ToaInvocations.attempts(active))
        assertEquals(100, ToaInvocations.supplyPercent(emptySet()))
    }

    @Test
    fun `every invocation total is capped`() {
        assertEquals(ToaInvocations.MAX_LEVEL.coerceAtMost(ToaInvocation.entries.sumOf { it.level }),
            ToaInvocations.raidLevel(ToaInvocation.entries.toSet()))
    }
}
