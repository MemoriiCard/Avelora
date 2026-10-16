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
            for (name in NPCS) name.asRSCM(RSCMType.NPC)
            for (name in SEQS) name.asRSCM(RSCMType.SEQ)
            for (name in SPOTANIMS) name.asRSCM(RSCMType.SPOTANIM)
        } finally {
            cache.close()
        }
    }

    private companion object {
        val NPCS =
            listOf("", "_story", "_hard").flatMap { suffix ->
                listOf("100", "70", "50", "30").map { "npc.tob_maiden_$it$suffix" } +
                    listOf(
                        "npc.maiden_elemental$suffix",
                        "npc.maiden_blood_slug$suffix",
                        "npc.tob_bloat$suffix",
                    )
            }

        val SEQS =
            listOf(
                "seq.maiden_spawn",
                "seq.maiden_attack_blood",
                "seq.maiden_attack_special",
                "seq.tob_bloat_walk",
                "seq.tob_bloat_sleep",
            )

        val SPOTANIMS =
            listOf(
                "spotanim.maiden_blood_proj",
                "spotanim.tob_bloat_blood_splat",
                "spotanim.tob_bloat_flies_large",
            )

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
