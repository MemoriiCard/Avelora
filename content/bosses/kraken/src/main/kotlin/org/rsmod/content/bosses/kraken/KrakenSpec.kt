package org.rsmod.content.bosses.kraken

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal const val KRAKEN = "npc.slayer_kraken_boss"
internal const val KRAKEN_WHIRLPOOL = "npc.slayer_kraken_boss_whirlpool"
internal const val TENTACLE = "npc.slayer_kraken_boss_tentacle"
internal const val TENTACLE_WHIRLPOOL = "npc.slayer_kraken_boss_tentacle_whirlpool"
internal const val CAVE_KRAKEN = "npc.slayer_kraken"
internal const val CAVE_KRAKEN_WHIRLPOOL = "npc.slayer_kraken_sub"

internal fun krakenSpec(): BossSpec =
    boss(KRAKEN, KRAKEN_WHIRLPOOL) {
        stats(attackRate = 4)
        val blast =
            ability("blast") {
                anim("seq.swan_queen_spellcast")
                projectile(
                    spotanim = "spotanim.firewave_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.firewave_impact",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..28)), type = Ranged, penetration = 100),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(blast, weight = 1) } }
    }

internal fun tentacleSpec(): BossSpec =
    boss(TENTACLE, TENTACLE_WHIRLPOOL) {
        stats(attackRate = 4)
        val splash =
            ability("splash") {
                anim("seq.tentacle_monster_attack")
                projectile(
                    spotanim = "spotanim.waterwave_travel",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..2)), type = Ranged, penetration = 100),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(splash, weight = 1) } }
    }

internal fun caveKrakenSpec(): BossSpec =
    boss(CAVE_KRAKEN, CAVE_KRAKEN_WHIRLPOOL) {
        stats(attackRate = 6)
        val wave =
            ability("wave") {
                anim("seq.swan_queen_spellcast")
                projectile(
                    spotanim = "spotanim.waterwave_travel",
                    travel = "projanim.magic_spell",
                    impact = "spotanim.waterwave_impact",
                    hit = Effect.Hit(damage = Accuracy(Roll(0..13)), type = Magic),
                )
            }
        phase("combat") { weightedSelectorRandom { +random(wave, weight = 1) } }
    }
