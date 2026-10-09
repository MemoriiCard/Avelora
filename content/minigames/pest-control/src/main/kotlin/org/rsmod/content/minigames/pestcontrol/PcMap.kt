package org.rsmod.content.minigames.pestcontrol

import org.rsmod.map.CoordGrid

internal object PcMap {
    val OUTPOST = CoordGrid(2657, 2640, 0)
    val KNIGHT = CoordGrid(2656, 2592, 0)
    val START = listOf(CoordGrid(2654, 2601, 0), CoordGrid(2656, 2601, 0), CoordGrid(2658, 2601, 0))

    val PORTALS =
        listOf(
            CoordGrid(2628, 2592, 0),
            CoordGrid(2681, 2589, 0),
            CoordGrid(2670, 2571, 0),
            CoordGrid(2646, 2570, 0),
        )

    val PEST_SPAWNS =
        listOf(
            CoordGrid(2632, 2592, 0),
            CoordGrid(2677, 2590, 0),
            CoordGrid(2668, 2574, 0),
            CoordGrid(2646, 2574, 0),
        )

    private val ARENA_X = 2625..2686
    private val ARENA_Z = 2564..2616

    fun inArena(coords: CoordGrid): Boolean = coords.x in ARENA_X && coords.z in ARENA_Z
}
