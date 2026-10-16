package org.rsmod.content.raids.tob.party

object TobScaling {
    const val MIN_PARTY = 1
    const val MAX_PARTY = 5

    fun teamHpPercent(size: Int): Int =
        when {
            size <= 3 -> 75
            size == 4 -> 88
            else -> 100
        }

    fun hitpoints(base: Int, size: Int, mode: TobMode): Int =
        maxOf(1, base * teamHpPercent(size) / 100 * mode.hpPercent / 100)

    fun damage(base: Int, mode: TobMode): Int = maxOf(1, base * mode.damagePercent / 100)
}
