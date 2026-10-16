package org.rsmod.content.minigames.soulwars

import org.rsmod.map.CoordGrid

internal object SoulWarsMap {
    val HUB = CoordGrid(2208, 2856, 0)

    val GRAVEYARD =
        mapOf(
            SwTeam.Blue to CoordGrid(2139, 2905, 0),
            SwTeam.Red to CoordGrid(2275, 2919, 0),
        )

    val AVATAR =
        mapOf(
            SwTeam.Blue to CoordGrid(2130, 2905, 0),
            SwTeam.Red to CoordGrid(2282, 2919, 0),
        )

    private val ARENA_X = 2112..2303
    private val ARENA_Z = 2880..2943

    fun inArena(coords: CoordGrid): Boolean = coords.x in ARENA_X && coords.z in ARENA_Z
}
