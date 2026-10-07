package org.rsmod.content.bosses.smokedevil

import org.rsmod.map.CoordGrid

internal object SmokeDevilDungeon {
    const val AREA = "area.smoke_devil_dungeon"
    const val SLAYER_LEVEL = 93

    val SURFACE = CoordGrid(2412, 3060, 0)
    val DUNGEON = CoordGrid(2404, 9415, 0)
    val OUTSIDE_LAIR = CoordGrid(2379, 9452, 0)
    val INSIDE_LAIR = CoordGrid(2376, 9452, 0)
    val THERMY_SPAWN = CoordGrid(2360, 9452, 0)

    private const val LAIR_MIN_X = 2350
    private const val LAIR_MAX_X = 2377
    private const val LAIR_MIN_Z = 9438
    private const val LAIR_MAX_Z = 9460

    fun inLair(tile: CoordGrid): Boolean =
        tile.level == 0 && tile.x in LAIR_MIN_X..LAIR_MAX_X && tile.z in LAIR_MIN_Z..LAIR_MAX_Z
}
