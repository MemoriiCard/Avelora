package org.rsmod.content.bosses.giantmole

import org.rsmod.map.CoordGrid

internal object MoleHole {
    val LANDING = CoordGrid(1752, 5237, 0)
    val EXIT = CoordGrid(2985, 3316, 0)

    val MOLE_HILLS =
        listOf(
            CoordGrid(2984, 3387, 0),
            CoordGrid(2987, 3387, 0),
            CoordGrid(3005, 3376, 0),
            CoordGrid(2996, 3377, 0),
            CoordGrid(2989, 3378, 0),
            CoordGrid(2999, 3375, 0),
        )

    val BURROW_SPOTS =
        listOf(
            CoordGrid(1759, 5189, 0),
            CoordGrid(1735, 5226, 0),
            CoordGrid(1775, 5238, 0),
            CoordGrid(1736, 5207, 0),
            CoordGrid(1777, 5206, 0),
            CoordGrid(1750, 5222, 0),
            CoordGrid(1783, 5195, 0),
            CoordGrid(1772, 5164, 0),
            CoordGrid(1747, 5150, 0),
            CoordGrid(1780, 5152, 0),
        )

    private const val MIN_X = 1728
    private const val MAX_X = 1791
    private const val MIN_Z = 5120
    private const val MAX_Z = 5247

    fun contains(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in MIN_X..MAX_X && coords.z in MIN_Z..MAX_Z
}
