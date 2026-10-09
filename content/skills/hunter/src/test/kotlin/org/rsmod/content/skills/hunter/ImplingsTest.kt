package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImplingsTest {
    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
    }

    @Test
    fun `every impling npc, jar and net exists`() {
        for (impling in Implings.all + Implings.butterflies) {
            assertNotNull(ServerCacheManager.getItem(impling.emptyJar.asRSCM(RSCMType.OBJ)), impling.emptyJar)
            assertNotNull(ServerCacheManager.getItem(impling.jar.asRSCM(RSCMType.OBJ)), impling.jar)
            for (npc in impling.npcs) {
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
        }
        for (obj in listOf(Implings.EMPTY_JAR, Implings.NET, Implings.MAGIC_NET)) {
            assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
        }
    }

    @Test
    fun `levels follow the wiki and harder implings are harder to catch`() {
        val levels = Implings.all.map { it.level }.sorted()
        assertEquals(listOf(17, 22, 28, 36, 42, 50, 58, 65, 74, 80, 83, 89), levels)
        val byLevel = Implings.all.sortedBy { it.level }
        for ((easier, harder) in byLevel.zipWithNext()) {
            assert(harder.catchLow <= easier.catchLow) { "${harder.jar} should not be easier than ${easier.jar}" }
        }
    }
}
