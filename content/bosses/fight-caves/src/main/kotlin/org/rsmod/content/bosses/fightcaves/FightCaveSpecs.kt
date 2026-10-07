package org.rsmod.content.bosses.fightcaves

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val MEJKOT_HEAL_HANDLER = "fightcaves.mejkot_heal"
internal const val JAD_TELL_TICKS = 3

private val PRAYER_DRAIN = statDrain("stat.prayer", amount = 1)

internal fun tzKihSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_1a", "npc.tzhaar_fightcave_swarm_1b") {
        stats(attackRate = 4)
        val bite =
            ability("bite") {
                anim("seq.firebat_attack")
                hit {
                    damage(accurate(4, MeleeAttackType.Stab))
                    type(Melee)
                    onHit(PRAYER_DRAIN, evenOnMiss = true)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(bite, weight = 1) } }
    }

internal fun tzKekSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_2a", "npc.tzhaar_fightcave_swarm_2b") {
        stats(attackRate = 4)
        val bite = meleeAbility("bite", "seq.lavabeast_attack", 7, MeleeAttackType.Crush)
        val recoil =
            ability("recoil") {
                hit {
                    damage(Fixed(1))
                    type(Typeless)
                    hazard()
                }
            }
        phase("combat") { weightedSelectorRandom { +random(bite, weight = 1) } }
        onIncomingHit(recoil, requires = hitStyle(Melee))
    }

internal fun smallTzKekSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_2spawn") {
        stats(attackRate = 4)
        val bite = meleeAbility("bite", "seq.lavabeast_attack", 4, MeleeAttackType.Crush)
        phase("combat") { weightedSelectorRandom { +random(bite, weight = 1) } }
    }

internal fun tokXilSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_3a", "npc.tzhaar_fightcave_swarm_3b") {
        stats(attackRate = 4)
        val punch = meleeAbility("punch", "seq.magmaquris_punch", 13, MeleeAttackType.Crush)
        val spines =
            ability("spines") {
                anim("seq.magmaquris_spine_attack")
                projectile(
                    spotanim = "spotanim.tzhaar_spine_attack",
                    travel = "projanim.arrow",
                    hit = Effect.Hit(damage = accurate(14), type = Ranged),
                )
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(punch, weight = 1, requires = WithinMeleeRange)
                +random(spines, weight = 1, requires = !WithinMeleeRange)
            }
        }
    }

internal fun ytMejKotSpec(canHeal: Condition): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_4a", "npc.tzhaar_fightcave_swarm_4b") {
        stats(attackRate = 4)
        val smash = meleeAbility("smash", "seq.lizard_cleric_attack", 25, MeleeAttackType.Crush)
        val heal = ability("heal", external(MEJKOT_HEAL_HANDLER))
        phase("combat") {
            weightedSelectorRandom {
                +random(smash, weight = 1)
                +random(heal, weight = 1, requires = canHeal)
            }
        }
    }

internal fun ketZekSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_5a", "npc.tzhaar_fightcave_swarm_5b") {
        stats(attackRate = 4)
        val melee = meleeAbility("melee", "seq.igniferum_attack", 55, MeleeAttackType.Stab)
        val fireball =
            ability("fireball") {
                anim("seq.igniferum_ranged")
                projectile(
                    spotanim = "spotanim.tzhaar_fire_launch_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.tzhaar_fire_launch_impact",
                    hit = Effect.Hit(damage = accurate(52), type = Magic),
                )
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 1, requires = WithinMeleeRange)
                +random(fireball, weight = 1, requires = !WithinMeleeRange)
            }
        }
    }

internal fun tzTokJadSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_boss") {
        stats(attackRate = 8)
        val bite =
            ability("bite") {
                attackDelay = 4
                anim("seq.lordmagmus_attack")
                hit {
                    damage(accurate(97, MeleeAttackType.Stab))
                    type(Melee)
                }
            }
        val fire =
            ability("fire") {
                anim("seq.lordmagmus_fire")
                spotanim("spotanim.tzhaar_fire_spit_launch")
                wait(JAD_TELL_TICKS)
                projectile(
                    spotanim = "spotanim.tzhaar_fire_spit_travel",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = accurate(95), type = Magic),
                )
            }
        val boulder =
            ability("boulder") {
                anim("seq.lordmagmus_smash")
                wait(JAD_TELL_TICKS)
                hit {
                    damage(accurate(97))
                    type(Ranged)
                    spotanim("spotanim.tzhaar_rock_smash")
                    delay = 1
                }
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(bite, weight = 2, requires = WithinMeleeRange)
                +random(fire, weight = 1)
                +random(boulder, weight = 1)
            }
        }
    }

internal fun ytHurKotSpec(): BossSpec =
    boss("npc.tzhaar_fightcave_swarm_boss_cleric") {
        stats(attackRate = 4)
        val smash = meleeAbility("smash", "seq.lizard_cleric_attack", 14, MeleeAttackType.Crush)
        phase("combat") { weightedSelectorRandom { +random(smash, weight = 1) } }
    }

internal fun fightCaveSpecs(canHeal: Condition): List<BossSpec> =
    listOf(
        tzKihSpec(),
        tzKekSpec(),
        smallTzKekSpec(),
        tokXilSpec(),
        ytMejKotSpec(canHeal),
        ketZekSpec(),
        tzTokJadSpec(),
        ytHurKotSpec(),
    )

private fun accurate(max: Int, melee: MeleeAttackType? = null): DamageExpr =
    Accuracy(Roll(0..max), meleeAttackType = melee)

private fun BossSpecBuilder.meleeAbility(name: String, seq: String, max: Int, type: MeleeAttackType) =
    ability(name) {
        anim(seq)
        hit {
            damage(accurate(max, type))
            type(Melee)
        }
    }
