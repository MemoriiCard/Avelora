package org.rsmod.content.bosses.inferno

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType

internal const val JAD_TELL_TICKS = 3

internal const val BLOB_MAGE = "npc.inferno_creature_splitter_mage"
internal const val BLOB_RANGE = "npc.inferno_creature_splitter_range"
internal const val BLOB_MELEE = "npc.inferno_creature_splitter_melee"

internal fun nibblerSpec(): BossSpec =
    boss("npc.inferno_nibbler") {
        stats(attackRate = 4)
        val bite = meleeAbility("bite", "seq.jalnib_attack", 4, MeleeAttackType.Crush)
        phase("combat") { weightedSelectorRandom { +random(bite, weight = 1) } }
    }

internal fun batSpec(): BossSpec =
    boss("npc.inferno_creature_harpie") {
        stats(attackRate = 3)
        val swoop = rangedAbility("swoop", "seq.jalmejrah_attack", "spotanim.inferno_harpie_proj", 19)
        phase("combat") { weightedSelectorRandom { +random(swoop, weight = 1) } }
    }

internal fun blobSpec(): BossSpec =
    boss("npc.inferno_creature_splitter") {
        stats(attackRate = 6)
        val melee = meleeAbility("melee", "seq.jalak_attack_melee", 29, MeleeAttackType.Slash)
        val ranged = rangedAbility("ranged", "seq.jalak_attack_ranged", "spotanim.inferno_splitter_range", 29)
        val magic = magicAbility("magic", "seq.jalak_attack_magic", "spotanim.inferno_splitter_mage", 29)
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 1, requires = WithinMeleeRange)
                +random(ranged, weight = 1, requires = !WithinMeleeRange)
                +random(magic, weight = 1, requires = !WithinMeleeRange)
            }
        }
    }

internal fun miniBlobSpecs(): List<BossSpec> =
    listOf(
        boss(BLOB_MELEE) {
            stats(attackRate = 4)
            val hit = meleeAbility("melee", "seq.jalak_attack_melee", MINI_BLOB_MAX, MeleeAttackType.Slash)
            phase("combat") { weightedSelectorRandom { +random(hit, weight = 1) } }
        },
        boss(BLOB_RANGE) {
            stats(attackRate = 4)
            val hit = rangedAbility("ranged", "seq.jalak_attack_ranged", "spotanim.inferno_babysplitter_range", MINI_BLOB_MAX)
            phase("combat") { weightedSelectorRandom { +random(hit, weight = 1) } }
        },
        boss(BLOB_MAGE) {
            stats(attackRate = 4)
            val hit = magicAbility("magic", "seq.jalak_attack_magic", "spotanim.inferno_babysplitter_mage", MINI_BLOB_MAX)
            phase("combat") { weightedSelectorRandom { +random(hit, weight = 1) } }
        },
    )

internal fun meleerSpec(): BossSpec =
    boss("npc.inferno_creature_melee") {
        stats(attackRate = 4)
        val slash = meleeAbility("slash", "seq.jalimkot_attack", 49, MeleeAttackType.Slash)
        phase("combat") { weightedSelectorRandom { +random(slash, weight = 1) } }
    }

internal fun rangerSpec(): BossSpec =
    boss("npc.inferno_creature_ranger") {
        stats(attackRate = 4)
        val melee = meleeAbility("melee", "seq.jalxil_attack_melee", 46, MeleeAttackType.Slash)
        val ranged = rangedAbility("ranged", "seq.jalxil_attack_ranged", "spotanim.inferno_xil_projectile", 46)
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 1, requires = WithinMeleeRange)
                +random(ranged, weight = 1, requires = !WithinMeleeRange)
            }
        }
    }

internal fun magerSpec(): BossSpec =
    boss("npc.inferno_creature_mager") {
        stats(attackRate = 4)
        val melee = meleeAbility("melee", "seq.jalakxil_attack_melee", 70, MeleeAttackType.Slash)
        val magic = magicAbility("magic", "seq.jalakxil_attack_magic", "spotanim.inferno_zek_projectile", 70)
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 1, requires = WithinMeleeRange)
                +random(magic, weight = 1, requires = !WithinMeleeRange)
            }
        }
    }

internal fun jadSpec(): BossSpec =
    boss("npc.inferno_jad") {
        stats(attackRate = 8)
        val bite =
            ability("bite") {
                attackDelay = 4
                anim("seq.jaltokjad_attack_melee")
                hit {
                    damage(accurate(JAD_MAX, MeleeAttackType.Slash))
                    type(Melee)
                }
            }
        val magic =
            ability("magic") {
                anim("seq.jaltokjad_attack_magic")
                wait(JAD_TELL_TICKS)
                projectile(
                    spotanim = "spotanim.inferno_zek_projectile",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = accurate(JAD_MAX), type = Magic),
                )
            }
        val ranged =
            ability("ranged") {
                anim("seq.jaltokjad_attack_ranged")
                wait(JAD_TELL_TICKS)
                hit {
                    damage(accurate(JAD_MAX))
                    type(Ranged)
                    delay = 1
                }
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(bite, weight = 2, requires = WithinMeleeRange)
                +random(magic, weight = 1)
                +random(ranged, weight = 1)
            }
        }
    }

internal fun infernoSpecs(): List<BossSpec> =
    listOf(nibblerSpec(), batSpec(), blobSpec(), meleerSpec(), rangerSpec(), magerSpec(), jadSpec()) +
        miniBlobSpecs()

private const val JAD_MAX = 113
private const val MINI_BLOB_MAX = 8

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

private fun BossSpecBuilder.rangedAbility(name: String, seq: String, spotanim: String, max: Int) =
    ability(name) {
        anim(seq)
        projectile(
            spotanim = spotanim,
            travel = "projanim.arrow",
            hit = Effect.Hit(damage = accurate(max), type = Ranged),
        )
    }

private fun BossSpecBuilder.magicAbility(name: String, seq: String, spotanim: String, max: Int) =
    ability(name) {
        anim(seq)
        projectile(
            spotanim = spotanim,
            travel = "projanim.magic_spell",
            hit = Effect.Hit(damage = accurate(max), type = Magic),
        )
    }
