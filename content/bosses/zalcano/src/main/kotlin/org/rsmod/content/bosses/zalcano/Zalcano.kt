package org.rsmod.content.bosses.zalcano

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object ZalcanoFight {
    val BOSSES = arrayOf("npc.zalcano", "npc.zalcano_weak")
    const val GOLEM = "npc.zalcano_golem"

    const val PHASE = "main"

    const val ATTACK_RATE = 4
    const val MELEE_MAX_HIT = 25
    const val RANGED_MAX_HIT = 25
    const val MAGIC_MAX_HIT = 25
    const val ROCKFALL_MAX_HIT = 20
    const val ROCKFALL_EVERY_ATTACKS = 5
    const val GOLEM_EVERY_ATTACKS = 8
    const val GOLEM_COUNT = 2
    const val ARENA_RADIUS = 15

    private val PROJECTILE =
        ProjectileConfig(
            startHeight = 110,
            endHeight = 40,
            startDelay = 30,
            travelTime = 0,
            angle = 14,
            progress = 48,
            stepMultiplier = 5,
        )

    fun spec(): BossSpec =
        boss(*BOSSES) {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim("seq.zalcano_attack_melee")
                    hit {
                        damage(0..MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val ranged =
                ability("ranged") {
                    anim("seq.zalcano_attack_ranged")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.zalcano_ore_throw",
                                config = PROJECTILE,
                                hit =
                                    Effect.Hit(
                                        damage = Roll(0..RANGED_MAX_HIT),
                                        type = Ranged,
                                        spotanim = "spotanim.zalcano_ore_hit",
                                    ),
                            ),
                        )
                    )
                }

            val magic =
                ability("magic") {
                    anim("seq.zalcano_attack_magic")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Hit(
                                damage = Roll(0..MAGIC_MAX_HIT),
                                type = Magic,
                                spotanim = "spotanim.zalcano_fire_attack",
                            ),
                        )
                    )
                }

            val rockfall =
                ability("rockfall") {
                    anim("seq.zalcano_attack_rock_fall")
                    wait(2)
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Hit(
                                damage = Roll(0..ROCKFALL_MAX_HIT),
                                type = Typeless,
                                spotanim = "spotanim.zalcano_rock_fall",
                            ),
                        )
                    )
                }

            val golems =
                ability("golems") {
                    anim("seq.zalcano_attack_golem")
                    summon(GOLEM, count = GOLEM_COUNT, radius = 3, owned = true)
                }

            phase(PHASE) {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                    +random(magic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(ROCKFALL_EVERY_ATTACKS, ROCKFALL_EVERY_ATTACKS, rockfall)
                forceEveryAttacks(GOLEM_EVERY_ATTACKS, GOLEM_EVERY_ATTACKS, golems)
            }
        }
}

class Zalcano @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = ZalcanoFight.spec()
}
