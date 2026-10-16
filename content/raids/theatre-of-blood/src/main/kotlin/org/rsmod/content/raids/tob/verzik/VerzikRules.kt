package org.rsmod.content.raids.tob.verzik

import kotlin.math.abs
import kotlin.math.roundToInt
import org.rsmod.map.CoordGrid

enum class VerzikSpecial {
    Nylocas,
    Webs,
    Ball,
}

object VerzikRules {
    const val PHASE1_HP = 2000
    const val PHASE2_HP = 3500
    const val PHASE3_HP = 3500

    const val P1_RATE = 14
    const val P1_MAX = 137
    const val PILLAR_SIZE = 3
    const val PILLAR_HITS = 2
    const val PILLAR_HIT_MIN = 70
    const val PILLAR_HIT_MAX = 100
    const val COLLAPSE_RADIUS = 3
    const val COLLAPSE_MAX = 70

    const val P2_RATE = 4
    const val P2_MAX = 30
    const val BALL_RATE = 16
    const val BALL_LINKS = 3
    const val LINK_MIN = 10
    const val LINK_MAX = 20
    const val NYLOCAS_RATE = 20
    const val ATHANATOS_RATE = 30
    const val ATHANATOS_LIFE = 15
    const val LOW_PERCENT = 35
    const val EXPLODE_MAX = 63
    const val MAX_NYLOCAS = 8

    const val P3_RATE = 7
    const val ENRAGED_RATE = 5
    const val ENRAGE_PERCENT = 20
    const val RANGED_MAX = 34
    const val MELEE_MAX = 63
    const val AUTOS_PER_SPECIAL = 4
    const val WEB_DELAY = 8
    const val WEB_MIN = 40
    const val WEB_MAX = 60
    const val GREEN_MIN = 30
    const val GREEN_MAX = 60

    val SPECIALS = listOf(VerzikSpecial.Nylocas, VerzikSpecial.Webs, VerzikSpecial.Ball)

    val PILLARS =
        listOf(
            CoordGrid(3158, 4308, 0),
            CoordGrid(3176, 4308, 0),
            CoordGrid(3158, 4314, 0),
            CoordGrid(3176, 4314, 0),
            CoordGrid(3167, 4311, 0),
        )

    val ARENA_X = 3154..3182
    val ARENA_Z = 4303..4322

    val THRONE = CoordGrid(3165, 4318, 0)
    val PHASE2_SPOT = CoordGrid(3166, 4312, 0)
    val PHASE3_SPOT = CoordGrid(3164, 4310, 0)

    fun hpPercent(current: Int, max: Int): Int = if (max <= 0) 0 else current * 100 / max

    fun protectedDamage(damage: Int, protectMagic: Boolean): Int =
        if (protectMagic) damage / 2 else damage

    fun covers(pillar: CoordGrid, tile: CoordGrid): Boolean =
        tile.x - pillar.x in 0 until PILLAR_SIZE && tile.z - pillar.z in 0 until PILLAR_SIZE

    /** True when the straight line from [from] to [to] passes through any of the [pillars]. */
    fun blocked(from: CoordGrid, to: CoordGrid, pillars: List<CoordGrid>): Boolean {
        val dx = to.x - from.x
        val dz = to.z - from.z
        val steps = maxOf(abs(dx), abs(dz))
        for (step in 1 until steps) {
            val x = from.x + (dx.toDouble() * step / steps).roundToInt()
            val z = from.z + (dz.toDouble() * step / steps).roundToInt()
            val tile = CoordGrid(x, z, from.level)
            if (pillars.any { covers(it, tile) }) return true
        }
        return false
    }

    fun special(autos: Int, specialsDone: Int): VerzikSpecial? =
        if (autos > 0 && autos % AUTOS_PER_SPECIAL == 0) SPECIALS[specialsDone % SPECIALS.size] else null

    fun phase3Rate(percent: Int): Int = if (percent <= ENRAGE_PERCENT) ENRAGED_RATE else P3_RATE

    fun explode(roll: Int): Int = roll.coerceIn(0, EXPLODE_MAX)

    fun pillarHit(roll: Int): Int = PILLAR_HIT_MIN + roll.coerceIn(0, PILLAR_HIT_MAX - PILLAR_HIT_MIN)
}
