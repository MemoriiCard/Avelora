package org.rsmod.content.minigames.barbassault

import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal object BaMap {
    val OUTPOST = CoordGrid(2534, 3573, 0)
    val ARENA_START = CoordGrid(1880, 5398, 0)
    val ESCAPE = CoordGrid(1882, 5393, 0)
    val QUEEN = CoordGrid(1880, 5410, 0)
    val SPAWN_Z = 5412..5416
    val SPAWN_X = 1874..1894

    private const val SQUARE_X = 29
    private const val SQUARE_Z = 84
    private const val SQUARE = 64
    private const val ZONES = SQUARE / 8
    private const val ORIGIN_X = SQUARE_X * SQUARE
    private const val ORIGIN_Z = SQUARE_Z * SQUARE

    private val ARENA_X = 1862..1900
    private val ARENA_Z = 5384..5420

    fun inArena(coords: CoordGrid): Boolean = coords.x in ARENA_X && coords.z in ARENA_Z

    fun spawnTile(index: Int): CoordGrid {
        val width = SPAWN_X.last - SPAWN_X.first + 1
        val x = SPAWN_X.first + (index * 3) % width
        val z = SPAWN_Z.first + (index / 7) % (SPAWN_Z.last - SPAWN_Z.first + 1)
        return CoordGrid(x, z, 0)
    }

    fun template(): RegionStaticTemplate =
        RegionTemplate.create {
            for (zx in 0 until ZONES) {
                for (zz in 0 until ZONES) {
                    this[zx, zz, 0] = ZoneKey(SQUARE_X * ZONES + zx, SQUARE_Z * ZONES + zz, 0)
                }
            }
        }

    fun toInstance(southWest: CoordGrid, source: CoordGrid): CoordGrid =
        CoordGrid(southWest.x + source.x - ORIGIN_X, southWest.z + source.z - ORIGIN_Z, 0)

    fun inInstance(southWest: CoordGrid, coords: CoordGrid): Boolean =
        coords.level == 0 &&
            coords.x - southWest.x in 0 until SQUARE &&
            coords.z - southWest.z in 0 until SQUARE
}
