package org.rsmod.content.raids.toa.party

object ToaScaling {
    const val MIN_PARTY = 1
    const val MAX_PARTY = 8
    const val DAMAGE_CAP_PERCENT = 150

    fun teamHpPercent(size: Int): Int {
        val n = size.coerceIn(MIN_PARTY, MAX_PARTY)
        return 100 + 90 * (minOf(n, 3) - 1) + 60 * maxOf(n - 3, 0)
    }

    fun levelBonusPercent(raidLevel: Int): Int = raidLevel / 5 * 2

    fun damageBonusPercent(raidLevel: Int): Int =
        levelBonusPercent(raidLevel).coerceAtMost(DAMAGE_CAP_PERCENT)

    fun hitpoints(base: Int, size: Int, raidLevel: Int): Int =
        maxOf(1, (base.toLong() * teamHpPercent(size) / 100 * (100 + levelBonusPercent(raidLevel)) / 100).toInt())

    fun damage(base: Int, raidLevel: Int): Int =
        maxOf(1, base * (100 + damageBonusPercent(raidLevel)) / 100)

    fun accuracyPercent(raidLevel: Int): Int = 100 + levelBonusPercent(raidLevel)
}
