package org.rsmod.content.bosses.scorpia

import org.rsmod.map.CoordGrid

internal enum class PitCave(val surface: CoordGrid, val cave: CoordGrid) {
    NorthWest(surface = CoordGrid(3232, 3950, 0), cave = CoordGrid(3232, 10351, 0)),
    NorthEast(surface = CoordGrid(3242, 3948, 0), cave = CoordGrid(3243, 10351, 0)),
    South(surface = CoordGrid(3232, 3938, 0), cave = CoordGrid(3233, 10332, 0)),
}

internal object ScorpionPit {
    const val MIN_X = 3220
    const val MAX_X = 3246
    const val MIN_Z = 10332
    const val MAX_Z = 10351
    val SCORPIA_SPAWN = CoordGrid(3233, 10341, 0)

    fun inCave(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in MIN_X..MAX_X && coords.z in MIN_Z..MAX_Z

    fun nearestCave(from: CoordGrid): PitCave = PitCave.entries.minBy { it.surface.chebyshevDistance(from) }

    fun nearestSurface(from: CoordGrid): PitCave = PitCave.entries.minBy { it.cave.chebyshevDistance(from) }
}
