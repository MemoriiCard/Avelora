package org.rsmod.content.bosses.nightmare

import org.rsmod.map.CoordGrid

internal object NightmareArena {
    const val LEVEL = 3
    const val MIN_X = 3863
    const val MAX_X = 3881
    const val MIN_Z = 9941
    const val MAX_Z = 9961
    const val CORE_MIN_Z = 9945
    const val CORE_MAX_Z = 9957

    val SPAWN = CoordGrid(3869, 9948, LEVEL)
    val LOBBY = CoordGrid(3808, 9755, 1)

    val ARRIVALS =
        listOf(
            CoordGrid(3871, 9944, LEVEL),
            CoordGrid(3868, 9944, LEVEL),
            CoordGrid(3874, 9944, LEVEL),
            CoordGrid(3871, 9956, LEVEL),
        )

    val SURGE_WEST = CoordGrid(MIN_X, 9948, LEVEL)
    val SURGE_EAST = CoordGrid(MAX_X - NIGHTMARE_SIZE + 1, 9948, LEVEL)

    fun contains(tile: CoordGrid): Boolean =
        tile.level == LEVEL && tile.x in MIN_X..MAX_X && tile.z in MIN_Z..MAX_Z

    fun surgePath(from: CoordGrid, to: CoordGrid): Set<CoordGrid> {
        val tiles = mutableSetOf<CoordGrid>()
        for (x in minOf(from.x, to.x) until maxOf(from.x, to.x) + NIGHTMARE_SIZE) {
            for (z in from.z until from.z + NIGHTMARE_SIZE) tiles += CoordGrid(x, z, LEVEL)
        }
        return tiles
    }

    fun edgeTiles(): List<CoordGrid> =
        (CORE_MIN_Z..CORE_MAX_Z).flatMap { z -> listOf(CoordGrid(MIN_X, z, LEVEL), CoordGrid(MAX_X, z, LEVEL)) }
}

internal const val NIGHTMARE_SIZE = 5

internal enum class NightmareTotem(val index: Int, val tile: CoordGrid) {
    SouthWest(1, CoordGrid(3863, 9942, NightmareArena.LEVEL)),
    SouthEast(2, CoordGrid(3879, 9942, NightmareArena.LEVEL)),
    NorthWest(3, CoordGrid(3863, 9958, NightmareArena.LEVEL)),
    NorthEast(4, CoordGrid(3879, 9958, NightmareArena.LEVEL));

    val dormant: String
        get() = "npc.nightmare_totem_${index}_dormant"

    val ready: String
        get() = "npc.nightmare_totem_${index}_ready"

    val charged: String
        get() = "npc.nightmare_totem_${index}_charged"
}

internal enum class NightmarePhase(val shielded: String, val weak: String, val entry: String) {
    One("npc.nightmare_phase_01", "npc.nightmare_weak_phase_01", "npc.nightmare_entry_closed_01"),
    Two("npc.nightmare_phase_02", "npc.nightmare_weak_phase_02", "npc.nightmare_entry_closed_02"),
    Three("npc.nightmare_phase_03", "npc.nightmare_weak_phase_03", "npc.nightmare_entry_closed_03");

    val key: String
        get() = "phase_${ordinal + 1}"

    val next: NightmarePhase?
        get() = entries.getOrNull(ordinal + 1)
}
