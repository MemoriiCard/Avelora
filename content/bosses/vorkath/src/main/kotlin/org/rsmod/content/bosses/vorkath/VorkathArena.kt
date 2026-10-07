package org.rsmod.content.bosses.vorkath

import org.rsmod.map.CoordGrid

internal object VorkathArena {
    const val REGION = 9023
    const val VORKATH_SIZE = 7

    val UNGAEL_DOCK = CoordGrid(2277, 4034, 0)
    val RELLEKKA_DOCK = CoordGrid(2641, 3698, 0)
    val OUTSIDE = CoordGrid(2272, 4052, 0)
    val INSIDE = CoordGrid(2272, 4054, 0)
    val LAIR = CoordGrid(2269, 4062, 0)
    val DROP_TILE = CoordGrid(2272, 4061, 0)

    private const val MIN_X = 2261
    private const val MAX_X = 2283
    private const val MIN_Z = 4055
    private const val MAX_Z = 4075

    val TILES: List<CoordGrid> =
        buildList {
            for (x in MIN_X..MAX_X) for (z in MIN_Z..MAX_Z) {
                val tile = CoordGrid(x, z, 0)
                if (!inLair(tile)) add(tile)
            }
        }

    fun inLair(tile: CoordGrid): Boolean =
        tile.x - LAIR.x in 0 until VORKATH_SIZE && tile.z - LAIR.z in 0 until VORKATH_SIZE
}
