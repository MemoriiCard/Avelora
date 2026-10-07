package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class CoxRoomGamevalTest {
    @Test
    fun `every symbol the combat rooms use resolves`() {
        val cache = ServerCacheManager.init(240)
        try {
            for ((type, names) in SYMBOLS) {
                for (name in names) name.asRSCM(type)
            }
        } finally {
            cache.close()
        }
    }

    private companion object {
        val SYMBOLS =
            mapOf(
                RSCMType.CONTENT to
                    listOf(
                        "content.woodcutting_axe",
                    ),
                RSCMType.LOC to
                    listOf(
                        "loc.raids_meat_tree_full",
                        "loc.raids_vespula_herb",
                        "loc.raids_vespula_herb_empty",
                        "loc.raids_vespula_portal",
                    ),
                RSCMType.NPC to
                    listOf(
                        "npc.raids_dogodile",
                        "npc.raids_dogodile_junior",
                        "npc.raids_dogodile_meat_tree",
                        "npc.raids_dogodile_submerged",
                        "npc.raids_lizardshaman_a",
                        "npc.raids_lizardshaman_b",
                        "npc.raids_skeletonmystic_a",
                        "npc.raids_skeletonmystic_b",
                        "npc.raids_skeletonmystic_c",
                        "npc.raids_stoneguardians_left",
                        "npc.raids_stoneguardians_left_dead",
                        "npc.raids_stoneguardians_right",
                        "npc.raids_stoneguardians_right_dead",
                        "npc.raids_tekton_fighting_enraged",
                        "npc.raids_tekton_fighting_standard",
                        "npc.raids_tekton_hammering",
                        "npc.raids_tekton_waiting",
                        "npc.raids_tekton_walking_enraged",
                        "npc.raids_tekton_walking_standard",
                        "npc.raids_vanguard_dormant",
                        "npc.raids_vanguard_magic",
                        "npc.raids_vanguard_melee",
                        "npc.raids_vanguard_ranged",
                        "npc.raids_vasanistirio_crystal",
                        "npc.raids_vasanistirio_dormant",
                        "npc.raids_vasanistirio_healing",
                        "npc.raids_vasanistirio_walking",
                        "npc.raids_vespula_caterpillar_dead",
                        "npc.raids_vespula_caterpillar_healthy",
                        "npc.raids_vespula_caterpillar_infected",
                        "npc.raids_vespula_caterpillar_sickly",
                        "npc.raids_vespula_enraged",
                        "npc.raids_vespula_flying",
                        "npc.raids_vespula_portal",
                        "npc.raids_vespula_vespine_flying",
                        "npc.raids_vespula_walking",
                        "npc.zeah_lizardshaman_spawn",
                    ),
                RSCMType.OBJ to
                    listOf(
                        "obj.raids_vespula_herb",
                    ),
                RSCMType.PROJANIM to
                    listOf(
                        "projanim.arrow",
                        "projanim.magic_spell",
                    ),
                RSCMType.SEQ to
                    listOf(
                        "seq.dohgadyle_bark",
                        "seq.dohgadyle_bite",
                        "seq.dohgadyle_death",
                        "seq.dohgadyle_defend",
                        "seq.dohgadyle_emerge",
                        "seq.dohgadyle_lazor",
                        "seq.dohgadyle_slam",
                        "seq.human_pickupfloor",
                        "seq.human_staff_block",
                        "seq.human_staff_pound",
                        "seq.human_staff_pummel",
                        "seq.human_woodcutting_bronze_axe",
                        "seq.luxgrub_death",
                        "seq.raids_vespular_portal_closing",
                        "seq.shay_lizard_warrior_attack_melee",
                        "seq.shay_lizard_warrior_attack_ranged",
                        "seq.shay_lizard_warrior_death",
                        "seq.shay_lizard_warrior_defend",
                        "seq.shayzien_lizard_boss_jump",
                        "seq.shayzien_lizard_boss_land",
                        "seq.shayzien_lizard_boss_minion_summon",
                        "seq.skeleton_update_attack_weapon",
                        "seq.skeleton_update_death",
                        "seq.skeleton_update_defend",
                        "seq.skeleton_update_mage_casting",
                        "seq.tekton_attack_stab",
                        "seq.tekton_attack_stab_enraged",
                        "seq.tekton_death",
                        "seq.tekton_hammer_crush",
                        "seq.tekton_hammer_crush_enraged",
                        "seq.tekton_ready_defend",
                        "seq.tekton_ready_defend_enraged",
                        "seq.tekton_slash",
                        "seq.tekton_slash_enraged",
                        "seq.vanguard_attack_magic",
                        "seq.vanguard_attack_melee",
                        "seq.vanguard_attack_ranged",
                        "seq.vanguard_death",
                        "seq.vanguard_defend_magic",
                        "seq.vanguard_defend_melee",
                        "seq.vanguard_defend_ranged",
                        "seq.vanguard_heal",
                        "seq.vanguard_spawn",
                        "seq.vasa_attack",
                        "seq.vasa_death",
                        "seq.vasa_healed",
                        "seq.vasa_spawn",
                        "seq.vasa_stun_spawn",
                        "seq.vespine_explode",
                        "seq.vespine_hatch",
                        "seq.vespula_attack_melee_flying",
                        "seq.vespula_attack_ranged",
                        "seq.vespula_attack_ranged_flying",
                        "seq.vespula_death_flying",
                        "seq.vespula_death_walking",
                        "seq.vespula_defend",
                        "seq.vespula_defend_flying",
                        "seq.vespula_landing",
                        "seq.vespula_takeoff",
                    ),
                RSCMType.SPOTANIM to
                    listOf(
                        "spotanim.fireblast_impact",
                        "spotanim.fireblast_travel",
                        "spotanim.lizardman_spit",
                        "spotanim.lizardshaman_acid_splash",
                        "spotanim.lizardshaman_spawn_explode",
                        "spotanim.lizardshaman_spit_acid",
                        "spotanim.raids_vanguard_magic",
                        "spotanim.raids_vanguard_range_0",
                        "spotanim.raids_vasanistirio_magic_impact",
                        "spotanim.raids_vasanistirio_magic_travel",
                        "spotanim.raids_vasanistirio_range_impact",
                        "spotanim.raids_vasanistirio_range_travel",
                        "spotanim.raids_vespula_poison",
                        "spotanim.raids_vespula_vespine_explode",
                        "spotanim.raids_vespula_vespine_hatch",
                        "spotanim.raids_vespula_vespine_healportal",
                        "spotanim.rockfall",
                        "spotanim.wild_falloff_meteor_blast",
                        "spotanim.wild_falloff_meteor_flying",
                    ),
                RSCMType.STAT to
                    listOf(
                        "stat.prayer",
                    ),
                RSCMType.VARBIT to
                    listOf(
                        "varbit.prayer_protectfrommagic",
                    ),
            )
    }
}
