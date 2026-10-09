package org.rsmod.content.minigames.castlewars

import org.rsmod.map.CoordGrid

internal object CastleWarsMap {
    val LOBBY = CoordGrid(2440, 3089, 0)

    val WAITING_ROOM =
        mapOf(
            CwTeam.Saradomin to CoordGrid(2377, 9485, 0),
            CwTeam.Zamorak to CoordGrid(2421, 9524, 0),
        )

    val SPAWN =
        mapOf(
            CwTeam.Saradomin to CoordGrid(2426, 3076, 1),
            CwTeam.Zamorak to CoordGrid(2373, 3129, 1),
        )

    private val ARENA_X = 2368..2431
    private val ARENA_Z = 3072..3135
    private val UNDERGROUND_Z = 9472..9535

    fun inside(coords: CoordGrid): Boolean =
        coords.x in ARENA_X && (coords.z in ARENA_Z || coords.z in UNDERGROUND_Z)
}
