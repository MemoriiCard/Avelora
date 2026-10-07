package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.plugin.scripts.ScriptContext

class MysticsScript @Inject constructor(deps: BossDeps, private val points: CoxDamagePoints) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
    }

    override val spec: BossSpec =
        boss(*MysticsRoom.TYPES.toTypedArray()) {
            stats(attackRate = 4)
            val punchesPrayers = Condition.WithinMeleeRange and Condition.TargetPraying(HitType.Magic)
            val blast =
                ability("blast") {
                    missile(
                        "seq.skeleton_update_mage_casting",
                        "spotanim.fireblast_travel",
                        HitType.Magic,
                        impact = "spotanim.fireblast_impact",
                        penetration = PRAYER_PENETRATION,
                    )
                }
            val punch =
                ability("punch") {
                    melee("seq.skeleton_update_attack_weapon", MeleeAttackType.Crush)
                }
            phase("fight") {
                weightedSelectorRandom {
                    +random(blast, weight = 1, requires = Condition.Not(punchesPrayers))
                    +random(punch, weight = 1, requires = punchesPrayers)
                }
            }
        }

    private companion object {
        const val PRAYER_PENETRATION = 50
    }
}
