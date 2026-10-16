package org.rsmod.content.bosses.kalphitequeen

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val KQ_MAX_HIT = 31
internal const val CRAWLING = "crawling"
internal const val AIRBORNE = "airborne"
internal const val CRAWLING_NPC = "npc.kalphite_queen"
internal const val AIRBORNE_NPC = "npc.kalphite_flyingqueen"

internal fun kalphiteQueenSpec(): BossSpec =
    boss(CRAWLING_NPC, AIRBORNE_NPC) {
        stats(attackRate = 4)

        val crawlingMelee = melee("crawling_melee", "seq.kalphite_update_queen_jaws_attack")
        val crawlingMagic =
            magic("crawling_magic", "seq.kalphite_update_queen_ranged_attack", "spotanim.kalphite_queen_glow")
        val crawlingRanged =
            ranged("crawling_ranged", "seq.kalphite_update_queen_ranged_attack", "spotanim.kalphite_queen_spine")

        val airborneMelee = melee("airborne_melee", "seq.kalphite_update_flying_queen_stinger_attack")
        val airborneMagic =
            magic("airborne_magic", "seq.kalphite_update_flying_queen_ranged_attack", "spotanim.kalphite_glow")
        val airborneRanged =
            ranged("airborne_ranged", "seq.kalphite_update_flying_queen_ranged_attack", "spotanim.kalphite_spine")

        phase(CRAWLING) {
            weightedSelectorRandom {
                +random(crawlingMelee, weight = 2, requires = WithinMeleeRange)
                +random(crawlingMagic, weight = 1)
                +random(crawlingRanged, weight = 1)
            }
        }
        phase(AIRBORNE, transmog = AIRBORNE_NPC) {
            weightedSelectorRandom {
                +random(airborneMelee, weight = 2, requires = WithinMeleeRange)
                +random(airborneMagic, weight = 1)
                +random(airborneRanged, weight = 1)
            }
        }
    }

private fun BossSpecBuilder.melee(name: String, seq: String): AbilityRef =
    ability(name) {
        anim(seq)
        hit {
            damage(Accuracy(Roll(0..KQ_MAX_HIT), meleeAttackType = MeleeAttackType.Stab))
            type(Melee)
        }
    }

private fun BossSpecBuilder.magic(name: String, seq: String, cast: String): AbilityRef =
    ability(name) {
        anim(seq)
        spotanim(cast)
        projectile(
            spotanim = "spotanim.kalphite_glow_travel",
            travel = "projanim.magic_spell",
            impact = "spotanim.kalphite_glow_impact",
            hit = Effect.Hit(damage = Roll(0..KQ_MAX_HIT), type = Magic),
        )
    }

private fun BossSpecBuilder.ranged(name: String, seq: String, spine: String): AbilityRef =
    ability(name) {
        anim(seq)
        projectile(
            spotanim = spine,
            travel = "projanim.arrow",
            hit =
                Effect.Hit(
                    damage = Roll(0..KQ_MAX_HIT),
                    type = Ranged,
                    target = AllInRadius(1, of = CurrentTarget),
                ),
        )
        statDrain("stat.prayer", amount = 1)
    }
