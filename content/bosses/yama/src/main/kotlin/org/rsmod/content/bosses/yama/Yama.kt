package org.rsmod.content.bosses.yama

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object YamaFight {
    const val BOSS = "npc.yama"

    const val PHASE_OPENING = "opening"
    const val PHASE_ENRAGED = "enraged"
    const val ENRAGE_HP = 0.4

    const val ATTACK_RATE = 5
    const val ENRAGED_ATTACK_RATE = 4
    const val MELEE_MAX_HIT = 50
    const val MAGIC_MAX_HIT = 45
    const val STOMP_EVERY_ATTACKS = 6
    const val STOMP_MAX_HIT = 35
    const val ARENA_RADIUS = 15

    private val PROJECTILE =
        ProjectileConfig(
            startHeight = 130,
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

            val melee =
                ability("melee") {
                    anim("seq.npc_yama01_melee01")
                    hit {
                        damage(0..MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val fireMagic =
                ability("fire_magic") {
                    anim("seq.npc_yama01_magic01")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.vfx_yama_flaming_rock_projectile_01",
                                config = PROJECTILE,
                                hit =
                                    Effect.Hit(
                                        damage = Roll(0..MAGIC_MAX_HIT),
                                        type = Magic,
                                        spotanim = "spotanim.vfx_player_yama_magic_fire_impact01",
                                    ),
                            ),
                        )
                    )
                }

            val shadowMagic =
                ability("shadow_magic") {
                    anim("seq.npc_yama01_magic02")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.vfx_yama_shadow_spike_projectile_01",
                                config = PROJECTILE,
                                hit =
                                    Effect.Hit(
                                        damage = Roll(0..MAGIC_MAX_HIT),
                                        type = Magic,
                                        spotanim = "spotanim.vfx_player_yama_magic_shadow_impact01",
                                    ),
                            ),
                        )
                    )
                }

            val stomp =
                ability("stomp") {
                    anim("seq.npc_yama01_stomp01")
                    wait(2)
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Hit(damage = Roll(0..STOMP_MAX_HIT), type = Typeless),
                        )
                    )
                }

            fun PhaseBuilder.pattern() {
                weightedSelectorRandom {
                    +random(melee, weight = 2, requires = WithinMeleeRange)
                    +random(fireMagic, weight = 1)
                    +random(shadowMagic, weight = 1)
                }
                forceEveryAttacks(STOMP_EVERY_ATTACKS, STOMP_EVERY_ATTACKS, stomp)
            }

            phase(PHASE_OPENING) { pattern() }
            phase(PHASE_ENRAGED, entryHp = ENRAGE_HP, attackRate = ENRAGED_ATTACK_RATE) { pattern() }
        }
}

class Yama @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = YamaFight.spec()
}
