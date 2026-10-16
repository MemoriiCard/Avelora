package org.rsmod.content.raids.toa.zebak

import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.map.CoordGrid

object ZebakRules {
    const val BASE_HP = 580
    const val STAT = 70
    const val SIZE = 9
    const val ATTACK_RATE = 7
    const val ENRAGED_RATE = 4
    const val ENRAGE_PERCENT = 25
    const val MELEE_MAX = 38
    const val RANGED_MAX = 16
    const val MAGIC_MAX = 16
    const val MELEE_REACH = 2
    const val ROAR_DELAY = 6
    const val ROAR_MAX = 60
    const val SAFE_RADIUS = 2
    const val SAFE_SPOTS = 3
    const val WAVES = 3
    const val WAVE_GAP_TICKS = 5
    const val WAVE_GAP_WIDTH = 4
    const val WAVE_MIN = 6
    const val WAVE_MAX = 10
    const val BLITZ_MAX = 18

    val SPECIAL_AT = listOf(85, 70, 55, 40)

    val BOSS = CoordGrid(3926, 5403, 0)
    val ARENA_X = 3924..3955
    val ARENA_Z = 5390..5426
    val SAFE_X = 3938..3953
    val SAFE_Z = 5395..5420
    val WAVE_X = 3936..3955
    val WAVE_Z = 5390..5426

    fun attackRate(percent: Int): Int = if (percent <= ENRAGE_PERCENT) ENRAGED_RATE else ATTACK_RATE

    fun nextSpecial(percent: Int, done: Int): Int? =
        SPECIAL_AT.indices.firstOrNull { it >= done && percent <= SPECIAL_AT[it] }

    fun isRoar(index: Int): Boolean = index % 2 == 0

    fun style(attackNumber: Int, adjacent: Boolean): ToaStyle =
        when {
            adjacent -> ToaStyle.Melee
            attackNumber % 2 == 0 -> ToaStyle.Ranged
            else -> ToaStyle.Magic
        }

    fun maxHit(style: ToaStyle): Int =
        when (style) {
            ToaStyle.Melee -> MELEE_MAX
            ToaStyle.Ranged -> RANGED_MAX
            ToaStyle.Magic -> MAGIC_MAX
        }

    fun safeRadius(upsetStomach: Boolean): Int = if (upsetStomach) SAFE_RADIUS - 1 else SAFE_RADIUS

    fun onWave(waveX: Int, playerX: Int): Boolean = waveX == playerX

    fun inGap(gapCenter: Int, playerZ: Int): Boolean =
        kotlin.math.abs(playerZ - gapCenter) <= WAVE_GAP_WIDTH / 2
}
