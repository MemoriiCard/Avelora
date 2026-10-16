package org.rsmod.content.raids.cox.party

import kotlin.math.sqrt
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.baseMiningLvl

/** Raid NPC scaling, matching the formulas the OSRS wiki's DPS calculator uses. */
object CoxScaling {
    private const val MAX_COMBAT = 126

    fun scaledPartySize(party: CoxParty): Int = maxOf(party.size, party.scaling).coerceIn(1, 100)

    fun snapshot(party: CoxParty): Snapshot =
        Snapshot(
            partySize = scaledPartySize(party),
            highestCombat =
                if (party.members.any { it.coxLevelScalingOff }) MAX_COMBAT
                else party.members.maxOf { it.combatLevel },
            highestHitpoints = party.members.maxOf { it.baseHitpointsLvl },
            challengeMode = party.challengeMode,
            averageMining = party.members.map { it.baseMiningLvl }.average().toInt(),
        )

    data class Snapshot(
        val partySize: Int,
        val highestCombat: Int,
        val highestHitpoints: Int,
        val challengeMode: Boolean,
        val averageMining: Int,
    ) {
        private val n: Int
            get() = partySize.coerceIn(1, 100)

        private val m: Int
            get() = n - 1

        private val maxCombat: Int
            get() = highestCombat.coerceIn(60, 126)

        private val hpFactor: Int
            get() = (55 + 44 * highestHitpoints / 99).coerceIn(55, 99)

        fun offence(base: Int): Int {
            if (base <= 1) return base
            var value = base * hpFactor / 99
            value = value * (100 + 7 * isqrt(m) + m) / 100
            if (challengeMode) value = addPercent(value, 50)
            return value.coerceIn(50, 5000)
        }

        fun defence(base: Int, tekton: Boolean = false, crystal: Boolean = false): Int {
            if (base <= 1) return base
            var value = base * hpFactor / 99
            value = value * (100 + isqrt(m) + 7 * m / 10) / 100
            if (challengeMode && !crystal) {
                value = addPercent(value, if (tekton) (if (n < 4) 20 else 35) else 50)
            }
            return value.coerceIn(50, 20000)
        }

        fun hitpoints(base: Int, crystal: Boolean = false): Int {
            var value = base * maxCombat / 126
            value += value * (n * 50 / 100)
            if (challengeMode && !crystal) value = addPercent(value, 50)
            return value.coerceIn(50, 30000)
        }

        fun guardianHitpoints(): Int = hitpoints(151 + averageMining)

        fun singleHitpoints(base: Int): Int {
            var scale = maxCombat
            if (challengeMode) scale = addPercent(scale, 50)
            return maxOf(base * scale / 126, 5)
        }

        fun singleStat(base: Int): Int {
            var scale = highestHitpoints.coerceIn(55, 99)
            if (challengeMode) scale = addPercent(scale, 50)
            return maxOf(base * scale / 99, 1)
        }

        val olmPhases: Int
            get() = 4 + minOf(n, 50) / 8

        private val olmFactor: Int
            get() = minOf(n - 1, 50) - 3 * (minOf(n, 50) / 8)

        val olmHeadHitpoints: Int
            get() = 800 + 400 * olmFactor

        val olmHandHitpoints: Int
            get() = 600 + 300 * olmFactor

        private fun isqrt(value: Int): Int = sqrt(value.toDouble()).toInt()

        private fun addPercent(value: Int, percent: Int): Int = value + value * percent / 100
    }
}
