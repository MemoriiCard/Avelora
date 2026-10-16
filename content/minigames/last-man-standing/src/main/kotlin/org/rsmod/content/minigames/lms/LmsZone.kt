package org.rsmod.content.minigames.lms

import kotlin.math.max

object LmsZone {
    const val CENTER_X = 3502
    const val CENTER_Z = 5975
    const val DAMAGE_INTERVAL = 5

    private val STAGES = listOf(0 to 250, 300 to 150, 550 to 80, 750 to 40)

    fun radius(elapsed: Int): Int = STAGES.last { elapsed >= it.first }.second

    fun stage(elapsed: Int): Int = STAGES.indexOfLast { elapsed >= it.first }

    fun isOutside(x: Int, z: Int, elapsed: Int): Boolean {
        val dx = x - CENTER_X
        val dz = z - CENTER_Z
        val radius = radius(elapsed)
        return dx * dx + dz * dz > radius * radius
    }

    fun damage(elapsed: Int): Int = 1 + stage(elapsed) * 2 + max(0, elapsed - 750) / 100
}
