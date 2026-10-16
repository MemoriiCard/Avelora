package org.rsmod.content.bosses.grotesque

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition

internal object GrotesqueFight {
    val DAWN = arrayOf("npc.gargboss_dawn_phase1", "npc.gargboss_dawn_phase3")
    val DUSK = arrayOf("npc.gargboss_dusk_phase2_attacking", "npc.gargboss_dusk_phase4")

    const val ATTACK_RATE = 6
    const val DAWN_MELEE_MAX_HIT = 25
    const val DAWN_RANGED_MAX_HIT = 20
    const val DUSK_MELEE_MAX_HIT = 35
    const val DUSK_SWEEP_MAX_HIT = 30
    const val DUSK_SWEEP_EVERY_ATTACKS = 4

    fun dawnSpec(): BossSpec =
        boss(*DAWN) {
            stats(attackRate = ATTACK_RATE)

            val slash =
                ability("slash") {
                    anim("seq.gg_dawn_attack_slash")
                    hit {
                        damage(0..DAWN_MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val ranged =
                ability("ranged") {
                    anim("seq.gg_dawn_attack_ranged")
                    hit {
                        damage(0..DAWN_RANGED_MAX_HIT).roll()
                        type(Ranged)
                    }
                }

            phase("main") {
                weightedSelectorRandom {
                    +random(slash, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
            }
        }

    fun duskSpec(): BossSpec =
        boss(*DUSK) {
            stats(attackRate = ATTACK_RATE)

            val slash =
                ability("slash") {
                    anim("seq.gg_dusk_attack_slash")
                    hit {
                        damage(0..DUSK_MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val sweep =
                ability("sweep") {
                    anim("seq.gg_dusk_attack_sweep")
                    hit {
                        damage(0..DUSK_SWEEP_MAX_HIT).roll()
                        type(Melee)
                        target = AllInRadius(radius = 2)
                    }
                }

            phase("main") {
                weightedSelectorRandom { +random(slash, weight = 1) }
                forceEveryAttacks(DUSK_SWEEP_EVERY_ATTACKS, DUSK_SWEEP_EVERY_ATTACKS, sweep)
            }
        }
}

class DawnScript @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = GrotesqueFight.dawnSpec()
}

class DuskScript @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = GrotesqueFight.duskSpec()
}
