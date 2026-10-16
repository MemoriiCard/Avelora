package org.rsmod.content.raids.toa.wardens

import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.map.CoordGrid

object WardensRules {
    const val OBELISK_HP = 260
    const val OBELISK_SIZE = 3
    const val OBELISK_STAT = 100
    const val BEAM_EVERY = 8
    const val BEAM_MAX = 18
    const val STRIKE_DELAY = 3
    const val STRIKE_RADIUS = 1

    const val EJECTIONS = 3
    const val ELIDINIS_HP = 140
    const val ELIDINIS_STAT = 190
    const val P2_ATTACK_RATE = 7
    const val P2_MAX_HIT = 20
    const val P2_LIGHTNING_EVERY = 4
    const val P2_LIGHTNING_MAX = 22
    const val CORE_BASE_TICKS = 8
    const val CORE_STEP_TICKS = 4
    const val REGROW_DELAY = 3

    const val P3_HP = 880
    const val P3_STAT = 150
    const val P3_ATTACK_RATE = 7
    const val P3_MAX_HIT = 26
    const val P3_SPECIAL_EVERY = 5
    const val P3_ENRAGED_SPECIAL_EVERY = 2
    const val ENRAGE_PERCENT = 5
    const val ENRAGE_HEAL_PERCENT = 20
    const val LIGHTNING_MAX = 26
    const val PHANTOM_HP = 30
    const val PHANTOM_HIT = 14
    const val PHANTOM_RATE = 4
    const val PHANTOM_LIFETIME = 25
    const val SIPHON_DRAIN = 12
    const val SIPHON_HEAL_PERCENT = 1

    val P1_OBELISK = CoordGrid(3807, 5153, 1)
    val P1_ELIDINIS = CoordGrid(3794, 5152, 1)
    val P1_TUMEKEN = CoordGrid(3820, 5152, 1)
    val P2_WARDEN = CoordGrid(3806, 5160, 1)
    val P3_WARDEN = CoordGrid(3933, 5163, 1)
    val P3_TUMEKEN = CoordGrid(3927, 5171, 1)
    val P3_PHANTOM_A = CoordGrid(3930, 5159, 1)
    val P3_PHANTOM_B = CoordGrid(3942, 5159, 1)

    val P1_ARENA_X = 3797..3819
    val P1_ARENA_Z = 5142..5166
    val P3_ARENA_X = 3931..3941
    val P3_ARENA_Z = 5157..5165

    enum class Special {
        Lightning,
        Phantoms,
        Siphon,
    }

    fun p2Style(attackNumber: Int): ToaStyle = if (attackNumber % 2 == 0) ToaStyle.Magic else ToaStyle.Ranged

    fun p3Style(attackNumber: Int): ToaStyle = ToaStyle.entries[attackNumber % ToaStyle.entries.size]

    fun isP2Lightning(attackNumber: Int): Boolean =
        attackNumber > 0 && attackNumber % P2_LIGHTNING_EVERY == 0

    fun coreWindow(ejection: Int): Int = CORE_BASE_TICKS + CORE_STEP_TICKS * ejection

    fun isP3Special(attackNumber: Int, enraged: Boolean): Boolean {
        val every = if (enraged) P3_ENRAGED_SPECIAL_EVERY else P3_SPECIAL_EVERY
        return attackNumber > 0 && attackNumber % every == 0
    }

    fun p3Special(specialNumber: Int, enraged: Boolean): Special =
        if (enraged) Special.Lightning else Special.entries[specialNumber % Special.entries.size]

    fun shouldEnrage(percent: Int, enraged: Boolean): Boolean = !enraged && percent <= ENRAGE_PERCENT

    fun enrageHeal(maxHp: Int): Int = maxHp * ENRAGE_HEAL_PERCENT / 100

    fun siphonHeal(maxHp: Int, players: Int): Int = maxHp * SIPHON_HEAL_PERCENT * players / 100

    fun inStrike(player: CoordGrid, centre: CoordGrid): Boolean =
        player.level == centre.level && player.chebyshevDistance(centre) <= STRIKE_RADIUS

    fun beamTargets(teamSize: Int): Int = (teamSize / 2 + 1).coerceIn(1, 4)
}
