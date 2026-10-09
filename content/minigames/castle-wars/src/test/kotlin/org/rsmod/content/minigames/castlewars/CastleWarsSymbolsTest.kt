package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class CastleWarsSymbolsTest {
    private val locs =
        listOf(
            "loc.castlewars_saradomin_tele",
            "loc.castlewars_zamorak_tele",
            "loc.castlewars_random_tele",
            "loc.castlewars_saradomin_exit",
            "loc.castlewars_zamorak_exit",
            "loc.castlewars_saradomin_quit",
            "loc.castlewars_zamorak_quit",
            "loc.castlewars_saradomin_banner+stand",
            "loc.castlewars_zamorak_banner+stand",
        )

    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            val items =
                CwTeam.entries.flatMap {
                    listOf(CastleWarsItems.cloak(it), CastleWarsItems.banner(it))
                } + CastleWarsItems.TICKET
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            assertEquals(65530, CastleWarsService.TIMER.asRSCM(RSCMType.TIMER))
            assertNotNull(CastleWarsShop.CURRENCY.asRSCM(RSCMType.CURRENCY))
            assertNotNull(CastleWarsShop.JUDGE.asRSCM(RSCMType.NPC))
            for (item in CastleWarsShop.PRICES.keys) {
                assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            }
        } finally {
            cache.close()
        }
    }
}
