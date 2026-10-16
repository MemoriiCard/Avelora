package org.rsmod.content.bosses.chaoselemental

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val CHAOS_ELEMENTAL_MAX_HIT = 28
internal const val CONFUSION_HANDLER = "chaoselemental.confusion"
internal const val MADNESS_HANDLER = "chaoselemental.madness"

internal fun chaosElementalSpec(): BossSpec =
    boss("npc.chaoselemental") {
        stats(attackRate = 4)

        val discord =
            ability("discord") {
                anim("seq.chaoselemental_attack")
                spotanim("spotanim.chaoselemental_spotanim_discord_casting")
                include(
                    oneOf(
                        discordBolt(Magic),
                        discordBolt(Magic),
                        discordBolt(Ranged),
                        discordBolt(Melee),
                    )
                )
            }

        val confusion =
            ability("confusion") {
                anim("seq.chaoselemental_attack")
                spotanim("spotanim.chaoselemental_spotanim_confusion_casting")
                projectile(
                    spotanim = "spotanim.chaoselemental_spotanim_confusion_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.chaoselemental_spotanim_confusion_impact",
                    onImpact = external(CONFUSION_HANDLER),
                )
            }

        val madness =
            ability("madness") {
                anim("seq.chaoselemental_attack")
                spotanim("spotanim.chaoselemental_spotanim_madness_casting")
                projectile(
                    spotanim = "spotanim.chaoselemental_spotanim_madness_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.chaoselemental_spotanim_madness_impact",
                    onImpact = external(MADNESS_HANDLER),
                )
            }

        phase("combat") {
            weightedSelectorRandom {
                +random(discord, weight = 3)
                +random(confusion, weight = 1)
                +random(madness, weight = 1, requires = hasFreeInvSpace)
            }
        }
    }

private val hasFreeInvSpace = Condition.Custom { _, target -> target != null && target.inv.freeSpace() > 0 }

private fun discordBolt(type: HitType): Effect =
    Effect.Projectile(
        spotanim = "spotanim.chaoselemental_spotanim_discord_travel",
        travel = "projanim.magic_spell",
        impact = "spotanim.chaoselemental_spotanim_discord_impact",
        hit = Effect.Hit(damage = Accuracy(Roll(0..CHAOS_ELEMENTAL_MAX_HIT), meleeAttackType = attackType(type)), type = type),
    )

private fun attackType(type: HitType): MeleeAttackType? = if (type == Melee) MeleeAttackType.Crush else null
