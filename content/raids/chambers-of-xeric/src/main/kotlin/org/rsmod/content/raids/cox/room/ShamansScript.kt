package org.rsmod.content.raids.cox.room

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ShamansScript
@Inject
constructor(
    deps: BossDeps,
    private val points: CoxDamagePoints,
    private val services: CoxRoomServices,
) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
        deps.extensionRegistry.register(ACID) { ctx -> acid(ctx.npc, ctx.target.coords) }
        deps.extensionRegistry.register(JUMP) { ctx -> jump(ctx.npc, ctx.target.coords) }
        deps.extensionRegistry.register(SPAWNS) { ctx -> spawns(ctx.target.coords) }
    }

    private fun acid(shaman: Npc, tile: CoordGrid) {
        services.lob(ACID_TRAVEL, shaman.coords.translate(1, 1), tile) {
            services.spotanimAt(ACID_SPLASH, tile)
            for (player in services.players) {
                if (player.coords.level == tile.level && player.coords.chebyshevDistance(tile) <= 1) {
                    val damage = services.random.of(0, ACID_MAX)
                    player.queueHit(shaman, 1, EngineHitType.Typeless, damage, deps.playerHitModifier)
                }
            }
        }
    }

    private fun jump(shaman: Npc, tile: CoordGrid) {
        shaman.anim(JUMP_SEQ)
        deps.worldQueues.add(JUMP_AIR_TICKS) {
            if (!shaman.isSlotAssigned || shaman.hitpoints <= 0) return@add
            PathingEntityCommon.telejump(shaman, deps.collision, tile)
            shaman.anim(LAND_SEQ)
            for (player in services.players) {
                if (player.coords.level == tile.level && player.coords.chebyshevDistance(tile) <= 1) {
                    val damage = services.random.of(0, JUMP_MAX)
                    player.queueHit(shaman, 1, EngineHitType.Typeless, damage, deps.playerHitModifier)
                }
            }
        }
    }

    private fun spawns(near: CoordGrid) {
        val type = ServerCacheManager.getNpc(SPAWN.asRSCM(RSCMType.NPC)) ?: return
        repeat(SPAWN_COUNT) {
            val spawn = Npc(type, services.random.of(near, 2))
            services.npcRepo.add(spawn, SPAWN_FUSE_TICKS + 1)
            deps.worldQueues.add(SPAWN_FUSE_TICKS) {
                if (!spawn.isSlotAssigned) return@add
                services.spotanimAt(SPAWN_EXPLODE, spawn.coords)
                for (player in services.players) {
                    if (player.coords.level == spawn.coords.level && player.coords.chebyshevDistance(spawn.coords) <= 1) {
                        val damage = services.random.of(0, SPAWN_MAX)
                        player.queueHit(1, EngineHitType.Typeless, damage, deps.playerHitModifier)
                    }
                }
                services.npcRepo.del(spawn, Int.MAX_VALUE)
            }
        }
    }

    override val spec: BossSpec =
        boss(*ShamansRoom.TYPES.toTypedArray()) {
            stats(attackRate = 4)
            val melee =
                ability("melee") {
                    melee("seq.shay_lizard_warrior_attack_melee", MeleeAttackType.Crush)
                }
            val spit =
                ability("spit") {
                    anim("seq.shay_lizard_warrior_attack_ranged")
                    projectile(
                        spotanim = "spotanim.lizardman_spit",
                        travel = "projanim.arrow",
                        hit =
                            Effect.Hit(
                                damage = scaledHit(),
                                type = HitType.Ranged,
                                onHit = Effect.Poison(POISON),
                            ),
                    )
                }
            val acid =
                ability("acid") {
                    anim("seq.shay_lizard_warrior_attack_ranged")
                    include(external(ACID))
                }
            val jump = ability("jump") { include(external(JUMP)) }
            val spawns =
                ability("spawns") {
                    anim("seq.shayzien_lizard_boss_minion_summon")
                    include(external(SPAWNS))
                }
            phase("fight") {
                weightedSelectorRandom {
                    +random(melee, weight = 4, requires = Condition.WithinMeleeRange)
                    +random(spit, weight = 4)
                    +random(acid, weight = 2, cooldown = 8)
                    +random(jump, weight = 1, cooldown = 15)
                    +random(spawns, weight = 1, cooldown = 20)
                }
            }
        }

    private companion object {
        const val ACID = "cox.shaman_acid"
        const val JUMP = "cox.shaman_jump"
        const val SPAWNS = "cox.shaman_spawns"
        const val ACID_TRAVEL = "spotanim.lizardshaman_spit_acid"
        const val ACID_SPLASH = "spotanim.lizardshaman_acid_splash"
        const val SPAWN = "npc.zeah_lizardshaman_spawn"
        const val SPAWN_EXPLODE = "spotanim.lizardshaman_spawn_explode"
        const val JUMP_SEQ = "seq.shayzien_lizard_boss_jump"
        const val LAND_SEQ = "seq.shayzien_lizard_boss_land"
        const val ACID_MAX = 39
        const val JUMP_MAX = 25
        const val SPAWN_MAX = 10
        const val SPAWN_COUNT = 3
        const val SPAWN_FUSE_TICKS = 6
        const val JUMP_AIR_TICKS = 3
        const val POISON = 12
    }
}
