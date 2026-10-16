package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.plugin.scripts.ScriptContext

class VanguardsScript
@Inject
constructor(
    deps: BossDeps,
    private val points: CoxDamagePoints,
    private val services: CoxRoomServices,
) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        for (spec in listOf(spec, rangedSpec, magicSpec)) {
            BossCombat.register(this, spec, deps, onHit = { points.award(this) })
        }
        deps.extensionRegistry.register(SCATTER) { ctx ->
            val ranged = ctx.params == EngineHitType.Ranged
            repeat(SCATTER_COUNT) {
                val tile = services.random.of(ctx.target.coords, SCATTER_RADIUS)
                val spot = if (ranged) RANGED_TRAVEL else MAGIC_TRAVEL
                services.lob(spot, ctx.npc.coords.translate(1, 1), tile) {
                    for (player in services.players) {
                        if (player.coords == tile) {
                            val style = if (ranged) EngineHitType.Ranged else EngineHitType.Magic
                            services.strike(ctx.npc, player, style)
                        }
                    }
                }
            }
        }
    }

    override val spec: BossSpec =
        boss(VanguardsRoom.MELEE) {
            stats(attackRate = 4)
            val slash =
                ability("slash") { melee("seq.vanguard_attack_melee", MeleeAttackType.Slash) }
            phase("fight") { weightedSelectorRandom { +random(slash, weight = 1) } }
        }

    private val rangedSpec: BossSpec =
        boss(VanguardsRoom.RANGED) {
            stats(attackRate = 4)
            val volley =
                ability("volley") {
                    missile("seq.vanguard_attack_ranged", RANGED_TRAVEL, HitType.Ranged)
                    include(external(SCATTER, EngineHitType.Ranged))
                }
            phase("fight") { weightedSelectorRandom { +random(volley, weight = 1) } }
        }

    private val magicSpec: BossSpec =
        boss(VanguardsRoom.MAGIC) {
            stats(attackRate = 4)
            val volley =
                ability("volley") {
                    missile("seq.vanguard_attack_magic", MAGIC_TRAVEL, HitType.Magic)
                    include(external(SCATTER, EngineHitType.Magic))
                }
            phase("fight") { weightedSelectorRandom { +random(volley, weight = 1) } }
        }

    private companion object {
        const val SCATTER = "cox.vanguard_scatter"
        const val SCATTER_COUNT = 2
        const val SCATTER_RADIUS = 2
        const val RANGED_TRAVEL = "spotanim.raids_vanguard_range_0"
        const val MAGIC_TRAVEL = "spotanim.raids_vanguard_magic"
    }
}
