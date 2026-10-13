package org.rsmod.content.minigames.foundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class FoundrySymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val items =
                Metal.entries.map { it.bar } +
                    listOf(
                        "obj.giants_foundry_preform",
                        "obj.giants_foundry_ore_pack",
                        "obj.giants_foundry_colossal_blade",
                        "obj.kovacs_grog",
                        "obj.smithing_catalyst",
                        "obj.smithing_uniform_torso",
                        "obj.smithing_uniform_legs",
                        "obj.smithing_uniform_boots",
                        "obj.smithing_uniform_gloves",
                        "obj.smithing_uniform_gloves_ice",
                        "obj.bucket_water",
                        "obj.bucket_empty",
                        "obj.coins",
                    )
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            val locs =
                listOf(
                    "loc.giants_foundry_entrance",
                    "loc.giants_foundry_exit",
                    "loc.giants_foundry_crucible_multi",
                    "loc.giants_foundry_mould_jig",
                    "loc.giants_foundry_lava_pool",
                    "loc.giants_foundry_waterfall",
                    "loc.giants_foundry_preform_storage",
                    "loc.giants_foundry_trip_hammer",
                    "loc.giants_foundry_grindstone",
                    "loc.giants_foundry_polishing_wheel",
                )
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            for (npc in listOf("giants_foundry_kovac_multi", "giants_foundry_kovac", "giants_foundry_kovac_1op")) {
                assertNotNull(ServerCacheManager.getNpc("npc.$npc".asRSCM(RSCMType.NPC)), npc)
            }
            for (tool in Tool.entries) tool.animation.asRSCM(RSCMType.SEQ)
            "varp.giants_foundry_reward_shop_points".asRSCM(RSCMType.VARP)
        } finally {
            cache.close()
        }
    }

    @Test
    fun `state varbits are wide enough for the values stored in them`() {
        val cache = ServerCacheManager.init(240)
        try {
            fun bits(name: String): Int {
                val type = ServerCacheManager.getVarbit("varbit.$name".asRSCM(RSCMType.VARBIT))!!
                return type.endBit - type.startBit + 1
            }
            for (name in listOf("preform_quality", "preform_start_quality", "preform_dificulty")) {
                assertTrue(bits("giants_foundry_$name") >= 8, name)
            }
            assertTrue(bits("giants_foundry_preform_temperature") >= 7)
            assertTrue(bits("giants_foundry_preform_completion") >= 7)
            for (metal in Metal.entries) {
                assertTrue(bits("giants_foundry_${metal.label}_count") >= 5, metal.label)
            }
            for (i in 0..6) assertTrue(bits("giants_foundry_seed_$i") >= 2, "seed $i")
            for (id in listOf("mould_selected_ricasso", "mould_selected_blade", "mould_selected_tip")) {
                assertTrue(bits("giants_foundry_$id") >= 4, id)
            }
            assertTrue(bits("giants_foundry_crucible_state") >= 2)
            assertTrue(bits("giants_foundry_mould_state") >= 2)
        } finally {
            cache.close()
        }
    }
}
