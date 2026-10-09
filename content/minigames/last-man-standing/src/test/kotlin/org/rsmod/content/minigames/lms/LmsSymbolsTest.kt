package org.rsmod.content.minigames.lms

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class LmsSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (loc in LmsScript.CHESTS) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            assertNotNull(ServerCacheManager.getNpc(LmsScript.WIZARD.asRSCM(RSCMType.NPC)))
            val items = LmsLoot.TABLE.map { it.obj } + listOf("obj.br_rune_scimitar", "obj.br_shark")
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            assertNotNull(ServerCacheManager.getVarp(LmsService.POINTS.asRSCM(RSCMType.VARP)))
            assertEquals(65533, LmsService.TIMER.asRSCM(RSCMType.TIMER))
        } finally {
            cache.close()
        }
    }
}
