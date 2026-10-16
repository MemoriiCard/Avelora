package org.rsmod.content.bosses.hydra

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val HYDRA_NEXT_ACTION = "hydra.next_action"
internal const val POISON_ABILITY = "poison_splash"
internal const val POOL_ABILITY = "poison_pool"
internal const val LIGHTNING_ABILITY = "lightning"
internal const val FLAME_ABILITY = "flame"

internal const val HYDRA_ATTACK_RATE = 6
internal const val ENRAGED_ATTACK_RATE = 4
internal const val HYDRA_MAX_HIT = 17
internal const val EMPOWERED_MAX_HIT = 26
internal const val LIGHTNING_BIND_TICKS = 5

internal fun standardAbility(empowered: Boolean, ranged: Boolean, phase: HydraPhase): String =
    "${if (ranged) "ranged" else "magic"}_${phase.stage}${if (empowered) "_empowered" else ""}"

internal fun hydraSpec(): BossSpec =
    boss(*(HydraPhase.entries.map { it.npc } + HydraPhase.entries.mapNotNull { it.transition }).toTypedArray()) {
        stats(attackRate = HYDRA_ATTACK_RATE)
        val next = ability("next_action", external(HYDRA_NEXT_ACTION))
        for (phase in HydraPhase.entries) {
            for (empowered in listOf(false, true)) {
                val max = if (empowered) EMPOWERED_MAX_HIT else HYDRA_MAX_HIT
                ability(standardAbility(empowered, ranged = true, phase)) {
                    anim("seq.hydra_stage_${phase.stage}_attack_ranged")
                    repeat(2) {
                        projectile(
                            spotanim = "spotanim.hydraboss_ranged_proj",
                            travel = "projanim.arrow",
                            hit = Effect.Hit(damage = Accuracy(Roll(0..max)), type = Ranged),
                        )
                    }
                }
                ability(standardAbility(empowered, ranged = false, phase)) {
                    anim("seq.hydra_stage_${phase.stage}_attack_magic")
                    repeat(2) {
                        projectile(
                            spotanim = "spotanim.hydraboss_magic_proj",
                            travel = "projanim.magic_spell",
                            hit = Effect.Hit(damage = Accuracy(Roll(0..max)), type = Magic),
                        )
                    }
                }
            }
        }
        ability(POISON_ABILITY) {
            hit {
                damage(0..12).roll()
                type(Typeless)
            }
        }
        ability(POOL_ABILITY) {
            hit {
                damage(1..12).roll()
                type(Typeless)
            }
        }
        ability(LIGHTNING_ABILITY) {
            hit {
                damage(0..22).roll()
                type(Typeless)
            }
            freeze(LIGHTNING_BIND_TICKS)
        }
        ability(FLAME_ABILITY) {
            hit {
                damage(0..25).roll()
                type(Typeless)
            }
        }
        phase("combat") { weightedSelectorRandom { +random(next, weight = 1) } }
    }
