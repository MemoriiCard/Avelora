package org.rsmod.content.raids.toa

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class ToaGamevalTest {
    @Test
    fun `every symbol the raid uses resolves`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (name in LOCS) name.asRSCM(RSCMType.LOC)
        } finally {
            cache.close()
        }
    }

    private companion object {
        val LOCS =
            listOf(
                "loc.toa_invocation_board",
                "loc.toa_lobby_raid_entry",
                "loc.toa_lobby_exit",
                "loc.toa_nexus_crondis_door",
                "loc.toa_nexus_het_door",
                "loc.toa_nexus_scabaras_door",
                "loc.toa_nexus_apmeken_door",
                "loc.toa_nexus_wardens_door",
            )
    }
}
