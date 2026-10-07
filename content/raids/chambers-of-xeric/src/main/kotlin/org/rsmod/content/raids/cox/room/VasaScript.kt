package org.rsmod.content.raids.cox.room

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.plugin.scripts.ScriptContext

class VasaScript @Inject constructor(deps: BossDeps, private val points: CoxDamagePoints) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
        val crystal =
            ServerCacheManager.getNpc(VasaRoom.CRYSTAL.asRSCM(RSCMType.NPC))
                ?: error("Missing ${VasaRoom.CRYSTAL}")
        onModifyNpcHit(crystal) {
            when (hit.type) {
                EngineHitType.Ranged -> hit.damage = 0
                EngineHitType.Magic -> hit.damage /= 3
                else -> Unit
            }
        }
        onNpcHit(crystal) { points.award(this) }
    }

    override val spec: BossSpec =
        boss(*VasaRoom.TYPES.toTypedArray()) {
            stats(attackRate = 3)
            val magic =
                ability("magic_boulder") {
                    missile(
                        "seq.vasa_attack",
                        "spotanim.raids_vasanistirio_magic_travel",
                        HitType.Magic,
                        impact = "spotanim.raids_vasanistirio_magic_impact",
                    )
                }
            val ranged =
                ability("ranged_boulder") {
                    missile(
                        "seq.vasa_attack",
                        "spotanim.raids_vasanistirio_range_travel",
                        HitType.Ranged,
                        impact = "spotanim.raids_vasanistirio_range_impact",
                    )
                }
            phase("fight") {
                weightedSelectorRandom {
                    +random(magic, weight = 1)
                    +random(ranged, weight = 1)
                }
            }
        }
}
