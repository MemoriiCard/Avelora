package org.rsmod.content.raids.tob.layout

import org.rsmod.map.CoordGrid

enum class TobRoom(
    val label: String,
    val squareX: Int,
    val squareZ: Int,
    val level: Int,
    val slotColumn: Int,
    val slotRow: Int,
    val plane: Int,
    val arrival: CoordGrid,
    val arena: CoordGrid,
    val exit: CoordGrid?,
    val inSequence: Boolean = true,
) {
    Maiden(
        "The Maiden of Sugadinti",
        49, 69, 0, 0, 0, 0,
        arrival = CoordGrid(3190, 4446, 0),
        arena = CoordGrid(3175, 4446, 0),
        exit = CoordGrid(3176, 4424, 0),
    ),
    Bloat(
        "The Pestilent Bloat",
        51, 69, 0, 1, 0, 0,
        arrival = CoordGrid(3320, 4447, 0),
        arena = CoordGrid(3296, 4447, 0),
        exit = CoordGrid(3270, 4447, 0),
    ),
    Nylocas(
        "The Nylocas",
        51, 66, 0, 0, 1, 0,
        arrival = CoordGrid(3295, 4282, 0),
        arena = CoordGrid(3295, 4250, 0),
        exit = CoordGrid(3302, 4275, 0),
    ),
    Sotetseg(
        "Sotetseg",
        51, 67, 0, 1, 1, 0,
        arrival = CoordGrid(3279, 4296, 0),
        arena = CoordGrid(3279, 4310, 0),
        exit = CoordGrid(3279, 4294, 0),
    ),
    Xarpus(
        "Xarpus",
        49, 68, 1, 0, 0, 1,
        arrival = CoordGrid(3170, 4376, 1),
        arena = CoordGrid(3170, 4386, 1),
        exit = CoordGrid(3170, 4399, 1),
    ),
    Verzik(
        "Verzik Vitur",
        49, 67, 0, 1, 0, 2,
        arrival = CoordGrid(3168, 4304, 0),
        arena = CoordGrid(3168, 4316, 0),
        exit = null,
    ),
    Treasure(
        "Treasure room",
        50, 67, 0, 0, 0, 3,
        arrival = CoordGrid(3237, 4312, 0),
        arena = CoordGrid(3237, 4312, 0),
        exit = null,
    ),
    Maze(
        "The Shadow Realm",
        52, 67, 3, 1, 0, 1,
        arrival = CoordGrid(3356, 4311, 3),
        arena = CoordGrid(3360, 4320, 3),
        exit = null,
        inSequence = false,
    );

    val next: TobRoom?
        get() = SEQUENCE.getOrNull(SEQUENCE.indexOf(this) + 1)

    val isFight: Boolean
        get() = this != Treasure && inSequence

    private val originX: Int
        get() = squareX * SQUARE
    private val originZ: Int
        get() = squareZ * SQUARE

    fun contains(source: CoordGrid): Boolean =
        source.x - originX in 0 until SQUARE && source.z - originZ in 0 until SQUARE

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
        val SEQUENCE: List<TobRoom> by lazy { entries.filter { it.inSequence } }

        fun at(southWest: CoordGrid, instance: CoordGrid): TobRoom? =
            entries.firstOrNull { it.owns(southWest, instance) }
    }
}
