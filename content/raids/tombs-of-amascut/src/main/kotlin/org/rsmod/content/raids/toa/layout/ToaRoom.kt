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
    val level: Int = 0,
) {
    Nexus("The Nexus", 55, 80, 0, 0, 0, arrival = CoordGrid(3551, 5159, 0)),
    CrondisPuzzle("Crondis Puzzle", 61, 82, 1, 0, 0, arrival = CoordGrid(3952, 5279, 0)),
    Zebak("Zebak's Lair", 61, 84, 0, 1, 0, arrival = CoordGrid(3957, 5407, 0)),
    ScabarasPuzzle("Scabaras Puzzle", 55, 82, 1, 1, 0, arrival = CoordGrid(3525, 5279, 0)),
    Kephri("Kephri's Lair", 55, 84, 0, 0, 1, arrival = CoordGrid(3537, 5407, 0)),
    HetPuzzle("Het Puzzle", 57, 82, 1, 0, 1, arrival = CoordGrid(3696, 5278, 0)),
    Akkha("Akkha's Lair", 57, 84, 0, 1, 1, arrival = CoordGrid(3696, 5406, 1), level = 1),
    ApmekenPuzzle("Apmeken Puzzle", 59, 82, 1, 1, 1, arrival = CoordGrid(3794, 5279, 0)),
    Baba("Ba-Ba's Lair", 59, 84, 0, 0, 2, arrival = CoordGrid(3791, 5407, 0)),
    WardensOne("The Wardens' Chamber", 59, 80, 1, 0, 2, arrival = CoordGrid(3808, 5146, 1), level = 1),
    WardensTwo("The Wardens' Throne", 61, 80, 0, 1, 2, arrival = CoordGrid(3936, 5159, 1), level = 1),
    Vault("Osmumten's Burial Chamber", 57, 80, 1, 1, 2, arrival = CoordGrid(3679, 5167, 0));

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
            level,
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
