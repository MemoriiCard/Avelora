package org.rsmod.content.bosses.giantmole

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.game.entity.Npc

internal const val GIANT_MOLE_MAX_HIT = 21
internal const val BURROW_ONE_IN = 4
internal const val BURROW_BELOW = 0.5
internal const val BURROW_MIN_HP = 10

internal fun giantMoleSpec(canBurrow: (Npc) -> Boolean): BossSpec =
    boss("npc.mole_giant") {
        stats(attackRate = 4)

        val melee =
            ability("melee") {
                anim("seq.mole_attack")
                hit {
                    damage(0..GIANT_MOLE_MAX_HIT).roll()
                    type(Melee)
                }
            }

        val burrow =
            ability("burrow") { include(chance(BURROW_ONE_IN, external("giantmole.burrow"))) }

        onIncomingHit(
            burrow,
            requires =
                HpBelow(BURROW_BELOW, inclusive = true) and
                    Condition.Custom { npc, _ -> canBurrow(npc) },
        )

        phase("combat") { weightedSelectorRandom { +random(melee, weight = 1) } }
    }
