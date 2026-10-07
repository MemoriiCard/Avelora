package org.rsmod.content.bosses.vorkath

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val VORKATH_NEXT_ACTION = "vorkath.next_action"
internal const val VORKATH_VENOM = "vorkath.venom"
internal const val SPAWN_EXPLODE = "vorkath.spawn_explode"

internal const val MELEE_ABILITY = "melee"
internal const val MAGIC_ABILITY = "magic"
internal const val RANGED_ABILITY = "ranged"
internal const val DRAGONFIRE_ABILITY = "dragonfire"
internal const val VENOM_FIRE_ABILITY = "venom_dragonfire"
internal const val PRAYER_FIRE_ABILITY = "prayer_dragonfire"
internal const val ICE_BREATH_ABILITY = "ice_breath"
internal const val FIREBALL_DIRECT_ABILITY = "fireball_direct"
internal const val FIREBALL_SPLASH_ABILITY = "fireball_splash"
internal const val RAPID_FIRE_ABILITY = "rapid_fire"
internal const val ACID_ABILITY = "acid"
internal const val BLAST_ABILITY = "blast"

internal const val VORKATH_ATTACK_RATE = 5
internal const val SPAWN_HITPOINTS = 38
internal const val SPAWN_MAX_HIT = 60

private const val ATTACK_SEQ = "seq.ds2_vorkath_ranged"
private val VENOM = external(VORKATH_VENOM)
private val PRAYERS_OFF = disablePrayers()

internal fun vorkathSpec(): BossSpec =
    boss("npc.vorkath") {
        stats(attackRate = VORKATH_ATTACK_RATE)
        val next = ability("next_action", external(VORKATH_NEXT_ACTION))
        ability(MELEE_ABILITY) {
            anim("seq.ds2_vorkath_attack_melee")
            hit {
                damage(Accuracy(Roll(0..32), meleeAttackType = MeleeAttackType.Slash))
                type(Melee)
            }
        }
        ability(MAGIC_ABILITY) {
            anim(ATTACK_SEQ)
            projectile(
                spotanim = "spotanim.vorkath_magic_travel",
                travel = "projanim.magic_spell",
                impact = "spotanim.vorkath_magic_impact",
                hit = Effect.Hit(damage = Accuracy(Roll(0..30)), type = Magic),
            )
        }
        ability(RANGED_ABILITY) {
            anim(ATTACK_SEQ)
            projectile(
                spotanim = "spotanim.vorkath_ranged_travel",
                travel = "projanim.arrow",
                impact = "spotanim.vorkath_ranged_impact",
                hit = Effect.Hit(damage = Accuracy(Roll(0..32)), type = Ranged),
            )
        }
        ability(DRAGONFIRE_ABILITY) {
            anim(ATTACK_SEQ)
            projectile(
                spotanim = "spotanim.dragon_ranged_fire_attack",
                travel = "projanim.dragonfire",
                hit = Effect.Hit(damage = Roll(0..80), type = Dragonfire),
            )
        }
        ability(VENOM_FIRE_ABILITY) {
            anim(ATTACK_SEQ)
            projectile(
                spotanim = "spotanim.dragon_ranged_venom_attack",
                travel = "projanim.dragonfire",
                impact = "spotanim.dragon_venom_impact",
                hit =
                    Effect.Hit(
                        damage = Roll(0..80),
                        type = Dragonfire,
                        onHit = VENOM,
                        onHitEvenOnMiss = true,
                    ),
            )
        }
        ability(PRAYER_FIRE_ABILITY) {
            anim(ATTACK_SEQ)
            projectile(
                spotanim = "spotanim.dragon_ranged_corrupting_attack",
                travel = "projanim.dragonfire",
                impact = "spotanim.dragon_corrupting_impact",
                hit =
                    Effect.Hit(
                        damage = Roll(0..80),
                        type = Dragonfire,
                        onHit = PRAYERS_OFF,
                        onHitEvenOnMiss = true,
                    ),
            )
        }
        ability(ICE_BREATH_ABILITY) {
            anim(ATTACK_SEQ)
            projectile(spotanim = "spotanim.dragon_ranged_ice_attack", travel = "projanim.dragonfire")
        }
        ability(FIREBALL_DIRECT_ABILITY) {
            hit {
                damage(110..121).roll()
                type(Typeless)
                hazard()
            }
        }
        ability(FIREBALL_SPLASH_ABILITY) {
            hit {
                damage(55..60).roll()
                type(Typeless)
                hazard()
            }
        }
        ability(RAPID_FIRE_ABILITY) {
            hit {
                damage(25..41).roll()
                type(Typeless)
                hazard()
            }
        }
        ability(ACID_ABILITY) {
            hit {
                damage(1..10).roll()
                type(Typeless)
                hazard()
            }
        }
        phase("combat") { weightedSelectorRandom { +random(next, weight = 1) } }
    }

internal fun zombifiedSpawnSpec(): BossSpec =
    boss("npc.vorkath_spawn") {
        stats(attackRate = 1)
        val explode = ability("explode", external(SPAWN_EXPLODE))
        ability(BLAST_ABILITY) {
            hit {
                damage(DamageExpr.Custom { npc, _ -> npc.hitpoints * SPAWN_MAX_HIT / SPAWN_HITPOINTS })
                type(Typeless)
                hazard()
            }
        }
        phase("combat") {
            weightedSelectorRandom { +random(explode, weight = 1, requires = WithinMeleeRange) }
        }
    }
