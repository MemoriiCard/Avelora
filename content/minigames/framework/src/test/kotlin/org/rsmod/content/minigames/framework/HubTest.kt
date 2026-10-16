package org.rsmod.content.minigames.framework

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

class HubCratesTest {
    @Test
    fun `table weights fill the denominator`() {
        assertEquals(HubCrates.DENOMINATOR, HubCrates.TABLE.sumOf { it.weight })
    }

    @Test
    fun `every roll lands on a drop within its range`() {
        for (pick in 0 until HubCrates.DENOMINATOR) {
            val reward = HubCrates.roll { bound -> if (bound == HubCrates.DENOMINATOR) pick else 0 }
            assertTrue(reward.amount >= 1)
        }
        val tokens = HubCrates.roll { bound -> if (bound == HubCrates.DENOMINATOR) 0 else bound - 1 }
        assertEquals(null, tokens.obj)
        assertEquals(120, tokens.amount)
    }

    @Test
    fun `bots shrink the crate chance and wins beat losses`() {
        fun chance(botShare: Double, won: Boolean) = (0 until 100).count { HubCrates.earned(botShare, won) { _ -> it } == 1 }
        assertEquals(HubCrates.WIN_PERCENT, chance(0.0, true))
        assertEquals(HubCrates.LOSS_PERCENT, chance(0.0, false))
        assertTrue(chance(0.5, true) < chance(0.0, true))
    }
}

class MinigameBoardTest {
    @Test
    fun `every minigame with a lobby is listed once and the bonus game is flagged`() {
        assertEquals(MinigameBoard.ENTRIES.size, MinigameBoard.ENTRIES.map { it.label }.toSet().size)
        for (day in 0L until Minigame.entries.size.toLong()) {
            val bonus = BonusOfTheDay.gameFor(day)
            val flagged = MinigameBoard.ENTRIES.filter { "double points" in MinigameBoard.label(it, day) }
            assertEquals(MinigameBoard.ENTRIES.filter { it.game == bonus }, flagged)
        }
    }
}

@ResourceLock("server-cache")
class HubSymbolsTest {
    @Test
    fun `store, crate and reward symbols resolve in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val objs = HubStore.ALL.map { it.obj } + HubCrates.TABLE.mapNotNull { it.obj } + HubCrates.CRATE
            for (obj in objs.toSet()) {
                assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
            }
        } finally {
            cache.close()
        }
    }
}
