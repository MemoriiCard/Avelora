package org.rsmod.content.bosses.inferno

import kotlin.math.abs
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal object ZukStats {
    const val NPC = "npc.inferno_tzkalzuk_placeholder"
    const val GLYPH = "npc.inferno_moving_safespot"
    const val HEALER = "npc.inferno_zuk_healer"
    const val JAD_HEALER = "npc.inferno_jad_healer"
    const val MAX_HIT = 148
    const val ATTACK_RATE = 10
    const val ENRAGED_ATTACK_RATE = 7
    const val JAD_AT = 0.4
    const val HEALERS_AT = 0.2
    const val SHIELD_HP = 600
    const val HEALER_COUNT = 4
    const val ZUK_HEAL_AMOUNT = 5
    const val ZUK_HEAL_INTERVAL = 4
    const val BOMBARD_MIN = 5
    const val BOMBARD_MAX = 10
    const val SET_INTERVAL = 60
    const val TOKKUL = 16_440
    const val PET_ODDS = 100
    const val CAPE = "obj.infernal_cape"
    const val PET = "obj.infernopet"
    const val SET_HANDLER = "inferno.zuk_set"
    const val JAD_HANDLER = "inferno.zuk_jad"
    const val HEALERS_HANDLER = "inferno.zuk_healers"
    val SPAWN = CoordGrid(2267, 5361, 0)
}

internal object ZukArena {
    const val GLYPH_Z = 5345
    const val GLYPH_MIN_X = 2262
    const val GLYPH_MAX_X = 2282
    const val GLYPH_STEP_TICKS = 2
    const val HALF_WIDTH = 1

    val HEALER_TILES = listOf(2262, 2266, 2278, 2282).map { CoordGrid(it, 5356, 0) }
}

internal class ZukShield(private val startTick: Int, private val dx: Int = 0, private val dz: Int = 0) {
    var hp: Int = ZukStats.SHIELD_HP
        private set

    val broken: Boolean
        get() = hp <= 0

    fun x(tick: Int): Int {
        val span = ZukArena.GLYPH_MAX_X - ZukArena.GLYPH_MIN_X
        val step = ((tick - startTick) / ZukArena.GLYPH_STEP_TICKS).coerceAtLeast(0)
        val phase = step % (span * 2)
        return ZukArena.GLYPH_MIN_X + if (phase <= span) phase else span * 2 - phase
    }

    fun covers(playerX: Int, playerZ: Int, tick: Int): Boolean =
        !broken && playerZ - dz < ZukArena.GLYPH_Z && abs(playerX - dx - x(tick)) <= ZukArena.HALF_WIDTH

    fun glyphCoords(tick: Int): CoordGrid =
        CoordGrid(x(tick) - ZukArena.HALF_WIDTH + dx, ZukArena.GLYPH_Z + dz, 0)

    fun absorb(damage: Int) {
        hp = (hp - damage).coerceAtLeast(0)
    }
}

internal fun zukSpec(roll: (Npc, Player) -> Int): BossSpec =
    boss(ZukStats.NPC) {
        stats(attackRate = ZukStats.ATTACK_RATE)
        val blast =
            ability("blast") {
                anim("seq.zuk_attack")
                projectile(
                    spotanim = "spotanim.inferno_zuk_projectile",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = DamageExpr.Custom(roll), type = Typeless),
                )
            }
        val summonJad = ability("summon_jad", external(ZukStats.JAD_HANDLER))
        val summonHealers = ability("summon_healers", external(ZukStats.HEALERS_HANDLER))
        phase("combat", attackRate = ZukStats.ATTACK_RATE) {
            every(ZukStats.SET_INTERVAL, external(ZukStats.SET_HANDLER))
            weightedSelectorRandom { +random(blast, weight = 1) }
        }
        phase("jad_entry", entryHp = ZukStats.JAD_AT, exitAfter = 1, nextPhase = "combat") {
            entry = summonJad.name
        }
        phase("healers_entry", entryHp = ZukStats.HEALERS_AT, exitAfter = 1, nextPhase = "enraged") {
            entry = summonHealers.name
        }
        phase("enraged", attackRate = ZukStats.ENRAGED_ATTACK_RATE) {
            every(ZukStats.SET_INTERVAL, external(ZukStats.SET_HANDLER))
            weightedSelectorRandom { +random(blast, weight = 1) }
        }
    }

internal fun zukHealerSpec(): BossSpec =
    boss(ZukStats.HEALER) {
        stats(attackRate = 4)
        val bombard =
            ability("bombard") {
                anim("seq.jalmejrah_attack")
                projectile(
                    spotanim = "spotanim.inferno_harpie_proj",
                    travel = "projanim.arrow",
                    hit =
                        Effect.Hit(
                            damage = DamageExpr.Roll(ZukStats.BOMBARD_MIN..ZukStats.BOMBARD_MAX),
                            type = Typeless,
                        ),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(bombard, weight = 1) } }
    }

internal fun jadHealerSpec(): BossSpec =
    boss(ZukStats.JAD_HEALER) {
        stats(attackRate = 4)
        val smash =
            ability("smash") {
                anim("seq.lizard_cleric_attack")
                hit {
                    damage(Accuracy(Roll(0..JAD_HEALER_MAX), meleeAttackType = MeleeAttackType.Crush))
                    type(Melee)
                }
            }
        phase("combat") { weightedSelectorRandom { +random(smash, weight = 1) } }
    }

private const val JAD_HEALER_MAX = 14
