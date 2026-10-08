package org.rsmod.content.raids.toa.layout

import org.rsmod.map.CoordGrid

enum class ToaRoom(
    val label: String,
    val squareX: Int,
    val squareZ: Int,
    val slotColumn: Int,
    val slotRow: Int,
    val plane: Int,
    val arrival: CoordGrid,
) {
    Nexus("The Nexus", 55, 80, 0, 0, 0, arrival = CoordGrid(3551, 5159, 0));

    private val originX: Int
        get() = squareX * SQUARE
    private val originZ: Int
        get() = squareZ * SQUARE

    fun instanceOffsetX(): Int = slotColumn * SQUARE

    fun instanceOffsetZ(): Int = slotRow * SQUARE

    fun toInstance(southWest: CoordGrid, source: CoordGrid): CoordGrid =
        CoordGrid(
            southWest.x + instanceOffsetX() + source.x - originX,
            southWest.z + instanceOffsetZ() + source.z - originZ,
            plane,
        )

    fun toSource(southWest: CoordGrid, instance: CoordGrid): CoordGrid =
        CoordGrid(
            instance.x - southWest.x - instanceOffsetX() + originX,
            instance.z - southWest.z - instanceOffsetZ() + originZ,
            0,
        )

    fun owns(southWest: CoordGrid, instance: CoordGrid): Boolean {
        val dx = instance.x - southWest.x - instanceOffsetX()
        val dz = instance.z - southWest.z - instanceOffsetZ()
        return instance.level == plane && dx in 0 until SQUARE && dz in 0 until SQUARE
    }

    val zoneSourceX: Int
        get() = squareX * ZONES

    val zoneSourceZ: Int
        get() = squareZ * ZONES

    companion object {
        const val SQUARE = 64
        const val ZONES = SQUARE / 8
        const val REGION_LENGTH = 128

        fun at(southWest: CoordGrid, instance: CoordGrid): ToaRoom? =
            entries.firstOrNull { it.owns(southWest, instance) }
    }
}
