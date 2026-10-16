package org.rsmod.content.raids.toa.baba

import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.map.CoordGrid

object BabaRules {
    const val BASE_HP = 380
    const val STAT = 80
    const val SIZE = 5
    const val ATTACK_RATE = 6
    const val MAX_HIT = 24
    const val SLAM_EVERY = 4
    const val SLAM_DELAY = 3
    const val SLAM_MAX = 32
    const val SLAM_ARM = 2
    const val ROCKFALL_EVERY = 5
    const val ROCKFALL_SPOTS = 3
    const val ROCKFALL_MAX = 36
    const val BOULDER_ROWS = 4
    const val BOULDER_GAP = 3
    const val BOULDER_MAX = 24
    const val BABOON_HP = 10
    const val BABOON_HIT = 6
    const val BABOON_RATE = 4
    val PHASES = listOf(66, 33)

    val BOSS = CoordGrid(3806, 5406, 0)
    val ARENA_X = 3800..3816
    val ARENA_Z = 5400..5416

    fun style(attackNumber: Int): ToaStyle = if (attackNumber % 2 == 0) ToaStyle.Melee else ToaStyle.Ranged

    fun isSlam(attackNumber: Int): Boolean = attackNumber > 0 && attackNumber % SLAM_EVERY == 0

    fun isRockfall(attackNumber: Int): Boolean = attackNumber > 0 && attackNumber % ROCKFALL_EVERY == 0

    fun nextPhase(percent: Int, done: Int): Int? = PHASES.indices.firstOrNull { it >= done && percent <= PHASES[it] }

    fun slamArm(shakingThingsUp: Boolean): Int = if (shakingThingsUp) SLAM_ARM + 1 else SLAM_ARM

    fun inCross(player: CoordGrid, centre: CoordGrid, arm: Int): Boolean {
        val dx = kotlin.math.abs(player.x - centre.x)
        val dz = kotlin.math.abs(player.z - centre.z)
        return (dx <= arm && dz <= 1) || (dz <= arm && dx <= 1) || (dx <= 1 && dz <= 1)
    }

    fun splitDamage(damage: Int, neighbours: Int): Int = damage / (1 + neighbours)

    fun baboonCount(teamSize: Int): Int = (teamSize / 2 + 1).coerceIn(1, 4)
}
