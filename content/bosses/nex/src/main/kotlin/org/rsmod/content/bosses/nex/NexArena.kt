package org.rsmod.content.bosses.nex

import org.rsmod.map.CoordGrid

internal object NexArena {
    const val LEVEL = 0
    const val MIN_X = 2910
    const val MAX_X = 2940
    const val MIN_Z = 5189
    const val MAX_Z = 5217

    val SPAWN = CoordGrid(2924, 5202, LEVEL)
    val CENTRE = CoordGrid(2925, 5203, LEVEL)

    val BARRIER_OUTSIDE = CoordGrid(2908, 5203, LEVEL)
    val BARRIER_INSIDE = CoordGrid(2910, 5203, LEVEL)
    val BANK_ROOM = CoordGrid(2904, 5203, LEVEL)

    /** South-west tiles Nex can occupy at each end of the cross-shaped walkways. */
    val DASH_ENDS =
        listOf(
            DashEnd(CoordGrid(2924, 5214, LEVEL), CoordGrid(2924, 5190, LEVEL)),
            DashEnd(CoordGrid(2924, 5190, LEVEL), CoordGrid(2924, 5214, LEVEL)),
            DashEnd(CoordGrid(2936, 5202, LEVEL), CoordGrid(2910, 5202, LEVEL)),
            DashEnd(CoordGrid(2910, 5202, LEVEL), CoordGrid(2936, 5202, LEVEL)),
        )

    fun contains(tile: CoordGrid): Boolean =
        tile.level == LEVEL && tile.x in MIN_X..MAX_X && tile.z in MIN_Z..MAX_Z

    data class DashEnd(val start: CoordGrid, val end: CoordGrid) {
        fun path(size: Int): Set<CoordGrid> {
            val tiles = mutableSetOf<CoordGrid>()
            for (x in minOf(start.x, end.x) until maxOf(start.x, end.x) + size) {
                for (z in minOf(start.z, end.z) until maxOf(start.z, end.z) + size) {
                    tiles += CoordGrid(x, z, LEVEL)
                }
            }
            return tiles
        }
    }
}

internal enum class NexMage(
    val npc: String,
    val tile: CoordGrid,
    val projectile: String,
) {
    Fumus("npc.nex_smokemage", CoordGrid(2914, 5215, NexArena.LEVEL), "spotanim.nex_smoke_attack_proj"),
    Umbra("npc.nex_shadowmage", CoordGrid(2936, 5215, NexArena.LEVEL), "spotanim.nex_shadow_attack_proj"),
    Cruor("npc.nex_bloodmage", CoordGrid(2936, 5191, NexArena.LEVEL), "spotanim.nex_blood_attack_proj"),
    Glacies("npc.nex_icemage", CoordGrid(2914, 5191, NexArena.LEVEL), "spotanim.nex_ice_attack_proj");

    val introShout: String
        get() = "$name!"

    val turnShout: String
        get() = "$name, don't fail me!"
}

internal enum class NexPhase(val key: String, val shout: String, val mage: NexMage?, val mageAtPercent: Int) {
    Smoke(PHASE_SMOKE, "Fill my soul with smoke!", NexMage.Fumus, 80),
    Shadow(PHASE_SHADOW, "Darken my shadow!", NexMage.Umbra, 60),
    Blood(PHASE_BLOOD, "Flood my lungs with blood!", NexMage.Cruor, 40),
    Ice(PHASE_ICE, "Infuse me with the power of ice!", NexMage.Glacies, 20),
    Zaros(PHASE_ZAROS, "NOW, THE POWER OF ZAROS!", null, 0);

    val next: NexPhase?
        get() = entries.getOrNull(ordinal + 1)

    fun mageThreshold(maxHitpoints: Int): Int = maxHitpoints * mageAtPercent / 100
}
