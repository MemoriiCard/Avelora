package org.rsmod.content.bosses.hueycoatl

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object HueycoatlFight {
    val BOSSES = arrayOf("npc.huey_head", "npc.huey_head_enraged")

    const val PHASE_OPENING = "opening"
    const val PHASE_ENRAGED = "enraged"
    const val ENRAGE_HP = 0.25

    const val ATTACK_RATE = 5
    const val ENRAGED_ATTACK_RATE = 4
    const val MELEE_MAX_HIT = 35
    const val RANGED_MAX_HIT = 30
    const val MAGIC_MAX_HIT = 30
    const val SLAM_MAX_HIT = 25
    const val SLAM_EVERY_ATTACKS = 6
    const val ARENA_RADIUS = 15

    private val PROJECTILE =
        ProjectileConfig(
            startHeight = 140,
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

            fun styleAttack(spot: String, impact: String, max: Int, type: HitType) =
                onEach(
                    AllInRadius(radius = ARENA_RADIUS),
                    Effect.Projectile(
                        spotanim = spot,
                        config = PROJECTILE,
                        hit = Effect.Hit(damage = Roll(0..max), type = type, spotanim = impact),
                    ),
                )

            val melee =
                ability("melee") {
                    anim("seq.huey_attack_melee")
                    hit {
                        damage(0..MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val ranged =
                ability("ranged") {
                    anim("seq.huey_attack_range")
                    include(
                        styleAttack(
                            "spotanim.vfx_huey_attack_ranged_projanim_01",
                            "spotanim.vfx_huey_attack_ranged_impactanim_01",
                            RANGED_MAX_HIT,
                            Ranged,
                        )
                    )
                }

            val magic =
                ability("magic") {
                    anim("seq.huey_attack_magic")
                    include(
                        styleAttack(
                            "spotanim.vfx_huey_attack_magic_projanim_01",
                            "spotanim.vfx_huey_attack_magic_impactanim_01",
                            MAGIC_MAX_HIT,
                            Magic,
                        )
                    )
                }

            val slam =
                ability("tail_slam") {
                    anim("seq.huey_screech")
                    wait(2)
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Hit(damage = Roll(0..SLAM_MAX_HIT), type = Typeless),
                        )
                    )
                }

            fun PhaseBuilder.pattern() {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                    +random(magic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(SLAM_EVERY_ATTACKS, SLAM_EVERY_ATTACKS, slam)
            }

            phase(PHASE_OPENING) { pattern() }
            phase(PHASE_ENRAGED, entryHp = ENRAGE_HP, attackRate = ENRAGED_ATTACK_RATE) { pattern() }
        }
}

class Hueycoatl @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = HueycoatlFight.spec()
}
