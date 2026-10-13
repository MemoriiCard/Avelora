package org.rsmod.content.minigames.colosseum

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val SOL_TELL_TICKS = 3

private fun accurate(max: Int, melee: MeleeAttackType? = null): DamageExpr =
    Accuracy(Roll(0..max), meleeAttackType = melee)

private fun BossSpecBuilder.melee(name: String, seq: String, max: Int, type: MeleeAttackType) =
    ability(name) {
        anim(seq)
        hit {
            damage(accurate(max, type))
            type(Melee)
        }
    }

private fun BossSpecBuilder.ranged(name: String, seq: String, max: Int, delay: Int = 2) =
    ability(name) {
        anim(seq)
        hit {
            damage(accurate(max))
            type(Ranged)
            this.delay = delay
        }
    }

private fun BossSpecBuilder.magic(name: String, seq: String, max: Int, delay: Int = 2) =
    ability(name) {
        anim(seq)
        hit {
            damage(accurate(max))
            type(Magic)
            this.delay = delay
        }
    }

internal fun warbanderMeleeSpec(): BossSpec =
    boss("npc.colosseum_warbander_melee_male") {
        stats(attackRate = 4)
        val stab = melee("stab", "seq.npc_fremennik_warbander_melee_human_sword_stab", 14, MeleeAttackType.Stab)
        phase("combat") { weightedSelectorRandom { +random(stab, weight = 1) } }
    }

internal fun warbanderMageSpec(): BossSpec =
    boss("npc.colosseum_warbander_mage_male") {
        stats(attackRate = 4)
        val cast = magic("cast", "seq.npc_fremennik_warbander_mage_zaros_vertical_casting_walkmerge", 14)
        phase("combat") { weightedSelectorRandom { +random(cast, weight = 1) } }
    }

internal fun warbanderArcherSpec(): BossSpec =
    boss("npc.colosseum_warbander_ranged_female") {
        stats(attackRate = 4)
        val shoot = ranged("shoot", "seq.npc_fremennik_warbander_archer_att_colosseum", 14)
        phase("combat") { weightedSelectorRandom { +random(shoot, weight = 1) } }
    }

internal fun shamanSpec(): BossSpec =
    boss("npc.colosseum_standard_mager") {
        stats(attackRate = 4)
        val bolt = magic("bolt", "seq.npc_serpent_mager_casting", 28)
        phase("combat") { weightedSelectorRandom { +random(bolt, weight = 1) } }
    }

internal fun jaguarSpec(): BossSpec =
    boss("npc.colosseum_jaguar_warrior") {
        stats(attackRate = 4)
        val claw = melee("claw", "seq.npc_jaguar_combat_melee", 22, MeleeAttackType.Slash)
        phase("combat") { weightedSelectorRandom { +random(claw, weight = 1) } }
    }

internal fun javelinSpec(): BossSpec =
    boss("npc.colosseum_javelin_colossus") {
        stats(attackRate = 5)
        val throwSpear = ranged("throw", "seq.npc_colossi_javelin_01_range_attack", 34, delay = 3)
        val artillery = ranged("artillery", "seq.npc_colossi_javelin_01_artillery_attack", 40, delay = 4)
        phase("combat") {
            weightedSelectorRandom {
                +random(throwSpear, weight = 3)
                +random(artillery, weight = 1)
            }
        }
    }

internal fun manticoreSpec(): BossSpec =
    boss("npc.colosseum_manticore") {
        stats(attackRate = 5)
        val fire = magic("fire", "seq.npc_manticore_01_triple_throw", 38, delay = 3)
        val sting = ranged("sting", "seq.npc_manticore_01_triple_throw", 38, delay = 3)
        val maul = melee("maul", "seq.npc_manticore_01_triple_charge", 38, MeleeAttackType.Slash)
        phase("combat") {
            weightedSelectorRandom {
                +random(fire, weight = 1)
                +random(sting, weight = 1)
                +random(maul, weight = 1, requires = WithinMeleeRange)
            }
        }
    }

