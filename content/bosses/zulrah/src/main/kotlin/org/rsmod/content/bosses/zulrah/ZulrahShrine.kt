package org.rsmod.content.bosses.zulrah

import org.rsmod.map.CoordGrid

internal object ZulrahShrine {
    val REGIONS = listOf(9007, 9008)

    val DOCK = CoordGrid(2213, 3056, 0)
    val PLAYER_START = CoordGrid(2268, 3069, 0)
    val EXIT_SCROLL = CoordGrid(2262, 3070, 0)

    private val POSITIONS =
        mapOf(
            ZulrahPosition.Middle to CoordGrid(2266, 3072, 0),
            ZulrahPosition.South to CoordGrid(2266, 3062, 0),
            ZulrahPosition.East to CoordGrid(2276, 3071, 0),
            ZulrahPosition.West to CoordGrid(2256, 3071, 0),
        )

    val OPENING_CLOUDS =
        listOf(
            CoordGrid(2263, 3068, 0),
            CoordGrid(2266, 3068, 0),
            CoordGrid(2269, 3068, 0),
            CoordGrid(2271, 3068, 0),
            CoordGrid(2262, 3071, 0),
            CoordGrid(2272, 3071, 0),
            CoordGrid(2262, 3074, 0),
            CoordGrid(2272, 3074, 0),
        )

    val SNAKELING_SPOTS =
        listOf(
            CoordGrid(2263, 3076, 0),
            CoordGrid(2273, 3076, 0),
            CoordGrid(2263, 3072, 0),
            CoordGrid(2273, 3072, 0),
            CoordGrid(2264, 3069, 0),
            CoordGrid(2272, 3069, 0),
        )

    fun position(position: ZulrahPosition): CoordGrid = POSITIONS.getValue(position)
}
