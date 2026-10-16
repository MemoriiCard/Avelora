package org.rsmod.content.bosses.cerberus

import org.rsmod.map.CoordGrid

internal object CerberusLair {
    const val SLAYER_LEVEL = 91

    val TAVERLEY_RETURN = CoordGrid(2873, 9848, 0)
    val HUB = CoordGrid(1310, 1237, 0)

    val LANDING = CoordGrid(1240, 1226, 0)
    val SPAWN = CoordGrid(1238, 1250, 0)
    val SOUL_TILES = listOf(CoordGrid(1239, 1256, 0), CoordGrid(1240, 1256, 0), CoordGrid(1241, 1256, 0))

    const val MIN_X = 1231
    const val MAX_X = 1249
    const val MIN_Z = 1226
    const val MAX_Z = 1256
}

internal enum class CerberusArena(private val dx: Int, private val dz: Int, val winch: CoordGrid, val hubReturn: CoordGrid) {
    West(0, 0, CoordGrid(1291, 1254, 0), CoordGrid(1292, 1253, 0)),
    North(64, 64, CoordGrid(1307, 1269, 0), CoordGrid(1309, 1269, 0)),
    East(128, 0, CoordGrid(1328, 1254, 0), CoordGrid(1328, 1253, 0));

    val landing: CoordGrid
        get() = shift(CerberusLair.LANDING)

    val spawn: CoordGrid
        get() = shift(CerberusLair.SPAWN)

    val soulTiles: List<CoordGrid>
        get() = CerberusLair.SOUL_TILES.map(::shift)

    fun contains(tile: CoordGrid): Boolean =
        tile.level == 0 &&
            tile.x - dx in CerberusLair.MIN_X..CerberusLair.MAX_X &&
            tile.z - dz in CerberusLair.MIN_Z..CerberusLair.MAX_Z

    fun shift(tile: CoordGrid): CoordGrid = tile.translate(dx, dz)

    companion object {
        fun byWinch(tile: CoordGrid): CerberusArena? = entries.firstOrNull { it.winch == tile }

        fun containing(tile: CoordGrid): CerberusArena? = entries.firstOrNull { it.contains(tile) }
    }
}
