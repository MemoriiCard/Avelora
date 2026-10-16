package org.rsmod.content.raids.cox.olm

import kotlin.math.abs
import kotlin.math.max

enum class OlmSide {
    West,
    East;

    val opposite: OlmSide
        get() = if (this == West) East else West
}

enum class OlmZone {
    Left,
    Middle,
    Right,
}

enum class OlmPower(val attacks: List<OlmAttack>) {
    Acid(listOf(OlmAttack.AcidSpray, OlmAttack.AcidDrip)),
    Flame(listOf(OlmAttack.Burn, OlmAttack.FireWall)),
    Crystal(listOf(OlmAttack.FallingCrystals, OlmAttack.CrystalBombs)),
}

enum class OlmAttack {
    AcidSpray,
    AcidDrip,
    Burn,
    FireWall,
    FallingCrystals,
    CrystalBombs,
}

enum class OlmStep {
    Standard,
    Empty,
    CrystalBurst,
    Lightning,
    Swap,
}

enum class OlmSphere(val prayer: String) {
    Melee("varbit.prayer_protectfrommelee"),
    Ranged("varbit.prayer_protectfrommissiles"),
    Magic("varbit.prayer_protectfrommagic"),
}

data class OlmPos(val x: Int, val z: Int)

/** The numbers and decisions behind the Great Olm that don't need a running world. */
object OlmRules {
    val ROTATION: List<OlmStep> =
        listOf(
            OlmStep.Standard,
            OlmStep.Empty,
            OlmStep.Standard,
            OlmStep.CrystalBurst,
            OlmStep.Standard,
            OlmStep.Empty,
            OlmStep.Standard,
            OlmStep.Lightning,
            OlmStep.Standard,
            OlmStep.Empty,
            OlmStep.Standard,
            OlmStep.Swap,
        )

    const val ATTACK_RATE = 4
    const val CLENCH_WINDOW = 8
    const val CLENCH_TICKS = 50
    const val REGROW_TICKS = 50
    const val POWER_CHANCE = 3
    const val FINAL_POWER_CHANCE = 2
    const val SPHERE_CHANCE = 6
    const val PRAYER_LEAK_PERCENT = 22
    const val PROTECTED_BOMB_COUNT_SMALL = 2
    const val BOMB_COUNT = 3

    val WEST = Layout(OlmPos(3220, 5738), OlmPos(3220, 5743), OlmPos(3220, 5733))
    val EAST = Layout(OlmPos(3238, 5738), OlmPos(3238, 5733), OlmPos(3238, 5743))

    class Layout(val head: OlmPos, val leftHand: OlmPos, val rightHand: OlmPos)

    fun layout(side: OlmSide): Layout = if (side == OlmSide.West) WEST else EAST

    fun maxHit(phase: Int, phaseCount: Int, challengeMode: Boolean): Int {
        val index =
            when {
                phase >= phaseCount - 1 -> 2
                phase == phaseCount - 2 -> 1
                else -> 0
            }
        return (if (challengeMode) CM_MAX_HITS else MAX_HITS)[index]
    }

    private val MAX_HITS = listOf(27, 28, 29)
    private val CM_MAX_HITS = listOf(38, 39, 41)

    /** The powers each phase uses: one per early phase, every power in the last two. */
    fun powersByPhase(phaseCount: Int, below: (Int) -> Int): List<Set<OlmPower>> {
        val bag = mutableListOf<OlmPower>()
        return List(phaseCount) { phase ->
            if (phase >= phaseCount - 2) {
                OlmPower.entries.toSet()
            } else {
                if (bag.isEmpty()) bag += OlmPower.entries
                setOf(bag.removeAt(below(bag.size)))
            }
        }
    }

    fun availableAttacks(powers: Set<OlmPower>, used: Set<OlmAttack>): List<OlmAttack> =
        powers.flatMap { it.attacks }.filter { it !in used }

    fun hasSpecials(phase: Int, phaseCount: Int): Boolean = phase < phaseCount - 1

    /** Which third of the arena [z] falls in, as seen from a head standing on [side]. */
    fun zoneOf(z: Int, side: OlmSide): OlmZone {
        val northish =
            when {
                z > HEAD_Z + 2 -> OlmZone.Left
                z < HEAD_Z - 2 -> OlmZone.Right
                else -> OlmZone.Middle
            }
        if (side == OlmSide.West || northish == OlmZone.Middle) return northish
        return if (northish == OlmZone.Left) OlmZone.Right else OlmZone.Left
    }

    fun busiestZone(counts: Map<OlmZone, Int>, current: OlmZone): OlmZone? {
        val best = counts.maxOfOrNull { it.value } ?: return null
        if (best <= 0) return null
        if ((counts[current] ?: 0) == best) return current
        return counts.entries.first { it.value == best }.key
    }

    fun swapDamage(distance: Int): Int = (distance * 5).coerceIn(0, 50)

    fun bombDamage(distance: Int): Int = if (distance > BOMB_RADIUS) 0 else minOf(60, 15 * (BOMB_RADIUS + 1 - distance))

    const val BOMB_RADIUS = 4

    fun clenches(damageInWindow: Int, baseHitpoints: Int): Boolean =
        damageInWindow >= max(1, baseHitpoints / 20)

    fun siphonHeal(damageDealt: Int): Int = damageDealt * 5

    fun crystalDamage(distance: Int, below: (Int, Int) -> Int): Int =
        when {
            distance == 0 -> below(16, 25)
            distance == 1 -> below(10, 16)
            else -> 0
        }

    fun distance(a: OlmPos, b: OlmPos): Int = max(abs(a.x - b.x), abs(a.z - b.z))

    fun <T> pairUp(players: List<T>, below: (Int) -> Int): List<Pair<T, T>> {
        val pool = players.toMutableList()
        val pairs = mutableListOf<Pair<T, T>>()
        while (pool.size >= 2 && pairs.size < MAX_SWAP_PAIRS) {
            val first = pool.removeAt(below(pool.size))
            val second = pool.removeAt(below(pool.size))
            pairs += first to second
        }
        return pairs
    }

    const val MAX_SWAP_PAIRS = 3
    const val HEAD_Z = 5738

    fun bombCount(partySize: Int): Int = if (partySize < 3) PROTECTED_BOMB_COUNT_SMALL else BOMB_COUNT
}