internal fun shockwaveSpec(): BossSpec =
    boss("npc.colosseum_shockwave_colossus") {
        stats(attackRate = 6)
        val clap =
            ability("clap") {
                anim("seq.npc_colossi_shockwave_01_clapattack")
                wait(SOL_TELL_TICKS)
                hit {
                    damage(accurate(36, MeleeAttackType.Crush))
                    type(Melee)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(clap, weight = 1) } }
    }

internal fun minotaurSpec(): BossSpec =
    boss("npc.colosseum_minotaur") {
        stats(attackRate = 4)
        val gore = melee("gore", "seq.npc_minotaur_boss_attack_melee", 32, MeleeAttackType.Crush)
        val roar = magic("roar", "seq.npc_minotaur_boss_attack_magic", 26)
        phase("combat") {
            weightedSelectorRandom {
                +random(gore, weight = 3, requires = WithinMeleeRange)
                +random(roar, weight = 1)
            }
        }
    }

internal fun minotaurRoutefindSpec(): BossSpec =
    boss("npc.colosseum_minotaur_routefind") {
        stats(attackRate = 4)
        val gore = melee("gore", "seq.npc_minotaur_boss_attack_melee", 32, MeleeAttackType.Crush)
        val roar = magic("roar", "seq.npc_minotaur_boss_attack_magic", 26)
        phase("combat") {
            weightedSelectorRandom {
                +random(gore, weight = 3, requires = WithinMeleeRange)
                +random(roar, weight = 1)
            }
        }
    }

internal fun solSpec(): BossSpec =
    boss("npc.colosseum_sol_p1") {
        stats(attackRate = 5)
        val strike =
            ability("strike") {
                anim("seq.npc_colossi_finalboss_01_melee_attack_telegraph")
                wait(2)
                anim("seq.npc_colossi_finalboss_01_melee_attack")
                hit {
                    damage(accurate(SOL_MELEE_MAX, MeleeAttackType.Slash))
                    type(Melee)
                }
            }
        val triple =
            ability("triple") {
                attackDelay = 8
                anim("seq.npc_colossi_finalboss_tripleattack")
                wait(SOL_TELL_TICKS)
                hit {
                    damage(accurate(SOL_TRIPLE_MAX, MeleeAttackType.Slash))
                    type(Melee)
                }
                wait(1)
                hit {
                    damage(accurate(SOL_TRIPLE_MAX, MeleeAttackType.Slash))
                    type(Melee)
                }
                wait(1)
                hit {
                    damage(accurate(SOL_TRIPLE_MAX, MeleeAttackType.Slash))
                    type(Melee)
                }
            }
        val slam =
            ability("slam") {
                attackDelay = 8
                anim("seq.npc_colossi_finalboss_01_shieldslam_telegraph")
                wait(SOL_TELL_TICKS + 1)
                hit {
                    damage(accurate(SOL_SLAM_MAX, MeleeAttackType.Crush))
                    type(Melee)
                }
            }
        val spear = ranged("spear", "seq.npc_colossi_finalboss_01_grapple_attack_telegraph", SOL_SPEAR_MAX, delay = 3)
        phase("combat") {
            weightedSelectorRandom {
                +random(strike, weight = 4, requires = WithinMeleeRange)
                +random(triple, weight = 2, requires = WithinMeleeRange)
                +random(slam, weight = 2, requires = WithinMeleeRange)
                +random(spear, weight = 2, requires = !WithinMeleeRange)
            }
        }
    }

internal fun colosseumSpecs(): List<BossSpec> =
    listOf(
        warbanderMeleeSpec(),
        warbanderMageSpec(),
        warbanderArcherSpec(),
        shamanSpec(),
        jaguarSpec(),
        javelinSpec(),
        manticoreSpec(),
        shockwaveSpec(),
        minotaurSpec(),
        minotaurRoutefindSpec(),
        solSpec(),
    )

private const val SOL_MELEE_MAX = 55
private const val SOL_TRIPLE_MAX = 40
private const val SOL_SLAM_MAX = 70
private const val SOL_SPEAR_MAX = 45
