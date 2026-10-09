package org.rsmod.content.minigames.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class PcSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val locs =
                listOf(
                    "loc.pest_lander_gangplank",
                    "loc.pest_lander_gangplank_2",
                    "loc.pest_lander_gangplank_3",
                )
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            for (npc in PcNpcs.ALL) assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            assertNotNull(ServerCacheManager.getVarp(PcService.POINTS.asRSCM(RSCMType.VARP)))
            assertEquals(65529, PcService.TIMER.asRSCM(RSCMType.TIMER))
        } finally {
            cache.close()
        }
    }

    @Test
    fun `tier follows combat level within bounds`() {
        assertEquals(1, PcNpcs.tier(3))
        assertEquals(5, PcNpcs.tier(126))
        assertEquals(PcNpcs.pest(2, 0), "npc.pest_brawler_2")
    }
}
