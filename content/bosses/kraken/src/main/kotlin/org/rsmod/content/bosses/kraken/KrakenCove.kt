package org.rsmod.content.bosses.kraken

import org.rsmod.map.CoordGrid

internal object KrakenCove {
    const val REGION = 9116
    const val SLAYER_LEVEL = 87
    const val PRIVATE_FEE = 25_000

    val SURFACE = CoordGrid(2278, 3610, 0)
    val COVE = CoordGrid(2276, 9988, 0)
    val OUTSIDE_LAIR = CoordGrid(2280, 10016, 0)
    val INSIDE_LAIR = CoordGrid(2280, 10022, 0)

    val BOSS_WHIRLPOOL = CoordGrid(2278, 10034, 0)
    val TENTACLE_WHIRLPOOLS =
        listOf(
            CoordGrid(2275, 10034, 0),
            CoordGrid(2275, 10038, 0),
            CoordGrid(2284, 10034, 0),
            CoordGrid(2284, 10038, 0),
        )

    private const val LAIR_MIN_X = 2264
    private const val LAIR_MAX_X = 2296
    private const val LAIR_MIN_Z = 10018
    private const val LAIR_MAX_Z = 10046

    fun inLair(tile: CoordGrid): Boolean =
        tile.level == 0 && tile.x in LAIR_MIN_X..LAIR_MAX_X && tile.z in LAIR_MIN_Z..LAIR_MAX_Z
}
