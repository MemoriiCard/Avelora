package org.rsmod.content.minigames.framework

class HubStock(val label: String, val obj: String, val quantity: Int, val cost: Int)

object HubStore {
    val SUPPLIES =
        listOf(
            HubStock("Stamina potion(4) x5", "obj.4dosestamina", 5, 30),
            HubStock("Prayer potion(4) x5", "obj.4doseprayerrestore", 5, 30),
            HubStock("Saradomin brew(4) x5", "obj.4dosepotionofsaradomin", 5, 40),
            HubStock("Super restore(4) x5", "obj.4dosestatrestore", 5, 40),
            HubStock("Anglerfish x15", "obj.anglerfish", 15, 25),
            HubStock("Minigame crate", HubCrates.CRATE, 1, 60),
        )

    val COSMETICS =
        listOf(
            HubStock("Armadyl halo", "obj.armadyl_halo", 1, 900),
            HubStock("Bandos halo", "obj.bandos_halo", 1, 900),
            HubStock("Seren halo", "obj.seren_halo", 1, 900),
            HubStock("Ancient halo", "obj.zaros_halo", 1, 900),
            HubStock("Brassica halo", "obj.brassica_halo", 1, 900),
            HubStock("Looting bag", "obj.looting_bag", 1, 150),
        )

    val ALL = SUPPLIES + COSMETICS
}
