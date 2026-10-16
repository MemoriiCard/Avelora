package org.rsmod.content.bosses.scorpia

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val SCORPIA = "npc.scorpia"
internal const val OFFSPRING = "npc.scorpia_minion"
internal const val GUARDIAN = "npc.scorpia_guardian"

internal const val PHASE_FIGHT = "fight"
internal const val PHASE_WOUNDED = "wounded"
internal const val PHASE_GUARDED = "guarded"

internal const val SCORPIA_MAX_HIT = 16
internal const val SCORPIA_POISON = 20
internal const val MELEE_PRAYER_DRAIN = 2
internal const val OFFSPRING_MAX_HIT = 2
internal const val OFFSPRING_POISON = 6

private val PROTECT_MELEE_DRAIN = statDrain("stat.prayer", amount = MELEE_PRAYER_DRAIN)
private val SCORPIA_POISONING = poison(SCORPIA_POISON)
private val OFFSPRING_POISONING = poison(OFFSPRING_POISON)

internal fun scorpiaSpec(): BossSpec =
    boss(SCORPIA) {
        stats(attackRate = 4)
        val sting =
            ability("sting") {
                anim("seq.scorpion_update_attack_tail")
                include(whenever(TargetPraying(Melee), PROTECT_MELEE_DRAIN))
                hit {
                    damage(Accuracy(Roll(0..SCORPIA_MAX_HIT), meleeAttackType = MeleeAttackType.Stab))
                    type(Melee)
                    onHit(SCORPIA_POISONING)
                }
            }
        for (name in listOf(PHASE_FIGHT, PHASE_WOUNDED, PHASE_GUARDED)) {
            phase(name) { weightedSelectorRandom { +random(sting, weight = 1, requires = WithinMeleeRange) } }
        }
    }

internal fun offspringSpec(): BossSpec =
    boss(OFFSPRING) {
        stats(attackRate = 4)
        val spit =
            ability("spit") {
                anim("seq.small_scorpion_update_attack")
                projectile(
                    spotanim = "spotanim.lizardman_spit",
                    travel = "projanim.arrow",
                    hit =
                        Effect.Hit(
                            damage = Accuracy(Roll(0..OFFSPRING_MAX_HIT)),
                            type = Ranged,
                            onHit = OFFSPRING_POISONING,
                        ),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(spit, weight = 1) } }
    }
