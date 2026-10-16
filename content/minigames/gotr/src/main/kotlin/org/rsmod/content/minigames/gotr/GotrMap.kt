package org.rsmod.content.minigames.gotr

import org.rsmod.map.CoordGrid

internal object GotrMap {
    val GAME = CoordGrid(3615, 9487, 0)
    val LOBBY_BARRIER_SIDE = CoordGrid(3615, 9481, 0)
    const val BARRIER_Z = 9483

    val GUARDIAN_SPAWN =
        mapOf(
            GotrKind.Elemental to CoordGrid(3620, 9493, 0),
            GotrKind.Catalytic to CoordGrid(3610, 9493, 0),
        )

    val CELL_TILES =
        listOf(
            CoordGrid(3609, 9507, 0),
            CoordGrid(3615, 9510, 0),
            CoordGrid(3611, 9509, 0),
            CoordGrid(3621, 9507, 0),
            CoordGrid(3619, 9509, 0),
            CoordGrid(3608, 9503, 0),
            CoordGrid(3622, 9503, 0),
        )

    private const val MIN_X = 3584
    private const val MAX_X = 3645
    private const val MIN_Z = 9472
    private const val MAX_Z = 9530

    fun inside(coords: CoordGrid): Boolean = coords.x in MIN_X..MAX_X && coords.z in MIN_Z..MAX_Z

    fun tileIndex(coords: CoordGrid): Int? = CELL_TILES.indexOfFirst { it == coords }.takeIf { it >= 0 }
}
