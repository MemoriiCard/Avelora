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
class HunterCreaturesTest {
    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
    }

    @Test
    fun `every trap, creature, loc and loot symbol exists in the cache`() {
        for (kind in TrapKind.entries) {
            assertNotNull(ServerCacheManager.getItem(kind.obj.asRSCM(RSCMType.OBJ)), kind.obj)
            for (loc in listOf(kind.setLoc, kind.failingLoc, kind.brokenLoc)) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
        }
        for (creature in HunterCreatures.all) {
            assertNotNull(ServerCacheManager.getNpc(creature.npc.asRSCM(RSCMType.NPC)), creature.npc)
            val trapping = if ("%s" in creature.trappingLoc) {
                listOf("n", "e", "s", "w").map { creature.trappingLoc.format(it) }
            } else {
                listOf(creature.trappingLoc)
            }
            for (loc in trapping + creature.fullLoc) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
            for (loot in creature.loot) {
                assertNotNull(ServerCacheManager.getItem(loot.obj.asRSCM(RSCMType.OBJ)), loot.obj)
            }
        }
    }

    @Test
    fun `trap timer is registered`() {
        assertTrue("timer.hunter_traps".asRSCM(RSCMType.TIMER) > 0)
    }

    @Test
    fun `creatures match the wiki levels and experience`() {
        val byNpc = HunterCreatures.all.associate { it.npc to (it.level to it.xp) }
        assertEquals(1 to 34.0, byNpc["npc.hunting_bird_jungle"])
        assertEquals(19 to 95.2, byNpc["npc.multicoloured_bird"])
        assertEquals(53 to 198.4, byNpc["npc.hunting_chinchompa"])
        assertEquals(73 to 315.0, byNpc["npc.hunting_chinchompa_black"])
    }

    @Test
    fun `trap limit follows the wiki table`() {
        assertEquals(2, HunterCreatures.maxTraps(1))
        assertEquals(2, HunterCreatures.maxTraps(39))
        assertEquals(3, HunterCreatures.maxTraps(40))
        assertEquals(4, HunterCreatures.maxTraps(60))
        assertEquals(5, HunterCreatures.maxTraps(99))
    }
}
