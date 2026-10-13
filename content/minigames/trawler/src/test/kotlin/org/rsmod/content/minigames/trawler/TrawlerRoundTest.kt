package org.rsmod.content.minigames.trawler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrawlerRoundTest {
    private fun round(roll: (Int) -> Int = { 1 }) = TrawlerRound(roll).also { it.start(1) }

    private fun TrawlerRound.run(ticks: Int): List<TrawlerEvent> {
        val all = ArrayList<TrawlerEvent>()
        repeat(ticks) { all += tick() }
        return all
    }

    @Test
    fun `leaks appear on schedule and drain water`() {
        val r = round()
        val events = r.run(TrawlerRound.BASE_LEAK_INTERVAL)
        assertEquals(1, events.count { it is TrawlerEvent.Leak })
        assertEquals(1, r.leakCount)
        r.run(60)
        assertTrue(r.water > 0)
    }

    @Test
    fun `patching a leak awards points and stops it`() {
        val r = round()
        r.run(TrawlerRound.BASE_LEAK_INTERVAL)
        val hull = (0 until TrawlerRound.HULLS).first { r.isLeaking(it) }
        assertTrue(r.patch(slot = 1, hull = hull))
        assertFalse(r.isLeaking(hull))
        assertEquals(TrawlerRound.PATCH_POINTS, r.pointsOf(1))
        assertFalse(r.patch(1, hull))
    }

    @Test
    fun `bailing needs water and awards points`() {
        val r = round()
        assertFalse(r.bail(1))
        r.run(TrawlerRound.BASE_LEAK_INTERVAL + 30)
        assertTrue(r.bail(1))
        assertEquals(TrawlerRound.BAIL_POINTS, r.pointsOf(1))
    }

    @Test
    fun `unattended boat sinks`() {
        val r = round()
        val events = r.run(TrawlerRound.TRAWL_TICKS * 3)
        assertTrue(events.contains(TrawlerEvent.Sunk) || events.contains(TrawlerEvent.TimeUp))
    }

    @Test
    fun `torn net stops catch uptime until mended`() {
        var rip = true
        val r = TrawlerRound { if (rip) 0 else 1 }.also { it.start(1) }
        r.run(10)
        assertFalse(r.netIntact(0))
        assertTrue(r.uptime() <= 0.1)
        rip = false
        assertTrue(r.repairNet(1, 0))
        assertTrue(r.repairNet(1, 1))
        assertEquals(TrawlerRound.NET_POINTS * 2, r.pointsOf(1))
    }

    @Test
    fun `fish need fifty points and scale with contribution`() {
        val r = round()
        r.run(TrawlerRound.TRAWL_TICKS - 1)
        assertEquals(0, r.fishFor(1))
        repeat(TrawlerRound.MIN_POINTS / TrawlerRound.BAIL_POINTS) { r.bail(1) }
        val low = r.fishFor(1)
        repeat(100) { r.bail(1) }
        assertTrue(r.fishFor(1) >= low)
    }

    @Test
    fun `hull coordinates map to indexes`() {
        assertEquals(0, TrawlerHullMap.indexAt(1885, 4826))
        assertEquals(7, TrawlerHullMap.indexAt(1892, 4826))
        assertEquals(8, TrawlerHullMap.indexAt(1885, 4823))
        assertEquals(15, TrawlerHullMap.indexAt(1892, 4823))
        assertEquals(null, TrawlerHullMap.indexAt(1893, 4823))
    }

    @Test
    fun `higher fishing unlocks better fish and the top tier dominates`() {
        assertTrue(TrawlerLoot.eligible(14).isEmpty())
        assertEquals(3, TrawlerLoot.eligible(15).size)
        assertEquals("obj.raw_mantaray", TrawlerLoot.eligible(99).last().obj)
        val random = java.util.Random(1)
        val picks = List(2000) { TrawlerLoot.pick(99) { random.nextInt(it) }!!.obj }
        assertTrue(picks.count { it == "obj.raw_mantaray" } > 1400)
    }

    @Test
    fun `junk count distribution and angler odds`() {
        assertEquals(2, TrawlerLoot.junkCount { 0 })
        assertEquals(1, TrawlerLoot.junkCount { 20 })
        assertEquals(0, TrawlerLoot.junkCount { 99 })
        assertNotNull(TrawlerLoot.anglerPiece({ false }) { 0 })
        assertEquals(null, TrawlerLoot.anglerPiece({ false }) { 1 })
    }
}
