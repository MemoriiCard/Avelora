package org.rsmod.content.bosses.araxxor

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition

internal object AraxxorFight {
    const val BOSS = "npc.araxxor"

    const val PHASE_OPENING = "opening"
    const val PHASE_ENRAGED = "enraged"
    const val ENRAGE_HP = 0.25

    const val ATTACK_RATE = 4
    const val MELEE_MAX_HIT = 45
    const val ENRAGED_MELEE_MAX_HIT = 60
    const val RANGED_MAX_HIT = 40
    const val MAGIC_MAX_HIT = 40

    fun spec(): BossSpec =
        boss(BOSS) {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim("seq.npc_araxxor_01_attack_melee_01")
                    hit {
                        damage(0..MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val enragedMelee =
                ability("enraged_melee") {
                    anim("seq.npc_araxxor_01_attack_melee_enraged_01")
                    hit {
                        damage(0..ENRAGED_MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val ranged =
                ability("ranged") {
                    anim("seq.npc_araxxor_01_attack_ranged_01")
                    hit {
                        damage(0..RANGED_MAX_HIT).roll()
                        type(Ranged)
                    }
                }

            val magic =
                ability("magic") {
                    anim("seq.npc_araxxor_01_attack_magic_01")
                    hit {
                        damage(0..MAGIC_MAX_HIT).roll()
                        type(Magic)
                    }
                }

            val enrage =
                ability("enrage") {
                    anim("seq.npc_araxxor_01_enrage_transition_01")
                    wait(3)
                }

            phase(PHASE_OPENING) {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                    +random(magic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
            }

            phase(PHASE_ENRAGED, entryHp = ENRAGE_HP) {
                entry = enrage.name
                weightedSelectorRandom {
                    +random(enragedMelee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                    +random(magic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
            }
        }
}

class Araxxor @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = AraxxorFight.spec()
}
