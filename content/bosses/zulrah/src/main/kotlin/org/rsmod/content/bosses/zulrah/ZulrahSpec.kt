package org.rsmod.content.bosses.zulrah

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val ZULRAH_NEXT_ACTION = "zulrah.next_action"
internal const val ZULRAH_VENOM = "zulrah.venom"

internal const val RANGED_ABILITY = "ranged"
internal const val MAGIC_ABILITY = "magic"
internal const val TAIL_ABILITY = "tail"
internal const val CLOUD_DAMAGE_ABILITY = "cloud_damage"

internal const val ZULRAH_MAX_HIT = 41
internal const val ZULRAH_ATTACK_RATE = 3

internal const val TAIL_STUN_TICKS = 5

private val VENOM = external(ZULRAH_VENOM)
private val TAIL_STUN = freeze(TAIL_STUN_TICKS)

internal fun zulrahSpec(): BossSpec =
    boss(*ZulrahForm.entries.map { it.npc }.toTypedArray()) {
        stats(attackRate = ZULRAH_ATTACK_RATE)
        val next = ability("next_action", external(ZULRAH_NEXT_ACTION))
        ability(RANGED_ABILITY) {
            anim("seq.snakeboss_attack_acidx1")
            projectile(
                spotanim = "spotanim.snakeboss_orb",
                travel = "projanim.arrow",
                hit = Effect.Hit(damage = Accuracy(Roll(0..ZULRAH_MAX_HIT)), type = Ranged, onHit = VENOM),
            )
        }
        ability(MAGIC_ABILITY) {
            anim("seq.snakeboss_attack_acidx1")
            projectile(
                spotanim = "spotanim.snakeboss_fireball",
                travel = "projanim.magic_spell",
                hit = Effect.Hit(damage = Accuracy(Roll(0..ZULRAH_MAX_HIT)), type = Magic, onHit = VENOM),
            )
        }
        ability(TAIL_ABILITY) {
            hit {
                damage(20..30).roll()
                type(Typeless)
                onHit(TAIL_STUN)
            }
        }
        ability(CLOUD_DAMAGE_ABILITY) {
            hit {
                damage(1..5).roll()
                type(Typeless)
                hazard()
            }
        }
        phase("combat") { weightedSelectorRandom { +random(next, weight = 1) } }
    }

internal fun meleeSnakelingSpec(): BossSpec =
    boss("npc.snakeboss_minion_melee") {
        stats(attackRate = 3)
        val bite =
            ability("bite") {
                anim("seq.snakeboss_pet_attack")
                hit {
                    damage(Accuracy(Roll(0..15), meleeAttackType = MeleeAttackType.Stab))
                    type(Melee)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(bite, weight = 1) } }
    }

internal fun magicSnakelingSpec(): BossSpec =
    boss("npc.snakeboss_minion_magic") {
        stats(attackRate = 3)
        val spell =
            ability("spell") {
                anim("seq.snakeboss_pet_attack")
                projectile(
                    spotanim = "spotanim.snakeboss_minion_spell",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..13)), type = Magic),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(spell, weight = 1) } }
    }
