package org.rsmod.content.minigames.soulwars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class SoulWarsSymbolsTest {
    private val locs =
        listOf(
            "loc.soul_wars_leave_soulwars_portal",
            "loc.soul_wars_blue_exit_portal",
            "loc.soul_wars_red_exit_portal",
            "loc.soul_wars_central_obelisk_neutral",
            "loc.soul_wars_central_obelisk_blue",
            "loc.soul_wars_central_obelisk_red",
        )

    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            val items = listOf(SoulWarsItems.FRAGMENT) + SwTeam.entries.map { SoulWarsItems.cape(it) }
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            for (team in SwTeam.entries) {
                val npc = SoulWarsItems.avatar(team)
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
            assertNotNull(ServerCacheManager.getVarp(SoulWarsService.ZEAL.asRSCM(RSCMType.VARP)))
            assertEquals(65531, SoulWarsService.TIMER.asRSCM(RSCMType.TIMER))
        } finally {
            cache.close()
        }
    }
}
