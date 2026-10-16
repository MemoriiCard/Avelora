package org.rsmod.content.minigames.wintertodt

import org.rsmod.map.CoordGrid

internal object WintertodtMap {
    val ARENA_ENTRY = CoordGrid(1630, 3972, 0)
    val LOBBY_EXIT = CoordGrid(1630, 3961, 0)

    val BRAZIERS =
        listOf(
            CoordGrid(1620, 3997, 0),
            CoordGrid(1638, 3997, 0),
            CoordGrid(1620, 4015, 0),
            CoordGrid(1638, 4015, 0),
        )

    val PYROMANCERS =
        listOf(
            CoordGrid(1622, 3995, 0),
            CoordGrid(1636, 3995, 0),
            CoordGrid(1622, 4013, 0),
            CoordGrid(1636, 4013, 0),
        )

    private const val ARENA_MIN_X = 1608
    private const val ARENA_MAX_X = 1652
    private const val ARENA_MIN_Z = 3965
    private const val ARENA_MAX_Z = 4030

    fun inside(coords: CoordGrid): Boolean =
        coords.level == 0 &&
            coords.x in ARENA_MIN_X..ARENA_MAX_X &&
            coords.z in ARENA_MIN_Z..ARENA_MAX_Z

    fun brazierAt(coords: CoordGrid): Int? =
        BRAZIERS.indices.firstOrNull { BRAZIERS[it].chebyshevDistance(coords) <= 2 }
}
