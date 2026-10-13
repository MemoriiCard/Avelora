package org.rsmod.content.bosses.royaltitans

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.bosses.validation.SpecValidator

@ResourceLock("server-cache")
class RoyalTitansSymbolsTest {
    @Test
    fun `specs pass the boss validator`() {
        for (titan in listOf(Titan.BRANDA, Titan.ELDRIC)) {
            assertEquals(emptyList<Any>(), SpecValidator.validate(RoyalTitanSpecs.build(titan)))
        }
    }

    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (titan in listOf(Titan.BRANDA, Titan.ELDRIC)) {
                for (npc in listOf(titan.npc, titan.elemental)) {
                    assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
                }
                for (seq in listOf(titan.meleeSeq, titan.summonSeq, titan.areaSeq)) {
                    seq.asRSCM(RSCMType.SEQ)
                }
                titan.wallSpotanim.asRSCM(RSCMType.SPOTANIM)
            }
            assertEquals(600, ServerCacheManager.getNpc("npc.rt_fire_queen".asRSCM(RSCMType.NPC))!!.hitpoints)
            for (loc in listOf("loc.royal_titans_entrance", "loc.royal_titans_exit")) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
            for (seq in listOf("seq.fire_minion_explode01", "seq.ice_minion_explode01")) {
                seq.asRSCM(RSCMType.SEQ)
            }
        } finally {
            cache.close()
        }
    }
}
