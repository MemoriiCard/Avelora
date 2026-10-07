package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Typeless
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.plugin.scripts.ScriptContext

class GuardiansScript @Inject constructor(deps: BossDeps, private val points: CoxDamagePoints) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                if (!hit.isFromPlayer) return@register
                val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) }
                val pickaxeLevel = hit.righthandType()?.name?.let(::pickaxeLevel)
                hit.damage =
                    if (attacker == null || pickaxeLevel == null) 0
                    else hit.damage * (BASE_MULTIPLIER + attacker.miningLvl + pickaxeLevel) / DIVISOR
            },
            onHit = { points.award(this) },
        )
    }

    override val spec: BossSpec =
        boss(GuardiansRoom.LEFT, GuardiansRoom.RIGHT) {
            stats(attackRate = 4)
            val cleave =
                ability("cleave") { melee("seq.human_staff_pound", MeleeAttackType.Slash) }
            val rockfall =
                ability("rockfall") {
                    anim("seq.human_staff_pummel")
                    debris(
                        telegraph = "spotanim.rockfall",
                        damage = DamageExpr.NpcMaxHit(MeleeAttackType.Crush),
                        type = Typeless,
                        windup = 2,
                        targetRadius = 1,
                        scatterRadius = 0,
                        count = 1..1,
                        center = TargetExpr.CurrentTarget,
                    )
                }
            phase("fight") {
                weightedSelectorRandom {
                    +random(cleave, weight = 3)
                    +random(rockfall, weight = 1)
                }
            }
        }

    private fun pickaxeLevel(name: String): Int? {
        val lower = name.lowercase()
        if ("pickaxe" !in lower) return null
        return PICKAXE_TIERS.firstOrNull { (prefix, _) -> lower.startsWith(prefix) }?.second
            ?: DRAGON_PICKAXE_LEVEL
    }

    private companion object {
        const val BASE_MULTIPLIER = 50
        const val DIVISOR = 150
        const val DRAGON_PICKAXE_LEVEL = 61
        val PICKAXE_TIERS =
            listOf(
                "bronze" to 1,
                "iron" to 1,
                "steel" to 6,
                "black" to 11,
                "mithril" to 21,
                "adamant" to 31,
                "rune" to 41,
                "gilded" to 41,
            )
    }
}
