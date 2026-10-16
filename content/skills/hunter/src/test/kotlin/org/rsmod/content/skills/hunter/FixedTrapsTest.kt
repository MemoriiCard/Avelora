package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FixedTrapsTest {
    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
    }

    private fun loc(symbol: String) =
        assertNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)), symbol)

    private fun obj(symbol: String) =
        assertNotNull(ServerCacheManager.getItem(symbol.asRSCM(RSCMType.OBJ)), symbol)

    @Test
    fun `every site loc exists in the cache`() {
        for (site in FixedSites.all) {
            listOf(site.idleLoc, site.setLoc, site.failingLoc).forEach(::loc)
        }
    }

    @Test
    fun `every catch loc, npc and loot symbol exists in the cache`() {
        val catches = FixedSites.deadfallPrey + FixedSites.nets.map { it.second }
        for (catch in catches) {
            loc(catch.fullLoc)
            if (catch.npc == null) {
                loc(catch.trappingLoc)
            } else {
                loc(catch.trappingLoc)
                loc("${catch.trappingLoc}_m")
                assertNotNull(ServerCacheManager.getNpc(catch.npc.asRSCM(RSCMType.NPC)), catch.npc)
            }
            catch.loot.forEach { obj(it.obj) }
        }
        listOf("obj.net_trap", "obj.net", "obj.rope", "obj.knife", "obj.fletching_knife").forEach(::obj)
    }

    @Test
    fun `fixed trap timer is registered`() {
        assertTrue("timer.hunter_fixed_traps".asRSCM(RSCMType.TIMER) > 0)
    }

    @Test
    fun `deadfall allows two traps and nets scale with level`() {
        assertEquals(2, FixedSites.deadfall.maxTraps(99))
        assertEquals(2, FixedSites.nets.first().first.maxTraps(1))
        assertEquals(5, FixedSites.nets.first().first.maxTraps(80))
    }

    @Test
    fun `creature levels are ascending and catch curves get easier with level`() {
        assertEquals(FixedSites.deadfallPrey.map { it.level }.sorted(), FixedSites.deadfallPrey.map { it.level })
        for (catch in FixedSites.deadfallPrey + FixedSites.nets.map { it.second }) {
            assertTrue(catch.catchLow < catch.catchHigh, catch.name)
            assertTrue(catch.xp > 0)
        }
    }
}
