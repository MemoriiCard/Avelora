package org.rsmod.content.bosses.crazyarchaeologist

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect

internal const val CRAZY_ARCHAEOLOGIST = "npc.crazy_archaeologist"
internal const val RAIN_HANDLER = "crazyarchaeologist.rain_of_knowledge"

internal const val ATTACK_RATE = 3
internal const val STANDARD_MAX_HIT = 14
internal const val RAIN_SHOUT = "Rain of knowledge!"
internal const val RANGED_WEIGHT = 4
internal const val MELEE_WEIGHT = 4
internal const val RAIN_WEIGHT = 2

internal val SHOUTS =
    listOf(
        "You belong in a museum!",
        "I'm Bellock - respect me!",
        "Get off my site!",
        "These ruins are mine!",
        "Taste my knowledge!",
        "No-one messes with Bellock's dig!",
    )

private const val SHOUT_ONE_IN = 4
private val shout = chance(SHOUT_ONE_IN, oneOf(SHOUTS.map { say(it) }))
private val rainShout = say(RAIN_SHOUT)
private val notAdjacent = Condition.Not(WithinMeleeRange)

internal fun crazyArchaeologistSpec(): BossSpec =
    boss(CRAZY_ARCHAEOLOGIST) {
        stats(attackRate = ATTACK_RATE)
        val book =
            ability("book") {
                anim("seq.book_chuck")
                include(shout)
                projectile(
                    spotanim = "spotanim.crazy_archaeologist_book",
                    travel = "projanim.thrown",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..STANDARD_MAX_HIT)), type = Ranged),
                )
            }
        val punch =
            ability("punch") {
                anim("seq.human_unarmedpunch")
                include(shout)
                hit {
                    damage(Accuracy(Roll(0..STANDARD_MAX_HIT), meleeAttackType = MeleeAttackType.Crush))
                    type(Melee)
                }
            }
        val rain =
            ability("rain_of_knowledge") {
                anim("seq.book_chuck")
                include(rainShout)
                include(external(RAIN_HANDLER))
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(book, weight = RANGED_WEIGHT + MELEE_WEIGHT, requires = notAdjacent)
                +random(book, weight = RANGED_WEIGHT, requires = WithinMeleeRange)
                +random(punch, weight = MELEE_WEIGHT, requires = WithinMeleeRange)
                +random(rain, weight = RAIN_WEIGHT)
            }
        }
    }
