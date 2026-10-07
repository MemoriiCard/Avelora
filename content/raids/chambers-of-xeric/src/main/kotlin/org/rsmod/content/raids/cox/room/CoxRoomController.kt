package org.rsmod.content.raids.cox.room

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.raids.cox.layout.CoxCell
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Runs one room of a raid: spawns its NPCs once the raid starts, wakes them when a player walks
 * in, drives the room's scripted mechanics every tick and reports when the room is cleared.
 */
abstract class CoxRoomController(
    val raid: CoxRaid,
    val room: CoxRoom,
    protected val services: CoxRoomServices,
) {
    val npcs = mutableListOf<Npc>()
    private val required = mutableListOf<Npc>()
    private val rangedNpcs = mutableListOf<Npc>()

    var cleared = false
        private set

    var engaged = false
        private set

    private var destroying = false

    /** Template variant: 0 exits to the left of the entrance, 1 straight on, 2 to the right. */
    val variant: Int
        get() = room.variantOffsetX / CoxCell.SIZE

    open val blocksExit: Boolean = true

    open val pointsPerDamage: Int = DEFAULT_POINTS_PER_DAMAGE

    abstract fun spawn()

    protected open fun onEngage(first: Player) {}

    protected open fun onTick() {}

    protected open fun onNpcRemoved(npc: Npc) {}

    protected open fun onCleared() {}

    fun tick() {
        if (cleared) return
        if (!engaged) {
            val first = playersInRoom().firstOrNull() ?: return
            engaged = true
            onEngage(first)
            aggroAll()
        }
        onTick()
    }

    fun owns(npc: Npc): Boolean = npcs.any { it === npc }

    fun npcRemoved(npc: Npc) {
        if (destroying || !owns(npc)) return
        npcs.removeAll { it === npc }
        rangedNpcs.removeAll { it === npc }
        val wasRequired = required.removeAll { it === npc }
        onNpcRemoved(npc)
        if (wasRequired && required.isEmpty()) clear()
    }

    fun clear() {
        if (cleared) return
        cleared = true
        onCleared()
        for (player in playersInRoom()) {
            player.mes("The path ahead is now clear.")
        }
    }

    fun destroy() {
        destroying = true
        for (npc in npcs) {
            if (npc.isSlotAssigned) services.npcRepo.del(npc, Int.MAX_VALUE)
        }
        npcs.clear()
        required.clear()
    }

    fun playersInRoom(): List<Player> =
        raid.insiders.filter { it.hitpoints > 0 && raid.roomAt(it.coords) === room }

    protected fun <T> pick(left: T, straight: T, right: T): T =
        when (variant) {
            0 -> left
            1 -> straight
            else -> right
        }

    protected fun local(localX: Int, localZ: Int): CoordGrid = raid.coords(room, localX, localZ)

    protected fun spawnNpc(
        type: String,
        centreX: Int,
        centreZ: Int,
        stats: CoxNpcStats?,
        required: Boolean = true,
        ranged: Boolean = false,
        hitpoints: Int? = null,
    ): Npc {
        val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: error("No npc: $type")
        val tile = standTile(local(centreX, centreZ), npcType.size)
        return spawnAt(type, tile, stats, required, ranged, hitpoints)
    }

    protected fun spawnAt(
        type: String,
        tile: CoordGrid,
        stats: CoxNpcStats?,
        required: Boolean = true,
        ranged: Boolean = false,
        hitpoints: Int? = null,
    ): Npc {
        val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: error("No npc: $type")
        val npc = Npc(npcType, tile)
        stats?.applyTo(npc, raid.scaling, hitpoints)
        services.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npcs += npc
        if (required) this.required += npc
        if (ranged) rangedNpcs += npc
        return npc
    }

    protected fun transmog(npc: Npc, name: String) {
        val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    protected fun findLocs(internal: String): List<LocInfo> {
        val id = internal.asRSCM(RSCMType.LOC)
        val a = local(0, 0)
        val b = local(CoxCell.SIZE - 1, CoxCell.SIZE - 1)
        val southWest = CoordGrid(minOf(a.x, b.x), minOf(a.z, b.z), a.level)
        val zones = CoxCell.SIZE / ZONE_SIZE
        return buildList {
            for (zx in 0 until zones) {
                for (zz in 0 until zones) {
                    val zone = ZoneKey.from(southWest.translate(zx * ZONE_SIZE, zz * ZONE_SIZE))
                    addAll(services.boss.locRepo.findAll(zone).filter { it.id == id })
                }
            }
        }
    }

    /** The south-west tile that puts an npc of [size] closest to [centre] without overlapping walls. */
    protected fun standTile(centre: CoordGrid, size: Int): CoordGrid {
        val origin = centre.translate(-(size - 1) / 2, -(size - 1) / 2)
        for (radius in 0..MAX_STAND_SEARCH) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) != radius) continue
                    val candidate = origin.translate(dx, dz)
                    if (canStand(candidate, size)) return candidate
                }
            }
        }
        return origin
    }

    protected fun canStand(origin: CoordGrid, size: Int): Boolean {
        for (dx in 0 until size) {
            for (dz in 0 until size) {
                val tile = origin.translate(dx, dz)
                if (raid.roomAt(tile) !== room) return false
                if (services.collision.isWalkBlocked(tile)) return false
            }
        }
        return true
    }

    protected fun aggroAll() {
        val targets = playersInRoom()
        if (targets.isEmpty()) return
        for (npc in npcs) {
            if (npc.isInCombat() || !npc.isValidTarget() || npc.ignoreCombatInteractions) continue
            if (npc !in required && npc !in rangedNpcs) continue
            engage(npc, targets.minBy { it.coords.chebyshevDistance(npc.coords) })
        }
    }

    protected fun engage(npc: Npc, target: Player) {
        if (rangedNpcs.any { it === npc }) {
            npc.apPlayer2(target, services.ai)
        } else {
            npc.opPlayer2(target, services.ai)
        }
    }

    protected fun nearestPlayer(npc: Npc): Player? =
        playersInRoom().minByOrNull { it.coords.chebyshevDistance(npc.coords) }

    protected fun randomPlayer(): Player? = playersInRoom().randomOrNull()

    companion object {
        const val DEFAULT_POINTS_PER_DAMAGE = 4
        private const val MAX_STAND_SEARCH = 8
        private const val ZONE_SIZE = 8
    }
}
