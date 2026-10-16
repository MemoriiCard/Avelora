package org.rsmod.content.minigames.pestcontrol

import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal object PcMap {
    val OUTPOST = CoordGrid(2657, 2640, 0)
    val KNIGHT = CoordGrid(2656, 2592, 0)
    val SQUIRE = CoordGrid(2656, 2603, 0)
    val START = listOf(CoordGrid(2654, 2601, 0), CoordGrid(2656, 2601, 0), CoordGrid(2658, 2601, 0))

    val PORTALS =
        listOf(
            CoordGrid(2628, 2592, 0),
            CoordGrid(2681, 2589, 0),
            CoordGrid(2670, 2571, 0),
            CoordGrid(2646, 2570, 0),
        )

    val PEST_SPAWNS =
        listOf(
            CoordGrid(2632, 2592, 0),
            CoordGrid(2677, 2590, 0),
            CoordGrid(2668, 2574, 0),
            CoordGrid(2646, 2574, 0),
        )

    private const val SQUARE_X = 41
    private const val SQUARE_Z = 40
    private const val SQUARE = 64
    private const val ZONES = SQUARE / 8
    private const val ORIGIN_X = SQUARE_X * SQUARE
    private const val ORIGIN_Z = SQUARE_Z * SQUARE

    private val ARENA_X = 2625..2686
    private val ARENA_Z = 2564..2616

    fun inArena(coords: CoordGrid): Boolean = coords.x in ARENA_X && coords.z in ARENA_Z

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
