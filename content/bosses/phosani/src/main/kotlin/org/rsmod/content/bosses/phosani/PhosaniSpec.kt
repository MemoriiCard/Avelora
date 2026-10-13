package org.rsmod.content.bosses.phosani

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType

internal const val PHOSANI_FIRST = "npc.nightmare_challenge_phase_01"
internal const val HUSK_MAGIC = "npc.nightmare_challenge_husk_magic"
internal const val HUSK_RANGED = "npc.nightmare_challenge_husk_ranged"

internal val PHOSANI_TYPES =
    listOf(PHOSANI_FIRST, "npc.nightmare_challenge_phase_02", "npc.nightmare_challenge_phase_03")

internal const val ATTACK_RATE = 6
internal const val MELEE_MAX_HIT = 73
internal const val MAGIC_MAX_HIT = 80
internal const val RANGED_MAX_HIT = 80
internal const val CLAWS_MAX_HIT = 80
internal const val CLAWS_DELAY = 3
internal const val HUSK_MAX_HIT = 12
internal const val PHASE_TWO_HP = 0.66
internal const val PHASE_THREE_HP = 0.33

private val arena = area(offset(Self, -9, -9), offset(Self, 9, 9))

private val clawsEffect: Effect =
    withTiles(
        "claws",
        tilesUnderPlayers(arena) + randomFreeTiles(arena, 10..14),
        sequence(
            onTiles(bound("claws"), mapSpotanim("spotanim.nightmare_rift", CurrentTile)),
            after(
                CLAWS_DELAY,
                onTiles(
                    bound("claws"),
                    hit(
                        damage = (1..CLAWS_MAX_HIT).roll(),
                        type = Typeless,
                        target = playersOn(CurrentTile),
                    ),
                ),
            ),
        ),
    )

private fun huskEffect(type: String): Effect =
    summon(npc = type, count = 1, radius = 2, centeredOn = CurrentTarget, duration = 60)

private val huskPairEffect: Effect = sequence(huskEffect(HUSK_MAGIC), huskEffect(HUSK_RANGED))

private val prayerBreakEffect: Effect = disablePrayers(overheadsOnly = true)

private fun volley(spotanim: String, impact: String?, maxHit: Int, type: HitType): Effect =
    Effect.Projectile(
        spotanim = spotanim,
        travel = "projanim.magic_spell",
        impact = impact,
        hit = Effect.Hit(damage = Accuracy(Roll(0..maxHit)), type = type),
    )

internal fun phosaniSpec(): BossSpec =
    boss(*PHOSANI_TYPES.toTypedArray()) {
        stats(attackRate = ATTACK_RATE)
        val melee =
            ability("melee") {
                anim("seq.nightmare_attack_melee")
                hit {
                    damage(Accuracy(Roll(0..MELEE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                    type(Melee)
                }
            }
        val breakingMelee =
            ability("breaking_melee") {
                anim("seq.nightmare_attack_melee")
                hit {
                    damage(Accuracy(Roll(0..MELEE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                    type(Melee)
                }
                include(prayerBreakEffect)
            }
        val magic =
            ability("magic") {
                anim("seq.nightmare_attack_magic")
                include(volley("spotanim.nightmare_magic_travel", "spotanim.nightmare_magic_impact", MAGIC_MAX_HIT, Magic))
            }
        val ranged =
            ability("ranged") {
                anim("seq.nightmare_attack_ranged")
                include(volley("spotanim.nightmare_ranged_travel", null, RANGED_MAX_HIT, Ranged))
            }
        val claws =
            ability("grasping_claws") {
                anim("seq.nightmare_attack_rift")
                include(clawsEffect)
            }
        val husks =
            ability("husks") {
                anim("seq.nightmare_attack_summon")
                include(huskPairEffect)
            }

        phase("phase_1") {
            weightedSelectorRandom {
                +random(magic, weight = 4)
                +random(ranged, weight = 4)
                +random(melee, weight = 3, requires = WithinMeleeRange)
                +random(claws, weight = 2)
                +random(husks, weight = 1)
            }
        }
        phase("phase_2", entryHp = PHASE_TWO_HP, transmog = "npc.nightmare_challenge_phase_02") {
            weightedSelectorRandom {
                +random(magic, weight = 4)
                +random(ranged, weight = 4)
                +random(melee, weight = 3, requires = WithinMeleeRange)
                +random(claws, weight = 3)
                +random(husks, weight = 2)
            }
        }
        phase("phase_3", entryHp = PHASE_THREE_HP, transmog = "npc.nightmare_challenge_phase_03") {
            weightedSelectorRandom {
                +random(magic, weight = 3)
                +random(ranged, weight = 3)
                +random(breakingMelee, weight = 5, requires = WithinMeleeRange)
                +random(claws, weight = 4)
                +random(husks, weight = 2)
            }
        }
    }

internal fun phosaniHuskSpec(type: String, travel: String, hitType: HitType): BossSpec =
    boss(type) {
        stats(attackRate = 4)
        val cast =
            ability("cast") {
                anim(if (hitType == Magic) "seq.husk_magic_attack" else "seq.husk_ranged_attack")
                projectile(
                    spotanim = travel,
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..HUSK_MAX_HIT)), type = hitType),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(cast, weight = 1) } }
    }
