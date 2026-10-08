package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.raids.cox.room.PotionBrewingScript
import org.rsmod.content.raids.cox.room.PotionBrewingScript.Strength
import org.rsmod.content.raids.cox.room.ResourceRoom
import org.rsmod.content.raids.cox.storage.CoxItems
import org.rsmod.content.raids.cox.storage.CoxStorage

@ResourceLock("server-cache")
class CoxSupplyTest {
    @Test
    fun `every supply symbol resolves`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (herb in ResourceRoom.Herb.entries) {
                herb.seed.asRSCM(RSCMType.OBJ)
                herb.item.asRSCM(RSCMType.OBJ)
                for (stage in listOf("seed", "growth1", "growth2", "growth3", "fullygrown")) {
                    herb.loc(stage).asRSCM(RSCMType.LOC)
                }
            }
            for (recipe in PotionBrewingScript.RECIPES + PotionBrewingScript.OVERLOAD) {
                for (strength in Strength.entries) {
                    for (dose in 1..PotionBrewingScript.FULL) {
                        strength.item(recipe.key, dose).asRSCM(RSCMType.OBJ)
                    }
                }
            }
            for (tier in 0..6) {
                "obj.raids_fish${tier}_raw".asRSCM(RSCMType.OBJ)
                "obj.raids_fish${tier}_cooked".asRSCM(RSCMType.OBJ)
                "obj.raids_bat${tier}_raw".asRSCM(RSCMType.OBJ)
                "obj.raids_bat${tier}_cooked".asRSCM(RSCMType.OBJ)
                "npc.raids_bat_$tier".asRSCM(RSCMType.NPC)
            }
            for (unit in CoxStorage.UNITS) unit.asRSCM(RSCMType.LOC)
            for (name in SYMBOLS) name.asRSCM(RSCMType.valueOf(name.substringBefore('.').uppercase()))
        } finally {
            cache.close()
        }
    }

    @Test
    fun `food tier follows the party average level`() {
        assertEquals(0, ResourceRoom.tierFor(emptyList()))
        assertEquals(0, ResourceRoom.tierFor(listOf(1, 10)))
        assertEquals(3, ResourceRoom.tierFor(listOf(40, 50)))
        assertEquals(6, ResourceRoom.tierFor(listOf(99, 99)))
    }

    @Test
    fun `herb yield rises with party size up to fourteen`() {
        assertEquals(4, ResourceRoom.herbYield(1))
        assertEquals(11, ResourceRoom.herbYield(14))
        assertEquals(11, ResourceRoom.herbYield(100))
    }

    @Test
    fun `potion strength follows herblore level`() {
        val elder = PotionBrewingScript.RECIPES.first { it.key == "elder" }
        assertNull(elder.strengthFor(46))
        assertEquals(Strength.Weak, elder.strengthFor(47))
        assertEquals(Strength.Standard, elder.strengthFor(59))
        assertEquals(Strength.Strong, elder.strengthFor(99))
        assertEquals("obj.raids_vial_elder_weak_4", Strength.Weak.item("elder", 4))
        assertEquals("obj.raids_vial_elder_4", Strength.Standard.item("elder", 4))
    }

    @Test
    fun `shared storage obeys its tier capacity`() {
        val storage = CoxStorage()
        assertEquals(0, storage.donate("obj.raids_golpar", 5))
        storage.upgrade(1)
        assertEquals(250, storage.donate("obj.raids_golpar", 400))
        assertEquals(0, storage.donate("obj.raids_cicely", 1))
        assertEquals(100, storage.takeShared("obj.raids_golpar", 100))
        storage.upgrade(4)
        assertEquals(1500, storage.sharedCapacity)
        assertEquals(120, storage.privateCapacity)
    }

    @Test
    fun `raid only items are recognised`() {
        assertTrue(CoxItems.isRaidOnly("obj.raids_vial_overload_4"))
        assertTrue(CoxItems.isRaidOnly("obj.raids_fish3_cooked"))
        assertTrue(CoxItems.isRaidOnly("obj.raids_plank"))
        assertFalse(CoxItems.isRaidOnly("obj.raids_tekton_book"))
        assertFalse(CoxItems.isRaidOnly("obj.coins"))
        assertTrue(CoxItems.fitsShared("obj.rake"))
        assertFalse(CoxItems.fitsShared(CoxItems.KINDLING))
    }

    private companion object {
        val SYMBOLS =
            listOf(
                "obj.rake",
                "obj.spade",
                "obj.dibber",
                "obj.fishing_rod",
                "obj.hunting_butterfly_net",
                "obj.dragon_warhammer",
                "obj.elder_maul",
                "obj.hammer",
                "obj.raids_fishingbait",
                "obj.raids_vial_empty",
                "obj.raids_vial_water",
                "obj.raids_plank",
                "obj.raids_stinkhorn_mushroom",
                "obj.raids_cicely",
                "obj.raids_endarkened_juice",
                "obj.raids_noxifer",
                "obj.raids_golpar",
                "obj.raids_buchuleaf",
                "loc.raids_farming_tools",
                "loc.raids_weeds",
                "loc.raids_gourd_tree",
                "loc.raids_geyser",
                "loc.raids_patch_empty",
                "loc.raids_floor_water_edge1_fishing",
                "loc.raids_storage_lobby",
                "loc.raids_corridor_boulder",
                "loc.raids_corridor_rocks",
                "loc.raids_corridor_rocks_cleared",
                "loc.raids_corridor_roots",
                "loc.raids_corridor_roots_cleared",
                "npc.raids_fishing_snake",
                "seq.farming_raking",
                "seq.farming_seed_dibbing",
                "seq.picking_mid",
                "seq.farming_pour_water",
                "seq.human_fishing_casting",
                "seq.human_catch",
                "seq.human_push",
                "seq.human_hammer_hit",
                "content.mining_pickaxe",
                "stat.farming",
                "stat.herblore",
                "stat.fishing",
                "stat.hunter",
                "stat.construction",
            )
    }
}
