package org.rsmod.content.bosses.vetion

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec

internal const val PHASE_NORMAL = "normal"
internal const val PHASE_NORMAL_HOUNDS = "normal_hounds"
internal const val PHASE_ENRAGED = "enraged"
internal const val PHASE_ENRAGED_HOUNDS = "enraged_hounds"
internal const val ATTACK_RATE = 6
internal const val ENRAGED_ATTACK_RATE = 5
internal const val LIGHTNING = "lightning"
internal const val BASH = "shield_bash"
internal const val LIGHTNING_WEIGHT = 3
internal const val BASH_WEIGHT = 1

internal fun lightningHandler(lair: VetionLair) = "${lair.key}.lightning"

internal fun bashHandler(lair: VetionLair) = "${lair.key}.shield_bash"

internal fun vetionSpec(lair: VetionLair): BossSpec =
    boss(lair.form, lair.enragedForm) {
        stats(attackRate = ATTACK_RATE)
        val lightning = ability(LIGHTNING, external(lightningHandler(lair)))
        val bash = ability(BASH, external(bashHandler(lair)))
        val normal: PhaseBuilder.() -> Unit = {
            weightedSelectorRandom {
                +random(lightning, weight = LIGHTNING_WEIGHT)
                +random(bash, weight = BASH_WEIGHT, requires = WithinMeleeRange)
            }
        }
        phase(PHASE_NORMAL, attackRate = ATTACK_RATE, block = normal)
        phase(PHASE_NORMAL_HOUNDS, attackRate = ATTACK_RATE, block = normal)
        phase(PHASE_ENRAGED, transmog = lair.enragedForm, attackRate = ENRAGED_ATTACK_RATE, block = normal)
        phase(PHASE_ENRAGED_HOUNDS, attackRate = ENRAGED_ATTACK_RATE, block = normal)
    }
