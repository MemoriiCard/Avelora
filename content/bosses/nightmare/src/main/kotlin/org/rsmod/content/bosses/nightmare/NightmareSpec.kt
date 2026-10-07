package org.rsmod.content.bosses.nightmare

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.map.CoordGrid

internal const val NIGHTMARE = "npc.nightmare_initial"
internal const val NIGHTMARE_BLAST = "npc.nightmare_blast"
internal const val NIGHTMARE_DYING = "npc.nightmare_dying"

internal val NIGHTMARE_TYPES: List<String> =
    listOf(NIGHTMARE) + NightmarePhase.entries.flatMap { listOf(it.shielded, it.weak) } + NIGHTMARE_BLAST

internal const val CALM_ATTACKS_VARN = "varn.nightmare_calm_attacks"
internal const val CALM_ATTACKS_AFTER_SPECIAL = 3

internal const val CLAWS_HANDLER = "nightmare.grasping_claws"
internal const val HUSKS_HANDLER = "nightmare.husks"
internal const val PARASITE_HANDLER = "nightmare.parasite"
internal const val SURGE_HANDLER = "nightmare.surge"
internal const val SPORES_HANDLER = "nightmare.spores"

internal const val ATTACK_RATE = 6
internal const val MELEE_MAX_HIT = 40
internal const val MAGIC_MAX_HIT = 30
internal const val RANGED_MAX_HIT = 30

internal const val MAIN_WEIGHT = 4
internal const val MELEE_WEIGHT = 3
internal const val SPECIAL_WEIGHT = 2

private val calm = Condition.VarnIn(CALM_ATTACKS_VARN, 1..CALM_ATTACKS_AFTER_SPECIAL)
private val readyForSpecial = Condition.Not(calm)
private val countDownCalm = whenever(calm, addVarn(CALM_ATTACKS_VARN, -1))
private val startCalm = setVarn(CALM_ATTACKS_VARN, CALM_ATTACKS_AFTER_SPECIAL)

private val arenaPlayers =
    playersIn(
        area(
            customTile { _, _ -> CoordGrid(NightmareArena.MIN_X, NightmareArena.MIN_Z, NightmareArena.LEVEL) },
            customTile { _, _ -> CoordGrid(NightmareArena.MAX_X, NightmareArena.MAX_Z, NightmareArena.LEVEL) },
        )
    )

private fun volley(spotanim: String, impact: String?, maxHit: Int, type: HitType): Effect =
    onEach(
        arenaPlayers,
        Effect.Projectile(
            spotanim = spotanim,
            travel = "projanim.magic_spell",
            impact = impact,
            hit = Effect.Hit(damage = Accuracy(Roll(0..maxHit)), type = type),
        ),
    )

internal fun nightmareSpec(): BossSpec =
    boss(*NIGHTMARE_TYPES.toTypedArray()) {
        stats(attackRate = ATTACK_RATE)
        val melee =
            ability("melee") {
                anim("seq.nightmare_attack_melee")
                include(countDownCalm)
                hit {
                    damage(Accuracy(Roll(0..MELEE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                    type(Melee)
                }
            }
        val magic =
            ability("magic") {
                anim("seq.nightmare_attack_magic")
                include(countDownCalm)
                include(volley("spotanim.nightmare_magic_travel", "spotanim.nightmare_magic_impact", MAGIC_MAX_HIT, Magic))
            }
        val ranged =
            ability("ranged") {
                anim("seq.nightmare_attack_ranged")
                include(countDownCalm)
                include(volley("spotanim.nightmare_ranged_travel", null, RANGED_MAX_HIT, Ranged))
            }
        fun special(name: String, handler: String, seq: String) =
            ability(name) {
                anim(seq)
                include(startCalm)
                include(external(handler))
            }
        val claws = special("grasping_claws", CLAWS_HANDLER, "seq.nightmare_attack_rift")
        val husks = special("husks", HUSKS_HANDLER, "seq.nightmare_attack_summon")
        val parasite = special("parasite", PARASITE_HANDLER, "seq.nightmare_attack_parasite")
        val surge = special("surge", SURGE_HANDLER, "seq.nightmare_attack_surge")
        val spores = special("spores", SPORES_HANDLER, "seq.nightmare_attack_infection")

        val specials =
            mapOf(
                NightmarePhase.One to listOf(claws, husks),
                NightmarePhase.Two to listOf(claws, parasite),
                NightmarePhase.Three to listOf(claws, surge, spores),
            )
        for ((phase, phaseSpecials) in specials) {
            phase(phase.key) {
                weightedSelectorRandom {
                    +random(magic, weight = MAIN_WEIGHT)
                    +random(ranged, weight = MAIN_WEIGHT)
                    +random(melee, weight = MELEE_WEIGHT, requires = WithinMeleeRange)
                    for (special in phaseSpecials) +random(special, weight = SPECIAL_WEIGHT, requires = readyForSpecial)
                }
            }
        }
    }

internal const val HUSK_MAX_HIT = 6

internal fun huskSpec(type: String, travel: String, hitType: HitType): BossSpec =
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
