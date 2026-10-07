package org.rsmod.content.bosses.dagannothkings

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val REX_MAX_HIT = 26
internal const val PRIME_MAX_HIT = 50
internal const val SUPREME_MAX_HIT = 30

internal fun rexSpec(): BossSpec =
    boss("npc.dagcave_melee_boss") {
        stats(attackRate = 4)
        val slash =
            ability("slash") {
                anim("seq.dagannoth_meganoth_attack_melee")
                hit {
                    damage(0..REX_MAX_HIT).roll()
                    type(Melee)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(slash, weight = 1) } }
    }

internal fun primeSpec(): BossSpec =
    boss("npc.dagcave_magic_boss") {
        stats(attackRate = 4)
        val waterBlast =
            ability("water_blast") {
                anim("seq.dagannoth_meganoth_attack_mage")
                projectile(
                    spotanim = "spotanim.waterwave_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.waterwave_impact",
                    hit = Effect.Hit(damage = Roll(0..PRIME_MAX_HIT), type = Magic),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(waterBlast, weight = 1) } }
    }

internal fun supremeSpec(): BossSpec =
    boss("npc.dagcave_ranged_boss") {
        stats(attackRate = 4)
        val spine =
            ability("spine") {
                anim("seq.dagannoth_meganoth_attack_range")
                projectile(
                    spotanim = "spotanim.dagannoth_spine_spotanim_travel",
                    travel = "projanim.arrow",
                    hit = Effect.Hit(damage = Roll(0..SUPREME_MAX_HIT), type = Ranged),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(spine, weight = 1) } }
    }
