package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.map.CoordGrid

class CastleWarsTunnelTest {
    @Test
    fun `an open tunnel collapses once and takes two clears to reopen`() {
        val ledger = TunnelLedger()
        assertNull(ledger.clear(0))
        assertTrue(ledger.collapse(0))
        assertFalse(ledger.collapse(0))
        assertEquals(TunnelStage.Loosened, ledger.clear(0))
        assertEquals(TunnelStage.Open, ledger.clear(0))
        assertEquals(TunnelStage.Open, ledger.stage(0))
    }

    @Test
    fun `tunnels are independent and reset reopens them`() {
        val ledger = TunnelLedger()
        ledger.collapse(1)
        assertEquals(TunnelStage.Open, ledger.stage(0))
        ledger.reset()
        assertEquals(TunnelStage.Open, ledger.stage(1))
    }

    @Test
    fun `a rockslide belongs to the nearest tunnel`() {
        assertEquals(0, TunnelRules.nearest(CoordGrid(2390, 9500, 0)))
        assertEquals(1, TunnelRules.nearest(CoordGrid(2399, 9514, 0)))
        assertEquals(2, TunnelRules.nearest(CoordGrid(2403, 9493, 0)))
        assertEquals(3, TunnelRules.nearest(CoordGrid(2411, 9505, 0)))
    }

    @Test
    fun `a tunnel covers a two by two area`() {
        assertEquals(4, TunnelRules.tiles(TunnelRules.SITES[0]).distinct().size)
    }

    @Test
    @ResourceLock("server-cache")
    fun `tunnel and flag symbols exist`() {
        TunnelStage.entries.mapNotNull { it.symbol }.forEach {
            assertNotNull(ServerCacheManager.getObject(it.asRSCM(RSCMType.LOC)), it)
        }
        CwTeam.entries.forEach {
            assertNotNull(ServerCacheManager.getItem(CastleWarsItems.banner(it).asRSCM(RSCMType.OBJ)))
        }
        listOf("loc.castlewars_cavewall_rockslide", "loc.castlewars_scoreboard").forEach {
            assertNotNull(ServerCacheManager.getObject(it.asRSCM(RSCMType.LOC)), it)
        }
    }
}
