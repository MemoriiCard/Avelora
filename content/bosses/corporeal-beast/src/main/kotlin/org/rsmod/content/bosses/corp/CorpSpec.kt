package org.rsmod.content.bosses.corp

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val CORP = "npc.corp_beast"
internal const val DARK_CORE = "npc.dark_core"

internal const val CORE_ROLL = "corp.core_roll"
internal const val SPLIT_SHOT = "corp.split_shot"

internal const val STOMP_ABILITY = "stomp"
internal const val CORE_LEECH_ABILITY = "core_leech"
internal const val SPLIT_DIRECT_ABILITY = "split_direct"
internal const val SPLIT_NEAR_ABILITY = "split_near"
internal const val SPLINTER_DIRECT_ABILITY = "splinter_direct"
internal const val SPLINTER_NEAR_ABILITY = "splinter_near"

private const val MAGIC_SEQ = "seq.corpbeast_sprite_shoot_1"
private val ROLL_CORE = external(CORE_ROLL)
private val DRAIN = statDrain("stat.magic", "stat.prayer", amount = 2, chance = 1, outOf = 2)

internal fun corpSpec(): BossSpec =
    boss(CORP) {
        stats(attackRate = 4)
        val melee =
            ability("melee") {
                include(ROLL_CORE)
                anim("seq.corpbeast_swiping_attack")
                hit {
                    damage(Accuracy(Roll(0..33), meleeAttackType = MeleeAttackType.Crush))
                    type(Melee)
                }
            }
        val strong =
            ability("magic") {
                include(ROLL_CORE)
                anim(MAGIC_SEQ)
                projectile(
                    spotanim = "spotanim.corp_spirit_beast_strong_proj",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..65)), type = Magic),
                )
            }
        val drain =
            ability("drain") {
                include(ROLL_CORE)
                anim(MAGIC_SEQ)
                projectile(
                    spotanim = "spotanim.corp_spirit_beast_mid_proj",
                    travel = "projanim.magic_spell",
                    hit =
                        Effect.Hit(
                            damage = Accuracy(Roll(0..55)),
                            type = Magic,
                            onHit = DRAIN,
                            lifesteal = 50,
                        ),
                )
            }
        val split =
            ability("split") {
                include(ROLL_CORE)
                anim(MAGIC_SEQ)
                include(external(SPLIT_SHOT))
            }
        ability(STOMP_ABILITY) { typeless(30..51) }
        ability(CORE_LEECH_ABILITY) {
            hit {
                damage(5..13).roll()
                type(Typeless)
                hazard()
                lifesteal(100)
            }
        }
        ability(SPLIT_DIRECT_ABILITY) { typeless(0..40) }
        ability(SPLIT_NEAR_ABILITY) { typeless(0..30) }
        ability(SPLINTER_DIRECT_ABILITY) { typeless(0..30) }
        ability(SPLINTER_NEAR_ABILITY) { typeless(0..20) }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 2, requires = WithinMeleeRange)
                +random(strong, weight = 1)
                +random(drain, weight = 1)
                +random(split, weight = 1)
            }
        }
    }

private fun AbilityBuilder.typeless(range: IntRange) {
    hit {
        damage(range).roll()
        type(Typeless)
        hazard()
    }
}
