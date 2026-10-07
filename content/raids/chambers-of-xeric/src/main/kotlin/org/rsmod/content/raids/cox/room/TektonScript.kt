package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.hitStyle
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.plugin.scripts.ScriptContext

class TektonScript @Inject constructor(deps: BossDeps, private val points: CoxDamagePoints) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
    }

    override val spec: BossSpec =
        boss(*TektonRoom.TYPES.toTypedArray()) {
            stats(attackRate = 3)

            val stab = ability("stab") { melee("seq.tekton_attack_stab", MeleeAttackType.Stab) }
            val slash = ability("slash") { melee("seq.tekton_slash", MeleeAttackType.Slash) }
            val crush =
                ability("crush") { melee("seq.tekton_hammer_crush", MeleeAttackType.Crush) }
            val stabEnraged =
                ability("stab_enraged") {
                    melee("seq.tekton_attack_stab_enraged", MeleeAttackType.Stab)
                }
            val slashEnraged =
                ability("slash_enraged") { melee("seq.tekton_slash_enraged", MeleeAttackType.Slash) }
            val crushEnraged =
                ability("crush_enraged") {
                    melee("seq.tekton_hammer_crush_enraged", MeleeAttackType.Crush)
                }

            phase("standard") {
                weightedSelectorRandom {
                    +random(stab, weight = 1)
                    +random(slash, weight = 1)
                    +random(crush, weight = 1)
                }
            }
            phase("enraged") {
                weightedSelectorRandom {
                    +random(stabEnraged, weight = 1)
                    +random(slashEnraged, weight = 1)
                    +random(crushEnraged, weight = 1)
                }
            }

            incoming {
                rule(hitStyle(Ranged)) { scalePercent(0) }
                rule(hitStyle(Magic)) { scalePercent(20) }
            }
        }
}
