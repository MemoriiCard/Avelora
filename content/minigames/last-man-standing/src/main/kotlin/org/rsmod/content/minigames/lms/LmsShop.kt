package org.rsmod.content.minigames.lms

internal class LmsStock(
    val label: String,
    val obj: String,
    val quantity: Int,
    val cost: Int,
    val needsWin: Boolean,
)

internal object LmsShop {
    val SUPPLIES =
        listOf(
            LmsStock("Blighted entangle sack (x70)", "obj.blighted_sack_entangle", 70, 1, true),
            LmsStock("Blighted teleport spell sack (x50)", "obj.blighted_sack_teleblock", 50, 1, true),
            LmsStock("Blighted vengeance sack (x50)", "obj.blighted_sack_vengeance", 50, 1, true),
            LmsStock("Blighted ancient ice sack (x30)", "obj.blighted_sack_icebarrage", 30, 1, true),
            LmsStock("Blighted karambwan (x12)", "obj.blighted_karambwan", 12, 1, true),
            LmsStock("Blighted manta ray (x15)", "obj.blighted_mantaray", 15, 1, true),
            LmsStock("Blighted anglerfish (x15)", "obj.blighted_anglerfish", 15, 1, true),
            LmsStock("Blighted super restore(4) (x4)", "obj.blighted_4dose2restore", 4, 1, true),
        )

    val UPGRADES =
        listOf(
            LmsStock("Steam staff upgrade kit", "obj.steam_staff_upgrade_kit", 1, 13, true),
            LmsStock("Lava staff upgrade kit", "obj.lava_staff_upgrade_kit", 1, 13, true),
            LmsStock("Dragon pickaxe upgrade kit", "obj.dragon_pickaxe_upgrade_kit", 1, 14, true),
            LmsStock("Ward upgrade kit", "obj.ward_upgrade_kit", 1, 20, true),
            LmsStock("Granite clamp", "obj.granite_clamp", 1, 25, true),
            LmsStock("Trouver parchment", "obj.trouver_parchment", 1, 18, true),
        )

    val COSMETICS =
        listOf(
            LmsStock("Deadman's legs", "obj.deadman_legs", 1, 160, false),
            LmsStock("Deadman's cape", "obj.deadman_cape", 1, 160, false),
            LmsStock("Swift blade", "obj.swift_blade", 1, 350, true),
            LmsStock("Looting bag", "obj.looting_bag", 1, 1, false),
            LmsStock("Armadyl halo", "obj.armadyl_halo", 1, 450, false),
            LmsStock("Bandos halo", "obj.bandos_halo", 1, 450, false),
            LmsStock("Seren halo", "obj.seren_halo", 1, 450, false),
            LmsStock("Ancient halo", "obj.zaros_halo", 1, 450, false),
            LmsStock("Brassica halo", "obj.brassica_halo", 1, 450, false),
        )

    val ALL = SUPPLIES + UPGRADES + COSMETICS

    fun canBuy(stock: LmsStock, wins: Int): Boolean = !stock.needsWin || wins > 0
}
