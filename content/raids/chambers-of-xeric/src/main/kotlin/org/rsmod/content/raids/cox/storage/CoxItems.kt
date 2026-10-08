package org.rsmod.content.raids.cox.storage

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

/** Which items belong to the Chambers: they never leave the raid, and shared storage only takes them. */
object CoxItems {
    const val KINDLING = "obj.raids_wood"
    const val PLANK = "obj.raids_plank"

    private val RAID_ONLY_PREFIXES =
        listOf(
            "obj.raids_vial_",
            "obj.raids_seed_",
            "obj.raids_grimy_",
            "obj.raids_fish",
            "obj.raids_bat",
        )

    private val RAID_ONLY =
        setOf(
            KINDLING,
            PLANK,
            "obj.raids_tightrope_keystone",
            "obj.raids_thievingchest_grubs",
            "obj.raids_vespula_herb",
            "obj.raids_noxifer",
            "obj.raids_golpar",
            "obj.raids_buchuleaf",
            "obj.raids_stinkhorn_mushroom",
            "obj.raids_endarkened_juice",
            "obj.raids_cicely",
        )

    private val SHARED_TOOLS =
        setOf(
            "obj.rake",
            "obj.spade",
            "obj.dibber",
            "obj.fishing_rod",
            "obj.hunting_butterfly_net",
            "obj.hammer",
            "obj.bronze_axe",
            "obj.iron_axe",
            "obj.iron_pickaxe",
            "obj.tinderbox",
            "obj.lockpick",
        )

    fun isRaidOnly(obj: String): Boolean =
        obj in RAID_ONLY || RAID_ONLY_PREFIXES.any { obj.startsWith(it) }

    fun fitsShared(obj: String): Boolean =
        (isRaidOnly(obj) && obj != KINDLING) || obj in SHARED_TOOLS

    fun isStackable(obj: String): Boolean =
        ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.isStackable == true

    fun displayName(obj: String): String =
        ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj
}
