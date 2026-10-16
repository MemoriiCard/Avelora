package org.rsmod.content.raids.tob.xarpus

import kotlin.math.abs
import org.rsmod.map.CoordGrid

enum class Quadrant {
    North,
    East,
    South,
    West,
}

object XarpusRules {
    const val BASE_HP = 5000
    const val START_PERCENT = 75
    const val COUNTER_PERCENT = 25
    const val REMAINS = 10
    const val REMAINS_RATE = 4
    const val REMAINS_TICKS = 6
    const val POISON_RATE = 4
    const val AUTO_MAX = 11
    const val UNDER_MAX = 4
    const val QUADRANT_TICKS = 8
    const val SIZE = 5

    val ARENA_X = 3164..3176
    val ARENA_Z = 4381..4392
    val SPAWN = CoordGrid(3168, 4384, 1)
    val CENTRE = CoordGrid(3170, 4386, 1)

    fun healEach(maxHp: Int): Int = maxHp * (100 - START_PERCENT) / 100 / REMAINS

    fun hpPercent(current: Int, max: Int): Int = if (max <= 0) 0 else current * 100 / max

    fun acidDamage(roll: Int, stacks: Int): Int = 4 + roll.coerceIn(0, 4) + stacks / 2

    fun counterDamage(roll: Int, stacks: Int): Int =
        (50 + roll.coerceIn(0, 25)) * (100 + stacks * 2 * 40 / 100) / 100

    fun quadrantOf(centre: CoordGrid, at: CoordGrid): Quadrant {
        val dx = at.x - centre.x
        val dz = at.z - centre.z
        return if (abs(dx) >= abs(dz)) {
            if (dx >= 0) Quadrant.East else Quadrant.West
        } else {
            if (dz >= 0) Quadrant.North else Quadrant.South
        }
    }

    fun acidTiles(centre: CoordGrid): List<CoordGrid> =
        (-1..1).flatMap { dx -> (-1..1).map { dz -> centre.translate(dx, dz) } }

    fun under(origin: CoordGrid, tile: CoordGrid): Boolean =
        tile.x - origin.x in 0 until SIZE && tile.z - origin.z in 0 until SIZE
}
