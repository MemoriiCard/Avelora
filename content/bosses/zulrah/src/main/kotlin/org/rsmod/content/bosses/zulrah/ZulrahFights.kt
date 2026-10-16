package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class ZulrahFight(val player: Player, val session: InstanceSession) {
    var phase: ZulrahPhase = ZulrahRotations.OPENING
    var rotation: List<ZulrahPhase> = emptyList()
    var phaseIndex: Int = -1
    var actionIndex: Int = 0
    var openingCloud: Int = 0
    var over: Boolean = false
    val clouds: MutableMap<CoordGrid, Int> = HashMap()
    val snakelings: MutableList<Npc> = ArrayList()
}

@Singleton
internal class ZulrahFights
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val fights = IdentityHashMap<Npc, ZulrahFight>()

    fun fightFor(npc: Npc): ZulrahFight? = fights[npc]

    fun spawn(player: Player, session: InstanceSession) {
        val coords = resolve(session, ZulrahShrine.position(ZulrahPosition.Middle)) ?: return
        val zulrah = Npc(ZulrahForm.Serpentine.npc, coords)
        deps.npcRepo.add(zulrah, Int.MAX_VALUE)
        zulrah.respawns = false
        zulrah.apRangeOverride = ZULRAH_ATTACK_RANGE
        zulrah.apRequiresLineOfSight = false
        instances.attachNpc(session.id, zulrah)
        val fight = ZulrahFight(player, session)
        fights[zulrah] = fight
        zulrah.anim(SPAWN_SEQ)
        deps.worldQueues.add(SPAWN_TICKS) {
            if (fight.over || !zulrah.isSlotAssigned) return@add
            zulrah.apPlayer2(player, aiPlayerInteractions)
            scheduleCloudTick(zulrah, fight)
        }
    }

    fun end(npc: Npc): ZulrahFight? {
        val fight = fights.remove(npc) ?: return null
        fight.over = true
        for (snakeling in fight.snakelings) {
            if (snakeling.isSlotAssigned) deps.npcRepo.del(snakeling, Int.MAX_VALUE)
        }
        fight.snakelings.clear()
        return fight
    }

    fun endFor(player: Player) {
        fights.entries.filter { it.value.player === player }.forEach { end(it.key) }
    }

    fun nextAction(zulrah: Npc, target: Player) {
        val fight = fights[zulrah] ?: return
        if (fight.over) return
        val action = fight.phase.actions.getOrNull(fight.actionIndex)
        if (action == null) {
            dive(zulrah, fight)
            return
        }
        fight.actionIndex++
        when (action) {
            ZulrahAction.Ranged -> deps.runAbility(zulrah, target, RANGED_ABILITY)
            ZulrahAction.Magic -> deps.runAbility(zulrah, target, MAGIC_ABILITY)
            ZulrahAction.Tanzanite -> {
                val ability = if (deps.random.of(TANZANITE_MAGIC_ODDS) == 0) RANGED_ABILITY else MAGIC_ABILITY
                deps.runAbility(zulrah, target, ability)
            }
            ZulrahAction.Melee -> tailWhip(zulrah, fight, target)
            ZulrahAction.Clouds -> cloudBarrage(zulrah, fight, target)
            ZulrahAction.Snakeling -> snakelingOrb(zulrah, fight)
        }
    }

    private fun dive(zulrah: Npc, fight: ZulrahFight) {
        val next = advance(fight)
        val dest = resolve(fight.session, ZulrahShrine.position(next.position)) ?: return
        val encounter = deps.encounter(zulrah)
        encounter.invulnerable = true
        deps.suppressAttacks(zulrah, DIVE_TICKS)
        zulrah.anim(SINK_SEQ)
        deps.worldQueues.add(SUBMERGED_TICKS) {
            if (fight.over || !zulrah.isSlotAssigned) return@add
            PathingEntityCommon.telejump(zulrah, deps.collision, dest)
            transform(zulrah, next.form)
            zulrah.anim(EMERGE_SEQ)
            deps.worldQueues.add(DIVE_TICKS - SUBMERGED_TICKS) {
                if (fight.over || !zulrah.isSlotAssigned) return@add
                encounter.invulnerable = false
            }
        }
    }

    private fun advance(fight: ZulrahFight): ZulrahPhase {
        fight.phaseIndex++
        if (fight.phaseIndex >= fight.rotation.size) {
            fight.rotation = ZulrahRotations.ROTATIONS[deps.random.of(ZulrahRotations.ROTATIONS.size)]
            fight.phaseIndex = 0
        }
        fight.phase = fight.rotation[fight.phaseIndex]
        fight.actionIndex = 0
        return fight.phase
    }

    private fun transform(zulrah: Npc, form: ZulrahForm) {
        if (form == ZulrahForm.Serpentine) {
            zulrah.resetTransmog()
            return
        }
        val type = ServerCacheManager.getNpc(form.npc.asRSCM(RSCMType.NPC)) ?: return
        zulrah.transmog(type, Int.MAX_VALUE)
    }

    private fun tailWhip(zulrah: Npc, fight: ZulrahFight, target: Player) {
        val tile = target.coords
        deps.suppressAttacks(zulrah, TAIL_TICKS)
        zulrah.faceSquare(tile)
        deps.worldQueues.add(TAIL_STARE_TICKS) {
            if (fight.over || !zulrah.isSlotAssigned) return@add
            val left = tile.x < zulrah.coords.x + zulrah.size / 2
            zulrah.anim(if (left) TAIL_LEFT_SEQ else TAIL_RIGHT_SEQ)
            deps.worldQueues.add(TAIL_LAND_TICKS) {
                if (fight.over || !zulrah.isSlotAssigned) return@add
                if (target.coords == tile && target.hitpoints > 0) deps.runAbility(zulrah, target, TAIL_ABILITY)
            }
        }
    }

    private fun cloudBarrage(zulrah: Npc, fight: ZulrahFight, target: Player) {
        zulrah.anim(BARRAGE_SEQ)
        val targets =
            if (fight.phase === ZulrahRotations.OPENING) {
                val first = fight.openingCloud
                fight.openingCloud += CLOUDS_PER_BARRAGE
                ZulrahShrine.OPENING_CLOUDS.drop(first).take(CLOUDS_PER_BARRAGE).mapNotNull { resolve(fight.session, it) }
            } else {
                val random = resolve(fight.session, ZulrahShrine.OPENING_CLOUDS.random())
                listOfNotNull(target.coords.translate(-1, -1), random)
            }
        for (tile in targets) {
            lob(zulrah, tile, CLOUD_ORB) {
                deps.locRepo.add(tile, CLOUD_LOC, CLOUD_TICKS, LocAngle.North, LocShape.CentrepieceStraight)
                fight.clouds[tile] = deps.mapClock.cycle + CLOUD_TICKS
            }
        }
    }

    private fun snakelingOrb(zulrah: Npc, fight: ZulrahFight) {
        zulrah.anim(BARRAGE_SEQ)
        val tile = resolve(fight.session, ZulrahShrine.SNAKELING_SPOTS.random()) ?: return
        lob(zulrah, tile, SNAKELING_ORB) {
            val magic = deps.random.of(2) == 0
            val type = if (magic) MAGIC_SNAKELING else MELEE_SNAKELING
            val snakeling = deps.spawnOwnedNpc(zulrah, type, tile, SNAKELING_LIFESPAN) ?: return@lob
            instances.attachNpc(fight.session.id, snakeling)
            fight.snakelings.removeIf { !it.isSlotAssigned }
            fight.snakelings += snakeling
            snakeling.anim(SNAKELING_SPAWN_SEQ)
            if (magic) {
                snakeling.apRangeOverride = SNAKELING_MAGIC_RANGE
                snakeling.apPlayer2(fight.player, aiPlayerInteractions)
            } else {
                snakeling.opPlayer2(fight.player, aiPlayerInteractions)
            }
        }
    }

    private fun lob(zulrah: Npc, tile: CoordGrid, spotanim: String, onLand: () -> Unit) {
        val fight = fights[zulrah] ?: return
        deps.bossProjectile(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            src = zulrah.coords.translate(zulrah.size / 2, zulrah.size / 2),
            target = tile,
            startHeight = ORB_START_HEIGHT,
            endHeight = 0,
            delay = ORB_DELAY,
            travel = ORB_TRAVEL,
            curve = ORB_CURVE,
        )
        deps.worldQueues.add(ORB_LAND_TICKS) {
            if (!fight.over && zulrah.isSlotAssigned) onLand()
        }
    }

    private fun scheduleCloudTick(zulrah: Npc, fight: ZulrahFight) {
        deps.worldQueues.add(1) {
            if (fight.over || !zulrah.isSlotAssigned) return@add
            val now = deps.mapClock.cycle
            fight.clouds.values.removeIf { it <= now }
            val player = fight.player
            val inCloud = fight.clouds.keys.any { sw -> player.coords.x - sw.x in 0..2 && player.coords.z - sw.z in 0..2 }
            if (inCloud && player.hitpoints > 0) deps.runAbility(zulrah, player, CLOUD_DAMAGE_ABILITY)
            scheduleCloudTick(zulrah, fight)
        }
    }

    private fun resolve(session: InstanceSession, coords: CoordGrid): CoordGrid? =
        instances.resolveCoord(session, coords)

    private companion object {
        const val ZULRAH_ATTACK_RANGE = 20
        const val SPAWN_TICKS = 3
        const val DIVE_TICKS = 6
        const val SUBMERGED_TICKS = 3
        const val TAIL_TICKS = 6
        const val TAIL_STARE_TICKS = 3
        const val TAIL_LAND_TICKS = 1
        const val TANZANITE_MAGIC_ODDS = 4
        const val CLOUDS_PER_BARRAGE = 2
        const val CLOUD_TICKS = 30
        const val SNAKELING_LIFESPAN = 67
        const val SNAKELING_MAGIC_RANGE = 8
        const val ORB_START_HEIGHT = 85
        const val ORB_DELAY = 30
        const val ORB_TRAVEL = 60
        const val ORB_CURVE = 30
        const val ORB_LAND_TICKS = 3

        const val SPAWN_SEQ = "seq.snakeboss_spawn"
        const val SINK_SEQ = "seq.snakeboss_sinkfast"
        const val EMERGE_SEQ = "seq.snakeboss_emergefast"
        const val BARRAGE_SEQ = "seq.snakeboss_attack_acidx3"
        const val TAIL_LEFT_SEQ = "seq.snakeboss_attack_tail_left"
        const val TAIL_RIGHT_SEQ = "seq.snakeboss_attack_tail_right"
        const val SNAKELING_SPAWN_SEQ = "seq.snakeboss_pet_spawn"
        const val CLOUD_ORB = "spotanim.snakeboss_double_orb"
        const val SNAKELING_ORB = "spotanim.snakeboss_egg"
        const val CLOUD_LOC = "loc.snakeboss_poisoncloud"
        const val MELEE_SNAKELING = "npc.snakeboss_minion_melee"
        const val MAGIC_SNAKELING = "npc.snakeboss_minion_magic"
    }
}
