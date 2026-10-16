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
            val locs = PcLander.entries.map { it.gangplank }
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            for (npc in PcNpcs.ALL) assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            for (knight in PcShopScript.KNIGHTS) {
                assertNotNull(ServerCacheManager.getNpc(knight.asRSCM(RSCMType.NPC)), knight)
            }
            val items =
                PcShop.VOID.map { it.obj } +
                    PcShop.ELITE.flatMap { listOf(it.from, it.to) } +
                    PcSeal.CHARGES.map { PcSeal.symbol(it) }
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            assertNotNull(ServerCacheManager.getVarp(PcPoints.VARP.asRSCM(RSCMType.VARP)))
        } finally {
            cache.close()
        }
    }

    @Test
    fun `tier follows combat level within bounds`() {
        assertEquals(1, PcNpcs.tier(3))
        assertEquals(5, PcNpcs.tier(126))
        assertEquals("npc.pest_brawler_2", PcPest.Brawler.symbol(2))
        assertEquals("npc.pest_shifter_3a", PcPest.Shifter.symbol(3))
    }
}
