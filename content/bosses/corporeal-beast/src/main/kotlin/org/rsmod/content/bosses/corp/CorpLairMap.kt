package org.rsmod.content.bosses.corp

import org.rsmod.map.CoordGrid

internal object CorpLairMap {
    const val IRONMAN_ROOM_OFFSET = 128
    const val IRONMAN_COMBAT = 90

    val CAVE_OUTSIDE = CoordGrid(3203, 3677, 0)
    val LOBBY = CoordGrid(2965, 4254, 2)

    const val PASSAGE_WEST_X = 2970
    const val PASSAGE_EAST_X = 2974
    const val PASSAGE_SOUTH_Z = 4254
    const val PASSAGE_NORTH_Z = 4258

    const val ROOM_MIN_X = 2974
    const val ROOM_MAX_X = 3005
    const val ROOM_MIN_Z = 4240
    const val ROOM_MAX_Z = 4270

    fun lobbyFor(ironmanRoom: Boolean): CoordGrid =
        if (ironmanRoom) LOBBY.translate(0, IRONMAN_ROOM_OFFSET) else LOBBY

    fun inRoom(tile: CoordGrid): Boolean {
        if (tile.level != LOBBY.level) return false
        val z = if (tile.z >= ROOM_MIN_Z + IRONMAN_ROOM_OFFSET) tile.z - IRONMAN_ROOM_OFFSET else tile.z
        return tile.x in ROOM_MIN_X..ROOM_MAX_X && z in ROOM_MIN_Z..ROOM_MAX_Z
    }

    fun throughPassage(from: CoordGrid): CoordGrid {
        val base = if (from.z >= PASSAGE_SOUTH_Z + IRONMAN_ROOM_OFFSET) IRONMAN_ROOM_OFFSET else 0
        val z = (from.z - base).coerceIn(PASSAGE_SOUTH_Z, PASSAGE_NORTH_Z) + base
        val x = if (from.x <= PASSAGE_WEST_X) PASSAGE_EAST_X else PASSAGE_WEST_X
        return CoordGrid(x, z, from.level)
    }
}
