package org.rsmod.content.bosses.inferno

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class InfernoSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (npc in InfernoMonsters.DEATHS.keys) assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            for (loc in listOf("loc.inferno_entrance_op", "loc.inferno_exit")) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
            for (item in listOf("obj.tzhaar_cape_fire", "obj.tzhaar_token")) {
                assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            }
            for (varp in listOf("varp.inferno_wave", "varp.inferno_unlocked")) {
                assertNotNull(ServerCacheManager.getVarp(varp.asRSCM(RSCMType.VARP)), varp)
            }
        } finally {
            cache.close()
        }
    }
}
