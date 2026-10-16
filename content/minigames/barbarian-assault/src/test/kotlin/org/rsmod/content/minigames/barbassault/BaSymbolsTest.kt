package org.rsmod.content.minigames.barbassault

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class BaSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val locs = listOf("loc.barbassault_recruitment_entrance", "loc.barbassault_game_exit")
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            for (npc in BaMonsters.ALL) assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            assertNotNull(ServerCacheManager.getVarp(BaService.HONOUR.asRSCM(RSCMType.VARP)))
            assertEquals(65534, BaService.TIMER.asRSCM(RSCMType.TIMER))
        } finally {
            cache.close()
        }
    }

    @Test
    fun `wave symbols stay within the monster table`() {
        for (wave in 1..BaMatch.WAVES) for (i in 0..8) assert(BaMonsters.symbol(wave, i) in BaMonsters.ALL)
    }
}
