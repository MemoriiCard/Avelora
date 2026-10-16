package org.rsmod.content.minigames.colosseum

import org.rsmod.map.CoordGrid

internal object ColosseumArena {
    const val REGION = 7216

    val LOBBY = CoordGrid(1808, 9506, 0)
    val START = CoordGrid(1812, 3106, 0)
    val MINIMUS = CoordGrid(1814, 3108, 0)
    val NORTH_GATE = CoordGrid(1824, 3121, 0)
    val SOUTH_GATE = CoordGrid(1824, 3091, 0)
    val SOL = CoordGrid(1822, 3104, 0)

    val SPAWN_POINTS =
        listOf(
            CoordGrid(1820, 3106, 0),
            CoordGrid(1834, 3106, 0),
            CoordGrid(1828, 3106, 0),
            CoordGrid(1820, 3112, 0),
            CoordGrid(1830, 3112, 0),
            CoordGrid(1820, 3100, 0),
            CoordGrid(1830, 3100, 0),
            CoordGrid(1824, 3118, 0),
            CoordGrid(1824, 3094, 0),
        )

    fun spawnPoint(wave: Int, index: Int): CoordGrid = SPAWN_POINTS[(wave * 3 + index) % SPAWN_POINTS.size]

    fun gate(index: Int): CoordGrid = if (index % 2 == 0) NORTH_GATE else SOUTH_GATE
}
