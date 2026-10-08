package org.rsmod.content.raids.toa.kephri

import kotlin.math.min
import org.rsmod.map.CoordGrid

object KephriRules {
    const val BASE_HP = 150
    const val STAT = 80
    const val SIZE = 5
    const val ATTACK_RATE = 6
    const val ENRAGED_RATE = 4
    const val FIREBALL_MAX = 24
    const val FIREBALL_DELAY = 3
    const val DUNG_EVERY = 12
    const val DUNG_MAX = 14
    const val EGGS_EVERY = 20
    const val EGG_FUSE = 5
    const val EGG_MAX = 20
    const val EGGS = 2
    const val LIVELY_EGGS = 4
    const val SCARAB_HP = 10
    const val SCARAB_HIT = 6
    const val SCARAB_RATE = 4
    const val GUARDIAN_HP = 40
    const val GUARDIAN_HIT = 10
    const val GUARDIAN_RATE = 4

    val SHIELD_AT = listOf(100, 66)
    const val ENRAGE_PERCENT = 33

    val BOSS = CoordGrid(3549, 5406, 0)
    val ARENA_X = 3538..3564
    val ARENA_Z = 5395..5421
    val SCARAB_DOORS =
        listOf(
            CoordGrid(3543, 5400, 0),
            CoordGrid(3559, 5414, 0),
            CoordGrid(3543, 5416, 0),
            CoordGrid(3559, 5402, 0),
        )
    val GUARDIANS =
        listOf(
            "npc.toa_kephri_guardian_melee" to CoordGrid(3546, 5409, 0),
            "npc.toa_kephri_guardian_ranged" to CoordGrid(3551, 5412, 0),
            "npc.toa_kephri_guardian_mage" to CoordGrid(3555, 5409, 0),
        )

    fun shieldScarabs(teamSize: Int): Int = min(4 + 2 * teamSize, 20)

    fun nextShield(percent: Int, done: Int): Int? =
        SHIELD_AT.indices.firstOrNull { it >= done && percent <= SHIELD_AT[it] }

    fun attackRate(percent: Int): Int = if (percent <= ENRAGE_PERCENT) ENRAGED_RATE else ATTACK_RATE

    fun eggCount(livelyLarvae: Boolean): Int = if (livelyLarvae) LIVELY_EGGS else EGGS
}
