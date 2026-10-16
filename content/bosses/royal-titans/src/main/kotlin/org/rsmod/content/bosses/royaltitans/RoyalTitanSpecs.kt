package org.rsmod.content.bosses.royaltitans

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal class Titan(
    val npc: String,
    val meleeSeq: String,
    val summonSeq: String,
    val areaSeq: String,
    val wallSpotanim: String,
    val elemental: String,
) {
    companion object {
        val BRANDA =
            Titan(
                npc = "npc.rt_fire_queen",
                meleeSeq = "seq.fire_giant_branda_queen_attack_melee01",
                summonSeq = "seq.fire_giant_branda_queen_attack_summon01",
                areaSeq = "seq.fire_giant_branda_queen_attack_aoe01",
                wallSpotanim = "spotanim.vfx_fire_giant_branda_queen_attack_aoe01",
                elemental = "npc.rt_summon_elemental_fire",
            )

        val ELDRIC =
            Titan(
                npc = "npc.rt_ice_king",
                meleeSeq = "seq.ice_giant_eldric_king_attack_melee01",
                summonSeq = "seq.ice_giant_eldric_king_attack_summon01",
                areaSeq = "seq.ice_giant_edric_king_attack_aoe01",
                wallSpotanim = "spotanim.royal_titans_area_attack_ice",
                elemental = "npc.rt_summon_elemental_ice",
            )
    }
}

internal object RoyalTitanSpecs {
    const val ELEMENTAL_HANDLER = "royal_titans.elemental"
    const val PHASE_TWO_HP = 0.35
    const val STOMP_MAX = 26
    const val ENRAGED_STOMP_MAX = 10
    const val WALL_MAX = 12
    const val WALL_DELAY = 4
    const val WALL_RADIUS = 4
    const val BLAST_MAX = 30
    const val BLAST_COUNT = 3
    const val BLAST_GAP = 2
    const val ELEMENTAL_COUNT = 3
    const val ATTACK_RATE = 7

    private fun wallsEffect(titan: Titan): Effect =
        withTiles(
            "walls",
            randomFreeTiles(
                area(
                    offset(CurrentTargetTile, -WALL_RADIUS, -WALL_RADIUS),
                    offset(CurrentTargetTile, WALL_RADIUS, WALL_RADIUS),
                ),
                5..8,
            ),
            sequence(
                onTiles(bound("walls"), mapSpotanim(titan.wallSpotanim, CurrentTile)),
                after(
                    WALL_DELAY,
                    onTiles(
                        bound("walls"),
                        hit(
                            damage = (0..WALL_MAX).roll(),
                            type = Typeless,
                            target = playersOn(CurrentTile),
                        ),
                    ),
                ),
            ),
        )

    private fun summonEffect(titan: Titan): Effect =
        summon(
            npc = titan.elemental,
            count = ELEMENTAL_COUNT,
            radius = 3,
            centeredOn = CurrentTarget,
            duration = 100,
            onSummon = ELEMENTAL_HANDLER,
        )

    private val blastEffect: Effect =
        sequence(
            message("<col=ff3045>The titan charges an elemental blast!</col>"),
            repeat(
                times = BLAST_COUNT,
                gap = BLAST_GAP,
                effect = hit(damage = (0..BLAST_MAX).roll(), type = Magic, delay = 2),
            ),
        )

    private val overheadsOffEffect: Effect = disablePrayers(overheadsOnly = true)

    fun build(titan: Titan): BossSpec {
        val walls = wallsEffect(titan)
        val summoned = summonEffect(titan)
        return boss(titan.npc) {
            stats(attackRate = ATTACK_RATE)

            val stomp =
                ability("stomp") {
                    anim(titan.meleeSeq)
                    hit {
                        damage((0..STOMP_MAX).roll())
                        type(Melee)
                    }
                }

            val enragedStomp =
                ability("enraged_stomp") {
                    anim(titan.meleeSeq)
                    hit {
                        damage((0..ENRAGED_STOMP_MAX).roll())
                        type(Melee)
                    }
                    include(overheadsOffEffect)
                }

            val wallAttack =
                ability("walls") {
                    anim(titan.areaSeq)
                    include(walls)
                }

            val summon =
                ability("summon_elementals") {
                    anim(titan.summonSeq)
                    include(summoned)
                }

            val blast =
                ability("elemental_blast") {
                    anim(titan.areaSeq)
                    include(blastEffect)
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(stomp, weight = 4, requires = WithinMeleeRange)
                    +random(wallAttack, weight = 2)
                    +random(summon, weight = 1)
                }
            }

            phase("enraged", entryHp = PHASE_TWO_HP) {
                weightedSelectorRandom {
                    +random(enragedStomp, weight = 3, requires = WithinMeleeRange)
                    +random(wallAttack, weight = 2)
                    +random(blast, weight = 2)
                    +random(summon, weight = 1)
                }
            }
        }
    }
}
