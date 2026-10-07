package org.rsmod.content.bosses.vetion

import org.rsmod.map.CoordGrid

internal data class VetionLair(
    val key: String,
    val form: String,
    val enragedForm: String,
    val hound: String,
    val greaterHound: String,
    val lightningMaxHit: Int,
    val enragedLightningMaxHit: Int,
    val bashMaxHit: Int,
    val entrance: String,
    val surface: CoordGrid,
    val arrival: CoordGrid,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
) {
    fun contains(coords: CoordGrid): Boolean =
        coords.level == LEVEL && coords.x in minX..maxX && coords.z in minZ..maxZ

    companion object {
        const val LEVEL = 1
    }
}

internal val VETION_REST =
    VetionLair(
        key = "vetion",
        form = "npc.vetion",
        enragedForm = "npc.vetion_2",
        hound = "npc.vetion_hellhound_jnr",
        greaterHound = "npc.vetion_hellhound_snr",
        lightningMaxHit = 30,
        enragedLightningMaxHit = 44,
        bashMaxHit = 35,
        entrance = "loc.wild_vetion_entrance01",
        surface = CoordGrid(3220, 3785, 0),
        arrival = CoordGrid(3295, 10193, 1),
        minX = 3287,
        maxX = 3303,
        minZ = 10195,
        maxZ = 10209,
    )

internal val SKELETAL_TOMB =
    VetionLair(
        key = "calvarion",
        form = "npc.vetion_single",
        enragedForm = "npc.vetion_2_single",
        hound = "npc.vetion_hellhound_jnr_singles",
        greaterHound = "npc.vetion_hellhound_snr_singles",
        lightningMaxHit = 18,
        enragedLightningMaxHit = 26,
        bashMaxHit = 20,
        entrance = "loc.wild_vetion_singles_entrance01",
        surface = CoordGrid(3180, 3681, 0),
        arrival = CoordGrid(1887, 11537, 1),
        minX = 1879,
        maxX = 1895,
        minZ = 11539,
        maxZ = 11553,
    )

internal val VETION_LAIRS = listOf(VETION_REST, SKELETAL_TOMB)
