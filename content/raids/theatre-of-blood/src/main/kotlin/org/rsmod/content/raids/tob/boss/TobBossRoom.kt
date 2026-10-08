package org.rsmod.content.raids.tob.boss

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.content.raids.tob.raid.TobRoomController
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

abstract class TobBossRoom(
    val raid: TobRaid,
    val room: TobRoom,
    protected val services: CoxRoomServices,
    private val onCleared: () -> Unit,
) : TobRoomController {
    protected val npcs = mutableListOf<Npc>()
    private var destroying = false

    private var done = false

    override val cleared: Boolean
        get() = done

    protected val clock: Int
        get() = services.cycle

    protected val mode: TobMode
        get() = raid.mode

    protected val teamSize: Int
        get() = raid.sizeAtStart.coerceIn(TobScaling.MIN_PARTY, TobScaling.MAX_PARTY)

    protected val suffix: String
        get() =
            when (mode) {
                TobMode.Entry -> "_story"
                TobMode.Normal -> ""
                TobMode.Hard -> "_hard"
            }

    protected open fun onNpcKilled(npc: Npc, hero: Player) {}

    protected open fun onNpcRemoved(npc: Npc) {}

    fun owns(npc: Npc): Boolean = npcs.any { it === npc }

    fun killed(npc: Npc, hero: Player) {
        if (!destroying && owns(npc)) onNpcKilled(npc, hero)
    }

    fun removed(npc: Npc) {
        if (destroying || !owns(npc)) return
        npcs.removeAll { it === npc }
        onNpcRemoved(npc)
    }

    protected fun finish() {
        if (done) return
        done = true
        onCleared()
    }

    override fun destroy() {
        destroying = true
        for (npc in npcs) {
            if (npc.isSlotAssigned) services.npcRepo.del(npc, Int.MAX_VALUE)
        }
        npcs.clear()
    }

    fun playersInRoom(): List<Player> =
        raid.alive.filter { it.hitpoints > 0 && raid.roomAt(it.coords) === room }

    protected fun world(source: CoordGrid): CoordGrid = raid.coords(room, source)

    protected fun spawn(
        type: String,
        source: CoordGrid,
        hitpoints: Int,
        stats: Int = 1,
    ): Npc {
        val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: error("No npc: $type")
        val npc = Npc(npcType, world(source))
        npc.baseHitpointsLvl = hitpoints
        npc.hitpoints = hitpoints
        npc.baseAttackLvl = stats
        npc.attackLvl = stats
        npc.baseStrengthLvl = stats
        npc.strengthLvl = stats
        npc.baseDefenceLvl = stats
        npc.defenceLvl = stats
        npc.baseMagicLvl = stats
        npc.magicLvl = stats
        npc.baseRangedLvl = stats
        npc.rangedLvl = stats
        services.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npcs += npc
        return npc
    }

    protected fun transmog(npc: Npc, name: String) {
        val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    protected fun remove(npc: Npc) {
        npcs.removeAll { it === npc }
        if (npc.isSlotAssigned) services.npcRepo.del(npc, Int.MAX_VALUE)
    }

    protected fun heal(npc: Npc, amount: Int) {
        npc.hitpoints = (npc.hitpoints + amount).coerceAtMost(npc.baseHitpointsLvl)
    }

    protected fun hurt(player: Player, damage: Int) {
        if (damage <= 0 || player.hitpoints <= 0) return
        player.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
    }

    protected fun nearestPlayer(from: CoordGrid): Player? =
        playersInRoom().minByOrNull { it.coords.chebyshevDistance(from) }

    protected fun tell(message: String) {
        for (player in playersInRoom()) player.mes(message)
    }
}
