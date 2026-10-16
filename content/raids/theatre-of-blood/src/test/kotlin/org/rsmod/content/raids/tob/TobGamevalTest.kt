package org.rsmod.content.raids.tob

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.raids.tob.reward.TobLoot
import org.rsmod.content.raids.tob.reward.TobRewards

@ResourceLock("server-cache")
class TobGamevalTest {
    @Test
    fun `every symbol the raid uses resolves`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (name in LOCS) name.asRSCM(RSCMType.LOC)
            for (name in NPCS) name.asRSCM(RSCMType.NPC)
            val objs =
                TobLoot.UNIQUES.map { it.obj } + TobLoot.COMMONS.map { it.obj } +
                    TobLoot.SHROUDS.map { it.second } +
                    listOf(TobLoot.PET, TobLoot.HARD_KIT, TobLoot.HARD_KIT_BLOOD, TobLoot.HARD_DUST)
            for (name in objs) name.asRSCM(RSCMType.OBJ)
            for (name in listOf(TobRewards.CHEST, TobRewards.CHEST_RARE)) name.asRSCM(RSCMType.LOC)
            "varp.total_completed_theatreofblood".asRSCM(RSCMType.VARP)
            for (name in SEQS) name.asRSCM(RSCMType.SEQ)
            for (name in SPOTANIMS) name.asRSCM(RSCMType.SPOTANIM)
        } finally {
            cache.close()
        }
    }

    private companion object {
        val NYLO = listOf("melee", "ranged", "magic")

        val NPCS =
            listOf("", "_story", "_hard").flatMap { suffix ->
                listOf("100", "70", "50", "30").map { "npc.tob_maiden_$it$suffix" } +
                    NYLO.flatMap { style ->
                        listOf(
                            "npc.tob_nylocas_incoming_$style$suffix",
                            "npc.tob_nylocas_big_incoming_$style$suffix",
                            "npc.tob_nylocas_fighting_$style$suffix",
                            "npc.tob_nylocas_big_fighting_$style$suffix",
                            "npc.nylocas_boss_$style$suffix",
                        )
                    } +
                    (1..3).map { "npc.verzik_phase$it$suffix" } +
                    NYLO.map { "npc.verzik_nylocas_$it$suffix" } +
                    listOf(
                        "npc.verzik_pillar_npc",
                        "npc.verzik_story_pillar_npc",
                        "npc.verzik_hard_pillar_npc",
                        "npc.verzik_web_npc$suffix",
                        "npc.tob_verzik_phase2_armourednylocas$suffix",
                        "npc.tob_xarpus_combat$suffix",
                        "npc.nylocas_boss_spawning$suffix",
                        "npc.tob_sotetseg_combat$suffix",
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
                "seq.tob_sotetseg_attack_melee",
                "seq.tob_sotetseg_attack_ranged",
            )

        val SPOTANIMS =
            listOf(
                "spotanim.maiden_blood_proj",
                "spotanim.tob_bloat_blood_splat",
                "spotanim.tob_bloat_flies_large",
                "spotanim.tob_sotetseg_sharedattack",
                "spotanim.tob_sotetseg_sharedattack_impact",
                "spotanim.tob_sotetseg_zap",
                "spotanim.tob_xarpus_acidspit",
                "spotanim.tob_xarpus_acidsplash",
                "spotanim.tob_xarpus_exhumed_energyorb",
            )

        val LOCS =
            listOf(
                "loc.tob_arena_barrier",
                "loc.tob_dungeon_walkway_exit_clickbox",
                "loc.tob_dungeon_xarpus_arena_door_exit",
                "loc.tob_treasureroom_teleportout",
                "loc.tob_surface_notice_board",
                "loc.tob_surface_raid_entrance",
                "loc.tob_sotetseg_darkrealm_exit",
            )
    }
}
