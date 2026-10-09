package org.rsmod.content.bosses.hespori

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object HesporiFight {
    const val BOSS = "npc.hespori"

    const val PHASE = "main"

    const val ATTACK_RATE = 5
    const val RANGED_MAX_HIT = 30
    const val MAGIC_MAX_HIT = 30
    const val VINE_MAX_HIT = 25
    const val VINE_EVERY_ATTACKS = 5
    const val ARENA_RADIUS = 15

    private val PROJECTILE =
        ProjectileConfig(
            startHeight = 120,
            endHeight = 40,
            startDelay = 30,
            travelTime = 0,
            angle = 14,
            progress = 48,
            stepMultiplier = 5,
        )

    fun spec(): BossSpec =
        boss(BOSS) {
            stats(attackRate = ATTACK_RATE)

            fun projectile(spot: String, impact: String?, max: Int, type: HitType) =
                onEach(
                    AllInRadius(radius = ARENA_RADIUS),
                    Effect.Projectile(
                        spotanim = spot,
                        config = PROJECTILE,
                        hit = Effect.Hit(damage = Roll(0..max), type = type, spotanim = impact),
                    ),
                )

            val ranged =
                ability("ranged") {
                    anim("seq.hespori_attack_ranged")
                    include(projectile("spotanim.hespori_range_proj", null, RANGED_MAX_HIT, Ranged))
                }

            val magic =
                ability("magic") {
                    anim("seq.hespori_attack_ranged")
                    include(
                        projectile(
                            "spotanim.hespori_magic_proj",
                            "spotanim.hespori_magic_impact",
                            MAGIC_MAX_HIT,
                            Magic,
                        )
                    )
                }

            val vines =
                ability("vines") {
                    anim("seq.hespori_attack_special")
                    include(
                        projectile(
                            "spotanim.hespori_vine_proj",
                            "spotanim.hespori_vine_impact",
                            VINE_MAX_HIT,
                            Typeless,
                        )
                    )
                }

            phase(PHASE) {
                weightedSelectorRandom {
                    +random(ranged, weight = 1)
                    +random(magic, weight = 1)
                }
                forceEveryAttacks(VINE_EVERY_ATTACKS, VINE_EVERY_ATTACKS, vines)
            }
        }
}

class Hespori @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = HesporiFight.spec()
}
