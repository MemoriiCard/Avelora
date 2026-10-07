package org.rsmod.content.bosses.nex

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.map.CoordGrid

internal const val NEX = "npc.nex"
internal const val NEX_SOULSPLIT = "npc.nex_soulsplit"

internal const val PHASE_SMOKE = "smoke"
internal const val PHASE_SHADOW = "shadow"
internal const val PHASE_BLOOD = "blood"
internal const val PHASE_ICE = "ice"
internal const val PHASE_ZAROS = "zaros"

internal const val CALM_ATTACKS_VARN = "varn.nex_calm_attacks"
internal const val CALM_ATTACKS_AFTER_SPECIAL = 4

internal const val NO_ESCAPE_HANDLER = "nex.no_escape"
internal const val VIRUS_HANDLER = "nex.virus"
internal const val SHADOW_SMASH_HANDLER = "nex.shadow_smash"
internal const val SIPHON_HANDLER = "nex.blood_siphon"
internal const val SACRIFICE_HANDLER = "nex.blood_sacrifice"
internal const val CONTAIN_HANDLER = "nex.contain"
internal const val ICE_PRISON_HANDLER = "nex.ice_prison"

internal const val ATTACK_RATE = 4
internal const val MELEE_MAX_HIT = 30
internal const val SMOKE_MAX_HIT = 33
internal const val SHADOW_MAX_HIT = 30
internal const val BLOOD_MAX_HIT = 30
internal const val ICE_MAX_HIT = 33
internal const val ZAROS_MAX_HIT = 33
internal const val LIFESTEAL_PERCENT = 25
internal const val SMOKE_POISON = 8
internal const val MAGE_MAX_HIT = 20

internal const val MAIN_WEIGHT = 6
internal const val MELEE_WEIGHT = 3
internal const val SPECIAL_WEIGHT = 2

private val calm = Condition.VarnIn(CALM_ATTACKS_VARN, 1..CALM_ATTACKS_AFTER_SPECIAL)
private val readyForSpecial = Condition.Not(calm)
private val countDownCalm = whenever(calm, addVarn(CALM_ATTACKS_VARN, -1))
private val startCalm = setVarn(CALM_ATTACKS_VARN, CALM_ATTACKS_AFTER_SPECIAL)

private val SMOKE_POISONING = chance(4, poison(SMOKE_POISON))
private val ICE_FREEZE = freeze(ticks = 5, chance = 1, outOf = 4)

private val arenaPlayers =
    playersIn(
        area(
            customTile { _, _ -> CoordGrid(NexArena.MIN_X, NexArena.MIN_Z, NexArena.LEVEL) },
            customTile { _, _ -> CoordGrid(NexArena.MAX_X, NexArena.MAX_Z, NexArena.LEVEL) },
        )
    )

private fun barrage(
    spotanim: String,
    impact: String?,
    maxHit: Int,
    type: HitType = Magic,
    lifesteal: Int = 0,
    onHit: Effect? = null,
): Effect =
    onEach(
        arenaPlayers,
        Effect.Projectile(
            spotanim = spotanim,
            travel = "projanim.magic_spell",
            impact = impact,
            hit = Effect.Hit(damage = Accuracy(Roll(0..maxHit)), type = type, lifesteal = lifesteal, onHit = onHit),
        ),
    )

internal fun nexSpec(): BossSpec =
    boss(NEX, NEX_SOULSPLIT) {
        stats(attackRate = ATTACK_RATE)
        val melee =
            ability("melee") {
                anim("seq.nex_attack")
                include(countDownCalm)
                hit {
                    damage(Accuracy(Roll(0..MELEE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                    type(Melee)
                }
            }
        fun main(name: String, effect: Effect, seq: String = "seq.nex_cast_attack") =
            ability(name) {
                anim(seq)
                include(countDownCalm)
                include(effect)
            }
        fun special(name: String, handler: String, seq: String = "seq.nex_cast_attack") =
            ability(name) {
                anim(seq)
                include(startCalm)
                include(external(handler))
            }

        val smoke = main("smoke", barrage("spotanim.nex_smoke_attack_proj", "spotanim.nex_smoke_attack_impact", SMOKE_MAX_HIT, onHit = SMOKE_POISONING))
        val noEscape = special("no_escape", NO_ESCAPE_HANDLER, seq = "seq.nex_spin_out")
        val virus = special("virus", VIRUS_HANDLER)

        val shadow = main("shadow", barrage("spotanim.nex_shadow_attack_proj", null, SHADOW_MAX_HIT, type = Ranged))
        val shadowSmash = special("shadow_smash", SHADOW_SMASH_HANDLER)

        val blood = main("blood", barrage("spotanim.nex_blood_attack_proj", "spotanim.nex_blood_attack_impact", BLOOD_MAX_HIT, lifesteal = LIFESTEAL_PERCENT))
        val siphon = special("blood_siphon", SIPHON_HANDLER, seq = "seq.nex_blood_siphon")
        val sacrifice = special("blood_sacrifice", SACRIFICE_HANDLER)

        val ice = main("ice", barrage("spotanim.nex_ice_attack_proj", "spotanim.nex_ice_attack_impact", ICE_MAX_HIT, onHit = ICE_FREEZE))
        val contain = special("contain", CONTAIN_HANDLER, seq = "seq.nex_smash_attack")
        val icePrison = special("ice_prison", ICE_PRISON_HANDLER)

        val finale =
            main(
                "finale",
                barrage("spotanim.nex_finale_attack_proj", "spotanim.nex_finale_attack_impact", ZAROS_MAX_HIT, lifesteal = LIFESTEAL_PERCENT),
                seq = "seq.nex_alternate_cast_attack",
            )

        fun phaseWith(name: String, attack: AbilityRef, vararg specials: AbilityRef, transmog: String? = null) =
            phase(name, transmog = transmog) {
                weightedSelectorRandom {
                    +random(attack, weight = MAIN_WEIGHT)
                    +random(melee, weight = MELEE_WEIGHT, requires = WithinMeleeRange)
                    for (special in specials) +random(special, weight = SPECIAL_WEIGHT, requires = readyForSpecial)
                }
            }
        phaseWith(PHASE_SMOKE, smoke, noEscape, virus)
        phaseWith(PHASE_SHADOW, shadow, shadowSmash)
        phaseWith(PHASE_BLOOD, blood, siphon, sacrifice)
        phaseWith(PHASE_ICE, ice, contain, icePrison)
        phaseWith(PHASE_ZAROS, finale, transmog = NEX_SOULSPLIT)
    }

internal fun mageSpec(mage: NexMage): BossSpec =
    boss(mage.npc) {
        stats(attackRate = 5)
        val cast =
            ability("cast") {
                anim("seq.human_casting")
                projectile(
                    spotanim = mage.projectile,
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..MAGE_MAX_HIT)), type = Magic),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(cast, weight = 1) } }
    }
