package org.rsmod.content.raids.toa.boss

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.party.ToaScaling
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoomController
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

private var Player.protectMelee by intVarBit("varbit.prayer_protectfrommelee")
private var Player.protectMissiles by intVarBit("varbit.prayer_protectfrommissiles")
private var Player.protectMagic by intVarBit("varbit.prayer_protectfrommagic")

abstract class ToaBossRoom(
    val raid: ToaRaid,
    val room: ToaRoom,
    protected val services: CoxRoomServices,
    private val onCleared: () -> Unit,
) : ToaRoomController {
    protected val npcs = mutableListOf<Npc>()
    private var destroying = false
    private var done = false

    override val cleared: Boolean
        get() = done

    protected val clock: Int
        get() = services.cycle

    protected val level: Int
        get() = raid.raidLevel

    protected val invocations: Set<ToaInvocation>
        get() = raid.invocations

    protected val teamSize: Int
        get() = raid.sizeAtStart.coerceIn(ToaScaling.MIN_PARTY, ToaScaling.MAX_PARTY)

    protected fun scaledHp(base: Int): Int = ToaScaling.hitpoints(base, teamSize, level)

    protected fun scaledDamage(base: Int): Int = ToaScaling.damage(base, level)

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

    protected open fun inside(coords: CoordGrid): Boolean = raid.roomAt(coords) === room

    fun playersInRoom(): List<Player> =
        raid.alive.filter { it.hitpoints > 0 && inside(it.coords) }

    protected fun world(source: CoordGrid): CoordGrid = raid.coords(room, source)

    protected fun spawn(type: String, source: CoordGrid, hitpoints: Int, stats: Int = 1): Npc {
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
        val drain = ToaCombat.prayerDrain(damage, invocations)
        if (drain > 0) player.statSub("stat.prayer", drain, 0)
    }

    protected fun hurtStyled(player: Player, style: ToaStyle, damage: Int) {
        val protected =
            when (style) {
                ToaStyle.Melee -> player.protectMelee > 0
                ToaStyle.Ranged -> player.protectMissiles > 0
                ToaStyle.Magic -> player.protectMagic > 0
            }
        hurt(player, ToaCombat.afterPrayer(damage, protected, invocations))
    }

    protected fun nearestPlayer(from: CoordGrid): Player? =
        playersInRoom().minByOrNull { it.coords.chebyshevDistance(from) }

    protected fun chase(npc: Npc, hitMax: Int, rate: Int, reach: Int = 1) {
        val target = nearestPlayer(npc.coords) ?: return
        val gap = npc.coords.chebyshevDistance(target.coords)
        if (gap <= reach) {
            if (clock % rate == 0) hurtStyled(target, ToaStyle.Melee, services.random.of(1, hitMax))
        } else if (clock % 2 == 0) {
            npc.walk(target.coords)
        }
    }

    protected fun tell(message: String) {
        for (player in playersInRoom()) player.mes(message)
    }
}
