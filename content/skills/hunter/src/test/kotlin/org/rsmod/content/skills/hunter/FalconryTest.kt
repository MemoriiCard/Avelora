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
class FalconryTest {
    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
    }

    @Test
    fun `every kebbit, fur, glove and falconer exists`() {
        for (kebbit in Falconry.kebbits) {
            assertNotNull(ServerCacheManager.getNpc(kebbit.npc.asRSCM(RSCMType.NPC)), kebbit.npc)
            assertNotNull(ServerCacheManager.getItem(kebbit.fur.asRSCM(RSCMType.OBJ)), kebbit.fur)
        }
        for (obj in listOf(Falconry.GLOVE, Falconry.COINS, Falconry.BONES)) {
            assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
        }
        for (npc in listOf("npc.hunting_npc_falconer", "npc.hunting_npc_falconer_nobird")) {
            assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
        }
    }

    @Test
    fun `levels and experience follow the wiki and harder kebbits are harder to catch`() {
        assertEquals(listOf(43, 57, 69), Falconry.kebbits.map { it.level })
        assertEquals(listOf(104.0, 132.0, 156.0), Falconry.kebbits.map { it.xp })
        for ((easier, harder) in Falconry.kebbits.zipWithNext()) {
            assertTrue(harder.catchLow < easier.catchLow && harder.catchHigh < easier.catchHigh)
        }
    }
}
