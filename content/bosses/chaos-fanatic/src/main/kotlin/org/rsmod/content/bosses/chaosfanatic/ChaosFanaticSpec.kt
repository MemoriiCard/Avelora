package org.rsmod.content.bosses.chaosfanatic

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect

internal const val CHAOS_FANATIC = "npc.chaos_fanatic"
internal const val CALM_ATTACKS_VARN = "varn.chaos_fanatic_calm_attacks"
internal const val EXPLOSION_HANDLER = "chaosfanatic.explosion"
internal const val DISARM_HANDLER = "chaosfanatic.disarm"

internal const val ATTACK_RATE = 2
internal const val MAGIC_MAX_HIT = 21
internal const val CALM_ATTACKS_AFTER_EXPLOSION = 12
internal const val STANDARD_WEIGHT = 10
internal const val EXPLOSION_WEIGHT = 4
internal const val DISARM_WEIGHT = 1

private val calm = Condition.VarnIn(CALM_ATTACKS_VARN, 1..CALM_ATTACKS_AFTER_EXPLOSION)
private val readyForSpecial = Condition.Not(calm)
private val hasFreeInvSpace = Condition.Custom { _, target -> target != null && target.inv.freeSpace() > 0 }
private val countDownCalm = whenever(calm, addVarn(CALM_ATTACKS_VARN, -1))
private val startCalm = setVarn(CALM_ATTACKS_VARN, CALM_ATTACKS_AFTER_EXPLOSION)

internal val SHOUTS =
    listOf(
        "BURN!",
        "WEUGH!",
        "Devilish Oxen Roll!!",
        "All your wilderness are belong to them!",
        "AhehHeheuhHhahueHuUEehEahAH!",
        "I shall call him squidgy and he shall be my squidgy!!",
    )
private const val SHOUT_ONE_IN = 4
private val shout = chance(SHOUT_ONE_IN, oneOf(SHOUTS.map { say(it) }))

internal fun chaosFanaticSpec(): BossSpec =
    boss(CHAOS_FANATIC) {
        stats(attackRate = ATTACK_RATE)
        val standard =
            ability("standard") {
                anim("seq.human_casting")
                include(countDownCalm)
                include(shout)
                projectile(
                    spotanim = "spotanim.chaoselemental_spotanim_discord_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.chaoselemental_spotanim_discord_impact",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..MAGIC_MAX_HIT)), type = Magic),
                )
            }
        val explosion =
            ability("explosion") {
                anim("seq.human_casting")
                include(startCalm)
                include(external(EXPLOSION_HANDLER))
            }
        val disarm =
            ability("disarm") {
                anim("seq.human_casting")
                projectile(
                    spotanim = "spotanim.chaoselemental_spotanim_madness_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.chaoselemental_spotanim_madness_impact",
                    onImpact = external(DISARM_HANDLER),
                )
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(standard, weight = STANDARD_WEIGHT)
                +random(explosion, weight = EXPLOSION_WEIGHT, requires = readyForSpecial)
                +random(disarm, weight = DISARM_WEIGHT, requires = readyForSpecial and hasFreeInvSpace)
            }
        }
    }
