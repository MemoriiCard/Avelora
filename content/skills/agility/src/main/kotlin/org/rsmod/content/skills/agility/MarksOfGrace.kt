package org.rsmod.content.skills.agility

/** Chance of a mark spawning on a lap, before the over-levelled penalty. */
data class MarkChance(val numerator: Int, val denominator: Int) {
    companion object {
        val STANDARD = MarkChance(1, 3)
        val HIGH = MarkChance(2, 3)
    }
}

object MarksOfGrace {
    const val OBJ = "obj.grace"
    const val COOLDOWN_MINUTES = 3
    const val DESPAWN_TICKS = 1000
    private const val OVER_LEVEL_THRESHOLD = 20

    fun isOffCooldown(nowMinute: Int, nextAllowedMinute: Int): Boolean = nowMinute >= nextAllowedMinute

    /**
     * Rolls a mark for a finished lap. Players 20 or more levels (unboosted) above the course get
     * one fifth of the normal chance; [roll] draws a uniform value in 0 until the given bound.
     */
    fun rolls(chance: MarkChance, courseLevel: Int, baseLevel: Int, roll: (Int) -> Int): Boolean {
        val overLevelled = baseLevel - courseLevel >= OVER_LEVEL_THRESHOLD
        val denominator = if (overLevelled) chance.denominator * 5 else chance.denominator
        return roll(denominator) < chance.numerator
    }
}
