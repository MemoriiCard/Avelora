package org.rsmod.content.minigames.trawler

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class TrawlerSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val items =
                TrawlerLoot.fish.map { it.obj } + TrawlerLoot.junk + TrawlerLoot.angler +
                    listOf(
                        TrawlerLoot.PET,
                        "obj.swamppaste",
                        "obj.rope",
                        "obj.hammer",
                        "obj.bronze_axe",
                        "obj.bucket_bailing",
                        "obj.bucket_bailingfull",
                    )
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            val locs =
                listOf(
                    "loc.trawler_gangplank",
                    "loc.trawler_gangplankoff",
                    "loc.trawlernetland",
                    "loc.trawler_ladder",
                    "loc.trawler_laddertop",
                    "loc.trawler_crate_bucket",
                    "loc.trawler_crate_hammer",
                    "loc.trawler_crate_axes",
                    "loc.trawler_hull_leak",
                    "loc.trawlernet_l",
                    "loc.trawlernet_r",
                )
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            for (npc in listOf("npc.murphy", "npc.murphy_normal")) {
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
            for (hull in 0 until TrawlerRound.HULLS) {
                val name = "varbit.trawler_hull_%02d".format(hull)
                assertNotNull(ServerCacheManager.getVarbit(name.asRSCM(RSCMType.VARBIT)), name)
            }
            "timer.trawler".asRSCM(RSCMType.TIMER)
            "inv.trawler_rewardinv".asRSCM(RSCMType.INV)
        } finally {
            cache.close()
        }
    }
}
