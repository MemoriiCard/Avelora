package org.rsmod.content.minigames.barbassault

import org.rsmod.map.CoordGrid

internal object BaMap {
    val OUTPOST = CoordGrid(2534, 3573, 0)
    val ARENA_START = CoordGrid(1880, 5398, 0)
    val SPAWN_Z = 5412..5416
    val SPAWN_X = 1874..1894

    private val ARENA_X = 1862..1900
    private val ARENA_Z = 5384..5420

    fun inArena(coords: CoordGrid): Boolean = coords.x in ARENA_X && coords.z in ARENA_Z

    fun spawnTile(index: Int): CoordGrid {
        val width = SPAWN_X.last - SPAWN_X.first + 1
        val x = SPAWN_X.first + (index * 3) % width
        val z = SPAWN_Z.first + (index / 7) % (SPAWN_Z.last - SPAWN_Z.first + 1)
        return CoordGrid(x, z, 0)
    }
}
