package org.rsmod.content.raids.tob.bloat

import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.map.CoordGrid

object BloatRules {
    const val SIZE = 5
    const val FLY_RATE = 3
    const val FLY_MAX = 8
    const val FLY_RANGE = 8
    const val FLESH_RATE = 2
    const val FLESH_DELAY = 2
    const val FLESH_FROM_PERCENT = 90
    const val LEG_STOMP_MIN = 40
    const val LEG_STOMP_MAX = 80
    const val FLESH_MIN = 30
    const val FLESH_MAX = 50

    val ARENA_X = 3288..3303
    val ARENA_Z = 4441..4454

    val RING =
        listOf(
            CoordGrid(3288, 4440, 0),
            CoordGrid(3299, 4440, 0),
            CoordGrid(3299, 4451, 0),
            CoordGrid(3288, 4451, 0),
        )

    fun walkTicks(roll: Int): Int = 39 + roll.coerceIn(0, 8)

    fun stopTicks(roll: Int): Int = 34 + roll.coerceIn(0, 8)

    /** Damage multiplier, in percent, applied to hits on the Bloat. */
    fun incomingPercent(walking: Boolean): Int = if (walking) 50 else 100

    fun stomp(roll: Int, mode: TobMode): Int =
        TobScaling.damage(LEG_STOMP_MIN + roll.coerceIn(0, LEG_STOMP_MAX - LEG_STOMP_MIN), mode)

    fun flesh(roll: Int, mode: TobMode): Int =
        TobScaling.damage(FLESH_MIN + roll.coerceIn(0, FLESH_MAX - FLESH_MIN), mode)

    fun fleshActive(percent: Int, walking: Boolean, mode: TobMode): Boolean =
        if (mode == TobMode.Hard) true else walking && percent <= FLESH_FROM_PERCENT

    fun nextCorner(index: Int): Int = (index + 1) % RING.size

    fun covers(origin: CoordGrid, tile: CoordGrid): Boolean =
        tile.x - origin.x in 0 until SIZE && tile.z - origin.z in 0 until SIZE
}
