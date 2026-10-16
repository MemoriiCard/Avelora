package org.rsmod.content.minigames.barbassault

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotEquals
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
            val varps = BaRole.entries.map { it.varp } + listOf(BaPoints.PROGRESS, BaPoints.QUEEN_KILLS)
            for (varp in varps) {
                assertNotNull(ServerCacheManager.getVarp(varp.asRSCM(RSCMType.VARP)), varp)
            }
            assertNotNull(ServerCacheManager.getNpc(BaGambleScript.COMMANDER.asRSCM(RSCMType.NPC)))
            val rewards = GambleTier.entries.flatMap { BaGamble.table(it) } + BaGamble.RARE
            for (item in rewards) {
                val type = ServerCacheManager.getItem(item.obj.asRSCM(RSCMType.OBJ))
                assertNotNull(type, item.obj)
                if (item.noted) assertNotEquals(0, type!!.certlink, item.obj)
            }
            val objs = BaGear.entries.map { it.obj } + BaEggs.COLOURS.map { it.obj } + listOf(BaGear.GRANITE_BODY, BaGear.PET)
            for (obj in objs) assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
        } finally {
            cache.close()
        }
    }

    @Test
    fun `wave symbols stay within the monster table`() {
        for (wave in 1..BaWave.FINAL) {
            for (kind in BaKind.entries) assert(BaMonsters.symbol(kind, wave) in BaMonsters.ALL)
        }
    }
}
