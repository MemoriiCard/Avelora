package org.rsmod.content.minigames.soulwars

import org.rsmod.map.CoordGrid

internal class AvatarShield {
    private val reduction = SwTeam.entries.associateWith { FULL }.toMutableMap()

    fun reduction(team: SwTeam): Double = reduction.getValue(team)

    fun vulnerable(team: SwTeam): Boolean = reduction(team) < FULL

    fun sacrifice(sacrificer: SwTeam, fragments: Int, controller: SwTeam?) {
        val share = if (controller == null || controller == sacrificer) 1.0 else OFF_CONTROL_SHARE
        val victim = sacrificer.other
        reduction[victim] = (reduction(victim) - fragments * FRAGMENT_POINTS * share).coerceAtLeast(0.0)
    }

    fun bury(team: SwTeam, bones: Int) {
        reduction[team] = (reduction(team) + bones * BONE_POINTS).coerceAtMost(FULL)
    }

    fun reset() = SwTeam.entries.forEach { reduction[it] = FULL }

    companion object {
        const val FULL = 100.0
        const val FRAGMENT_POINTS = 1.0
        const val BONE_POINTS = 1.0
        const val OFF_CONTROL_SHARE = 1.0 / 3.0
    }
}

internal object ObeliskControl {
    val CENTRE = CoordGrid(2207, 2912, 0)
    const val RADIUS = 10

    fun controller(positions: Map<SwTeam, Int>): SwTeam? {
        val blue = positions.getOrDefault(SwTeam.Blue, 0)
        val red = positions.getOrDefault(SwTeam.Red, 0)
        return when {
            blue > red -> SwTeam.Blue
            red > blue -> SwTeam.Red
            else -> null
        }
    }

    fun near(coords: CoordGrid): Boolean =
        coords.level == CENTRE.level &&
            kotlin.math.abs(coords.x - CENTRE.x) <= RADIUS &&
            kotlin.math.abs(coords.z - CENTRE.z) <= RADIUS
}
