package org.rsmod.content.raids.tob.maiden

import kotlin.math.min
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling

object MaidenRules {
    const val ATTACK_RATE = 10
    const val FIRST_ATTACK = 6
    const val SPLAT_OFFSET = 5
    const val SPLAT_DELAY = 4
    const val TRAIL_TICKS = 12
    const val FREEZE_TICKS = 10
    const val MAX_BLOOD_SPAWNS = 8
    const val BLOOD_SPAWN_TICKS = 40
    const val BASE_MAX_HIT = 18
    const val LEAK_BONUS = 3
    const val SPLAT_MAX = 10
    const val TRAIL_MAX = 8
    const val MAX_CRABS = 10

    val THRESHOLDS = listOf(70, 50, 30)

    fun hpPercent(current: Int, max: Int): Int = if (max <= 0) 0 else current * 100 / max

    /** The index of the first threshold at or above [stage] that [percent] has fallen through. */
    fun nextStage(percent: Int, stage: Int): Int? =
        THRESHOLDS.indices.firstOrNull { it >= stage && percent <= THRESHOLDS[it] }

    fun crabCount(players: Int, mode: TobMode): Int =
        when (mode) {
            TobMode.Hard -> MAX_CRABS
            TobMode.Normal -> min(MAX_CRABS, 2 * players)
            TobMode.Entry -> min(MAX_CRABS, players)
        }

    fun maxHit(leaked: Int, mode: TobMode): Int =
        TobScaling.damage(BASE_MAX_HIT + LEAK_BONUS * leaked, mode)

    fun crabHeal(remainingHp: Int): Int = remainingHp * 2

    fun bloodSpawnChance(anyoneHit: Boolean): Int = if (anyoneHit) 40 else 20
}
