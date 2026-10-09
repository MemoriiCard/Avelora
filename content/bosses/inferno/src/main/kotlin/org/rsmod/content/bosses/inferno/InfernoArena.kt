package org.rsmod.content.bosses.inferno

import org.rsmod.map.CoordGrid

internal object InfernoArena {
    const val REGION = 9043

    val OUTSIDE = CoordGrid(2496, 5122, 0)
    val START = CoordGrid(2271, 5329, 0)

    private val NORTH_WEST = CoordGrid(2260, 5352, 0)
    private val NORTH = CoordGrid(2268, 5352, 0)
    private val NORTH_EAST = CoordGrid(2278, 5352, 0)
    private val WEST = CoordGrid(2260, 5342, 0)
    private val EAST = CoordGrid(2279, 5342, 0)
    private val CENTRE = CoordGrid(2269, 5342, 0)

    val SPAWN_POINTS = listOf(NORTH_WEST, NORTH, NORTH_EAST, WEST, EAST, CENTRE)

    fun spawnPoint(wave: Int, index: Int): CoordGrid = SPAWN_POINTS[(wave + index) % SPAWN_POINTS.size]
}
