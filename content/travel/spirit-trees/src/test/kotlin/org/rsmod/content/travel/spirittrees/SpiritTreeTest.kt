package org.rsmod.content.travel.spirittrees

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class SpiritTreeTest {
    @Test
    fun `travel locs carry a Travel first op`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (loc in listOf("loc.spirittree_small_2ops", "loc.spirittree_prif_2ops", "loc.spirittree_big_2ops")) {
                val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC))
                assertNotNull(type, loc)
                assertEquals("Travel", type?.actions?.ops?.get(0)?.text, loc)
            }
        } finally {
            cache.close()
        }
    }

    @Test
    fun `destinations are unique`() {
        assertEquals(SpiritTrees.destinations.size, SpiritTrees.destinations.map { it.coords }.toSet().size)
    }
}
