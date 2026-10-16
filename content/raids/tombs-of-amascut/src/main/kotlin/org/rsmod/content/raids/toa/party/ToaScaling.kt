package org.rsmod.content.raids.toa.party

object ToaScaling {
    const val MIN_PARTY = 1
    const val MAX_PARTY = 8
    const val DAMAGE_CAP_PERCENT = 150
    const val FIRST_PATH_BONUS = 8
    const val PATH_STEP_BONUS = 5

    fun teamHpPercent(size: Int): Int {
        val n = size.coerceIn(MIN_PARTY, MAX_PARTY)
        return 100 + 90 * (minOf(n, 3) - 1) + 60 * maxOf(n - 3, 0)
    }

    fun levelBonusPercent(raidLevel: Int): Int = raidLevel / 5 * 2

    fun damageBonusPercent(raidLevel: Int): Int =
        levelBonusPercent(raidLevel).coerceAtMost(DAMAGE_CAP_PERCENT)

    fun pathBonusPercent(pathsCleared: Int): Int =
        if (pathsCleared <= 0) 0 else FIRST_PATH_BONUS + PATH_STEP_BONUS * (pathsCleared - 1)

    fun hitpoints(base: Int, size: Int, raidLevel: Int, pathsCleared: Int = 0): Int =
        maxOf(
            1,
            (base.toLong() * teamHpPercent(size) / 100 * (100 + levelBonusPercent(raidLevel)) / 100 *
                    (100 + pathBonusPercent(pathsCleared)) / 100)
                .toInt(),
        )

    fun damage(base: Int, raidLevel: Int, pathsCleared: Int = 0): Int =
        maxOf(1, base * (100 + damageBonusPercent(raidLevel)) / 100 * (100 + pathBonusPercent(pathsCleared)) / 100)

    fun accuracyPercent(raidLevel: Int): Int = 100 + levelBonusPercent(raidLevel)
}
