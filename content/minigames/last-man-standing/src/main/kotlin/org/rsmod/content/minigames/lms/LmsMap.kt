package org.rsmod.content.minigames.lms

import org.rsmod.map.CoordGrid

internal object LmsMap {
    val LOBBY = CoordGrid(3143, 3637, 0)

    val STARTS =
        listOf(
            CoordGrid(3412, 5809, 0),
            CoordGrid(3518, 5767, 0),
            CoordGrid(3489, 6119, 0),
            CoordGrid(3574, 6119, 0),
            CoordGrid(3495, 6069, 0),
            CoordGrid(3499, 5884, 0),
            CoordGrid(3545, 6059, 0),
        )

    private val ISLAND_X = 3392..3647
    private val ISLAND_Z = 5760..6175

    fun inArena(coords: CoordGrid): Boolean = coords.x in ISLAND_X && coords.z in ISLAND_Z
}
