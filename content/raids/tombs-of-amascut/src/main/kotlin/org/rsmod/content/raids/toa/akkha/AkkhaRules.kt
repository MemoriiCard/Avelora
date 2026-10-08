package org.rsmod.content.raids.toa.akkha

import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.map.CoordGrid

object AkkhaRules {
    const val BASE_HP = 400
    const val STAT = 80
    const val SIZE = 3
    const val ATTACK_RATE = 6
    const val MAX_HIT = 55
    const val ATTACKS_PER_STYLE = 7
    const val DETONATE_DELAY = 5
    const val DETONATE_MAX = 40
    const val DETONATE_REACH = 4
    const val BLAST_DELAY = 6
    const val BLAST_MAX = 45
    const val ORB_FUSE = 3
    const val ORB_MAX = 25
    const val ORB_SPECIAL_TICKS = 12
    const val ENRAGE_PERCENT = 20
    const val ENRAGE_ORB_EVERY = 5
    const val ENRAGE_ORB_MAX = 25
    const val STAY_VIGILANT_SWITCH = 4
    const val PRAYER_PIERCE_LEVEL = 300

    val BOSS = CoordGrid(3680, 5405, 0)
    val CENTER = CoordGrid(3681, 5406, 0)

    val CYCLE = listOf(ToaStyle.Melee, ToaStyle.Ranged, ToaStyle.Magic)

    enum class Special {
        Detonate,
        MemoryBlast,
        TrailingOrbs,
    }

    fun style(styleIndex: Int): ToaStyle = CYCLE[styleIndex % CYCLE.size]

    fun isSpecialTurn(attackNumber: Int): Boolean =
        attackNumber > 0 && attackNumber % ATTACKS_PER_STYLE == 0

    fun special(specialNumber: Int, teamSize: Int): Special {
        val pool = if (teamSize > 1) Special.entries else listOf(Special.MemoryBlast, Special.TrailingOrbs)
        return pool[specialNumber % pool.size]
    }

    fun quadrant(x: Int, z: Int, centerX: Int, centerZ: Int): Int =
        (if (x >= centerX) 1 else 0) + (if (z >= centerZ) 2 else 0)

    fun sharesLine(a: CoordGrid, b: CoordGrid): Boolean =
        a != b && (a.x == b.x || a.z == b.z) && a.chebyshevDistance(b) <= DETONATE_REACH

    fun shouldEnrage(percent: Int, enraged: Boolean): Boolean = !enraged && percent <= ENRAGE_PERCENT
}
