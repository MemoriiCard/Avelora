package org.rsmod.content.raids.tob

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class TobGamevalTest {
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
                "loc.tob_arena_barrier",
                "loc.tob_dungeon_walkway_exit_clickbox",
                "loc.tob_dungeon_xarpus_arena_door_exit",
                "loc.tob_treasureroom_teleportout",
                "loc.tob_surface_notice_board",
                "loc.tob_surface_raid_entrance",
            )
    }
}
