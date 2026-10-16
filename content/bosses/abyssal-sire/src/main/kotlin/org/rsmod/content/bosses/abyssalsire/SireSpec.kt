package org.rsmod.content.bosses.abyssalsire

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val SIRE_SLEEPING = "npc.abyssalsire_sire_stasis_sleeping"
internal const val SIRE_AWAKE = "npc.abyssalsire_sire_stasis_awake"
internal const val SIRE_STUNNED = "npc.abyssalsire_sire_stasis_stunned"
internal const val SIRE_WANDERING = "npc.abyssalsire_sire_wandering"
internal const val SIRE_PANICKING = "npc.abyssalsire_sire_panicking"
internal const val SIRE_APOCALYPSE = "npc.abyssalsire_sire_apocalypse"
internal val SIRE_TYPES =
    listOf(SIRE_SLEEPING, SIRE_AWAKE, SIRE_STUNNED, SIRE_WANDERING, SIRE_PANICKING, SIRE_APOCALYPSE)

internal val TENTACLE_SLEEPING =
    listOf(
        "npc.abyssalsire_tentacle_sleeping_north",
        "npc.abyssalsire_tentacle_sleeping_south",
        "npc.abyssalsire_tentacle_sleeping_upright",
    )
internal const val TENTACLE_ACTIVE = "npc.abyssalsire_tentacle_active"
internal const val TENTACLE_STUNNED = "npc.abyssalsire_tentacle_stunned"

internal const val LUNG = "npc.abyssalsire_lung"
internal const val LUNG_DYING = "npc.abyssalsire_lung_dying"
internal const val SPAWN = "npc.abyssalsire_spawn"
internal const val SCION = "npc.abyssalsire_scion"

internal const val SIRE_NEXT_ACTION = "abyssal_sire.next_action"
internal const val DOUBLE_HOOK_ABILITY = "double_hook"
internal const val MIASMA_ABILITY = "miasma"
internal const val EXPLOSION_ABILITY = "explosion"

internal const val SIRE_ATTACK_RATE = 7
internal const val SIRE_MELEE_MAX = 66
internal const val SIRE_MELEE_PRAYER_PENETRATION = 40
internal const val EXPLOSION_MAX = 96
internal const val MIASMA_POISON = 8

internal fun sireSpec(): BossSpec =
    boss(*SIRE_TYPES.toTypedArray()) {
        stats(attackRate = SIRE_ATTACK_RATE)
        val next = ability("next_action", external(SIRE_NEXT_ACTION))
        ability(DOUBLE_HOOK_ABILITY) {
            anim("seq.sire_attack_double_hook")
            hit {
                damage(Accuracy(Roll(0..SIRE_MELEE_MAX), meleeAttackType = MeleeAttackType.Slash))
                type(Melee)
                penetration(SIRE_MELEE_PRAYER_PENETRATION)
            }
        }
        ability(MIASMA_ABILITY) {
            hit {
                damage(1..4).roll()
                type(Typeless)
            }
            poison(MIASMA_POISON)
        }
        ability(EXPLOSION_ABILITY) {
            hit {
                damage(0..EXPLOSION_MAX).roll()
                type(Typeless)
            }
        }
        phase("combat") { weightedSelectorRandom { +random(next, weight = 1) } }
    }

internal fun tentacleSpec(): BossSpec =
    boss(*(TENTACLE_SLEEPING + TENTACLE_ACTIVE + TENTACLE_STUNNED).toTypedArray()) {
        stats(attackRate = 4)
        val swipe =
            ability("swipe") {
                anim("seq.abyssal_tentacle_attack")
                hit {
                    damage(Accuracy(Roll(0..30), meleeAttackType = MeleeAttackType.Crush))
                    type(Melee)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(swipe, weight = 1, requires = WithinMeleeRange) } }
    }

internal fun spawnSpec(): BossSpec =
    boss(SPAWN) {
        stats(attackRate = 4)
        val bite =
            ability("bite") {
                anim("seq.abyssal_spawn_attack")
                hit {
                    damage(Accuracy(Roll(0..5), meleeAttackType = MeleeAttackType.Stab))
                    type(Melee)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(bite, weight = 1, requires = WithinMeleeRange) } }
    }

internal fun scionSpec(): BossSpec =
    boss(SCION) {
        stats(attackRate = 4)
        val melee =
            ability("melee") {
                anim("seq.abyssal_scion_attack_melee")
                hit {
                    damage(Accuracy(Roll(0..10), meleeAttackType = MeleeAttackType.Stab))
                    type(Melee)
                }
            }
        val ranged =
            ability("ranged") {
                anim("seq.abyssal_scion_attack_ranged")
                projectile(
                    spotanim = "spotanim.abyssal_spawn_projanim",
                    travel = "projanim.arrow",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..15)), type = Ranged),
                )
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 2, requires = WithinMeleeRange)
                +random(ranged, weight = 1)
            }
        }
    }
