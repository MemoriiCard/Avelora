package org.rsmod.content.bosses.cerberus

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val CERBERUS_SITTING = "npc.cerberus_sitting"
internal const val CERBERUS = "npc.cerberus_attacking"
internal const val SOUL_RANGED = "npc.cerberus_spectre_ranged"
internal const val SOUL_MAGIC = "npc.cerberus_spectre_magic"
internal const val SOUL_MELEE = "npc.cerberus_spectre_melee"

internal const val CERBERUS_NEXT_ACTION = "cerberus.next_action"
internal const val MELEE_ABILITY = "bite"
internal const val RANGED_ABILITY = "ranged"
internal const val MAGIC_ABILITY = "magic"
internal const val LAVA_LAND_ABILITY = "lava_land"
internal const val LAVA_BURN_ABILITY = "lava_burn"
internal const val SOUL_ABILITY = "soul"

internal const val CERBERUS_ATTACK_RATE = 6
internal const val CERBERUS_MAX_HIT = 23
internal const val SOUL_DAMAGE = 30
internal const val SOUL_PRAYER_DRAIN = 30
internal const val LAVA_LAND_DAMAGE = 10
internal const val LAVA_BURN_DAMAGE = 15

private val SOUL_DRAIN = statDrain("stat.prayer", amount = SOUL_PRAYER_DRAIN)

internal fun cerberusSpec(): BossSpec =
    boss(CERBERUS_SITTING, CERBERUS) {
        stats(attackRate = CERBERUS_ATTACK_RATE)
        val next = ability("next_action", external(CERBERUS_NEXT_ACTION))
        ability(MELEE_ABILITY) {
            anim("seq.cerberus_bite")
            hit {
                damage(Accuracy(Roll(0..CERBERUS_MAX_HIT), meleeAttackType = MeleeAttackType.Crush))
                type(Melee)
            }
        }
        ability(RANGED_ABILITY) {
            anim("seq.cerberus_attack_range")
            projectile(
                spotanim = "spotanim.hh_bone_ball_spin",
                travel = "projanim.arrow",
                impact = "spotanim.hh_bone_ball_burst",
                hit = Effect.Hit(damage = Accuracy(Roll(0..CERBERUS_MAX_HIT)), type = Ranged),
            )
        }
        ability(MAGIC_ABILITY) {
            anim("seq.cerberus_fire_breath")
            projectile(
                spotanim = "spotanim.hh_hellfire_burst",
                travel = "projanim.magic_spell",
                impact = "spotanim.hh_hellfire_hit",
                hit = Effect.Hit(damage = Accuracy(Roll(0..CERBERUS_MAX_HIT)), type = Magic),
            )
        }
        ability(LAVA_LAND_ABILITY) {
            hit {
                damage(LAVA_LAND_DAMAGE..LAVA_LAND_DAMAGE).roll()
                type(Typeless)
            }
        }
        ability(LAVA_BURN_ABILITY) {
            hit {
                damage(LAVA_BURN_DAMAGE..LAVA_BURN_DAMAGE).roll()
                type(Typeless)
            }
        }
        phase("combat") { weightedSelectorRandom { +random(next, weight = 1) } }
    }

internal fun soulSpec(type: String, style: org.rsmod.api.bosses.spec.HitType, seq: String, spotanim: String): BossSpec =
    boss(type) {
        stats(attackRate = CERBERUS_ATTACK_RATE)
        val soul =
            ability(SOUL_ABILITY) {
                anim(seq)
                projectile(
                    spotanim = spotanim,
                    travel = "projanim.magic_spell",
                    hit =
                        Effect.Hit(
                            damage = Roll(SOUL_DAMAGE..SOUL_DAMAGE),
                            type = style,
                            onHit = whenever(TargetPraying(style), SOUL_DRAIN),
                            onHitEvenOnMiss = true,
                        ),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(soul, weight = 1) } }
    }

internal fun soulSpecs(): List<BossSpec> =
    listOf(
        soulSpec(SOUL_RANGED, Ranged, "seq.spectre_ghost_range_attack", "spotanim.hh_bone_ball_spin"),
        soulSpec(SOUL_MAGIC, Magic, "seq.spectre_ghost_mage_attack", "spotanim.hh_hellfire_burst"),
        soulSpec(SOUL_MELEE, Melee, "seq.spectre_ghost_melee_attack", "spotanim.spectre_melee_proj"),
    )
