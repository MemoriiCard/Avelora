package org.rsmod.content.bosses.fightcaves

import org.rsmod.map.CoordGrid

internal object FightCaveArena {
    const val REGION = 9551

    val OUTSIDE = CoordGrid(2438, 5168, 0)
    val START = CoordGrid(2413, 5117, 0)

    private val NORTH_WEST = CoordGrid(2376, 5106, 0)
    private val CENTRE = CoordGrid(2398, 5084, 0)
    private val SOUTH_EAST = CoordGrid(2416, 5078, 0)
    private val SOUTH_WEST = CoordGrid(2376, 5070, 0)
    private val SOUTH = CoordGrid(2398, 5066, 0)

    val SPAWN_POINTS = listOf(NORTH_WEST, CENTRE, SOUTH_EAST, SOUTH_WEST, SOUTH)

    val ROTATION =
        listOf(
            SOUTH_EAST, SOUTH_WEST, CENTRE, NORTH_WEST, SOUTH_WEST,
            SOUTH_EAST, SOUTH, NORTH_WEST, CENTRE, SOUTH_EAST,
            SOUTH_WEST, SOUTH, NORTH_WEST, CENTRE, SOUTH,
        )

    fun spawnPoints(wave: Int, rotationStart: Int, count: Int): List<CoordGrid> =
        List(count) { ROTATION[(rotationStart + wave - 1 + it) % ROTATION.size] }
}
