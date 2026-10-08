package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.plugin.scripts.ScriptContext

class ScavengersScript @Inject constructor(deps: BossDeps, private val points: CoxDamagePoints) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
    }

    override val spec: BossSpec =
        boss(*ScavengersRoom.TYPES.toTypedArray()) {
            stats(attackRate = 4)
            val bite = ability("bite") { melee("seq.skavid_attack", MeleeAttackType.Crush) }
            phase("fight") { weightedSelectorRandom { +random(bite, weight = 1) } }
        }
}
