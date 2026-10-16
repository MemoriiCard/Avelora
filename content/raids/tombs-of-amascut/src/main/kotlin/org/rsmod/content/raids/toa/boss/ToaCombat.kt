package org.rsmod.content.raids.toa.boss

import org.rsmod.content.raids.toa.invocation.ToaInvocation

enum class ToaStyle {
    Melee,
    Ranged,
    Magic,
}

object ToaCombat {
    fun afterPrayer(damage: Int, protected: Boolean, active: Set<ToaInvocation>): Int =
        when {
            !protected -> damage
            ToaInvocation.QuietPrayers in active -> damage * 10 / 100
            else -> 0
        }

    fun prayerDrain(damageTaken: Int, active: Set<ToaInvocation>): Int =
        if (ToaInvocation.DeadlyPrayers in active) damageTaken * 20 / 100 else 0

    fun percent(current: Int, max: Int): Int = if (max <= 0) 0 else current * 100 / max
}
