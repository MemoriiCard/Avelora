package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.raids.cox.rimor.CoxCapes

@ResourceLock("server-cache")
class CoxCapesTest {
    @Test
    fun `capes unlock at the Challenge Mode completion thresholds`() {
        assertNull(CoxCapes.earned(99))
        assertEquals("obj.cox_challenge_cape_t1", CoxCapes.earned(100))
        assertEquals("obj.cox_challenge_cape_t2", CoxCapes.earned(500))
        assertEquals("obj.cox_challenge_cape_t3", CoxCapes.earned(1_000))
        assertEquals("obj.cox_challenge_cape_t4", CoxCapes.earned(1_500))
        assertEquals("obj.cox_challenge_cape_t5", CoxCapes.earned(5_000))
        assertEquals(100, CoxCapes.next(0)?.first)
        assertNull(CoxCapes.next(2_000))
    }

    @Test
    fun `rimor and the capes resolve`() {
        val cache = ServerCacheManager.init(240)
        try {
            "npc.raids_temple_captain".asRSCM(RSCMType.NPC)
            "varp.cox_level_scaling_off".asRSCM(RSCMType.VARP)
            for ((_, obj) in CoxCapes.TIERS) obj.asRSCM(RSCMType.OBJ)
        } finally {
            cache.close()
        }
    }
}
