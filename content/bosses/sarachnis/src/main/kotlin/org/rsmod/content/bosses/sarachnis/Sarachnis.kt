package org.rsmod.content.bosses.sarachnis

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object SarachnisFight {
    const val BOSS = "npc.sarachnis"
    const val MELEE_SPAWN = "npc.sarachnis_melee_spawn"
    const val MAGE_SPAWN = "npc.sarachnis_mage_spawn"

    const val PHASE_OPENING = "opening"
    const val PHASE_MELEE_SPAWN = "melee_spawn"
    const val PHASE_MAGE_SPAWN = "mage_spawn"

    const val MELEE_SPAWN_HP = 0.66
    const val MAGE_SPAWN_HP = 0.33

    const val ATTACK_RATE = 4
    const val MELEE_MAX_HIT = 31
    const val RANGED_MAX_HIT = 31
    const val WEB_MAX_HIT = 10
    const val WEB_EVERY_ATTACKS = 4
    const val WEB_FREEZE_TICKS = 6
    const val ARENA_RADIUS = 15
    const val SPAWN_RADIUS = 4

    private val RANGED_PROJECTILE =
        ProjectileConfig(
            startHeight = 110,
            endHeight = 60,
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
                    anim("seq.hosdun_spider_boss_attack_melee")
                    hit {
                        damage(0..MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val ranged =
                ability("ranged") {
                    anim("seq.hosdun_spider_boss_attack_ranged")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.sarachnis_rangeproj",
                                config = RANGED_PROJECTILE,
                                hit = Effect.Hit(damage = Roll(0..RANGED_MAX_HIT), type = Ranged),
                            ),
                        )
                    )
                }

            val web =
                ability("web") {
                    anim("seq.hosdun_spider_boss_attack_ranged")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.sarachnis_web_proj",
                                config = RANGED_PROJECTILE,
                                hit = Effect.Hit(damage = Roll(0..WEB_MAX_HIT), type = Typeless),
                            ),
                        )
                    )
                    freeze(WEB_FREEZE_TICKS)
                }

            val summonMelee =
                ability("summon_melee") {
                    summon(MELEE_SPAWN, radius = SPAWN_RADIUS, duration = Int.MAX_VALUE, owned = true)
                }

            val summonMage =
                ability("summon_mage") {
                    summon(MAGE_SPAWN, radius = SPAWN_RADIUS, duration = Int.MAX_VALUE, owned = true)
                }

            fun PhaseBuilder.attackPattern() {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(WEB_EVERY_ATTACKS, WEB_EVERY_ATTACKS, web)
            }

            phase(PHASE_OPENING) { attackPattern() }
            phase(PHASE_MELEE_SPAWN, entryHp = MELEE_SPAWN_HP) {
                entry = summonMelee.name
                attackPattern()
            }
            phase(PHASE_MAGE_SPAWN, entryHp = MAGE_SPAWN_HP) {
                entry = summonMage.name
                attackPattern()
            }
        }
}

class Sarachnis @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec = SarachnisFight.spec()
}
