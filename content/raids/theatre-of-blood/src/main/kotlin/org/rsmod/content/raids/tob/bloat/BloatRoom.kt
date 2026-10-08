package org.rsmod.content.raids.tob.bloat

import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.boss.TobBossRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class BloatRoom(
    raid: TobRaid,
    services: CoxRoomServices,
    onCleared: () -> Unit,
) : TobBossRoom(raid, TobRoom.Bloat, services, onCleared) {
    private class Flesh(val tile: CoordGrid, val landsAt: Int)

    private var bloat: Npc? = null
    private var walking = false
    private var phaseEndsAt = 0
    private var corner = 0
    private val falling = mutableListOf<Flesh>()

    val isWalking: Boolean
        get() = walking

    override fun begin() {
        val hp = TobScaling.hitpoints(BASE_HP, teamSize, mode)
        bloat = spawn("npc.tob_bloat$suffix", START, hp, stats = BLOAT_STAT)
        startWalking()
        tell("The Pestilent Bloat stirs.")
    }

    override fun tick() {
        val boss = bloat ?: return
        if (clock >= phaseEndsAt) {
            if (walking) stopWalking(boss) else startWalking()
        }
        if (walking) {
            keepMoving(boss)
            if (clock % BloatRules.FLY_RATE == 0) flies(boss)
        }
        val percent = boss.hitpoints * 100 / boss.baseHitpointsLvl.coerceAtLeast(1)
        if (BloatRules.fleshActive(percent, walking, mode) && clock % BloatRules.FLESH_RATE == 0) {
            dropFlesh()
        }
        landFlesh()
    }

    fun incoming(damage: Int): Int = damage * BloatRules.incomingPercent(walking) / 100

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc !== bloat) return
        falling.clear()
        tell("<col=ef1020>The Pestilent Bloat has been defeated!</col>")
        finish()
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === bloat) bloat = null
    }

    private fun startWalking() {
        val boss = bloat ?: return
        if (phaseEndsAt != 0) stomp(boss)
        walking = true
        phaseEndsAt = clock + BloatRules.walkTicks(services.random.of(9))
        boss.anim("seq.tob_bloat_walk")
        boss.walk(corner())
    }

    private fun stopWalking(boss: Npc) {
        walking = false
        phaseEndsAt = clock + BloatRules.stopTicks(services.random.of(9))
        boss.abortRoute()
        boss.anim("seq.tob_bloat_sleep")
    }

    private fun keepMoving(boss: Npc) {
        if (boss.coords == corner()) {
            corner = BloatRules.nextCorner(corner)
            boss.walk(corner())
        } else if (!boss.routeDestination.isNotEmpty()) {
            boss.walk(corner())
        }
    }

    private fun corner(): CoordGrid = world(BloatRules.RING[corner])

    private fun stomp(boss: Npc) {
        for (player in playersInRoom()) {
            if (BloatRules.covers(boss.coords, player.coords)) {
                hurt(player, BloatRules.stomp(services.random.of(41), mode))
            }
        }
    }

    private fun flies(boss: Npc) {
        for (player in playersInRoom()) {
            if (distanceToBloat(boss, player.coords) <= BloatRules.FLY_RANGE) {
                hurt(player, TobScaling.damage(services.random.of(0, BloatRules.FLY_MAX), mode))
            }
        }
    }

    private fun dropFlesh() {
        val players = playersInRoom()
        val tile =
            if (players.isNotEmpty() && services.random.of(3) != 0) {
                players.random().coords
            } else {
                world(
                    CoordGrid(
                        BloatRules.ARENA_X.first + services.random.of(BloatRules.ARENA_X.count()),
                        BloatRules.ARENA_Z.first + services.random.of(BloatRules.ARENA_Z.count()),
                        0,
                    )
                )
            }
        services.spotanimAt("spotanim.tob_bloat_flies_large", tile)
        falling += Flesh(tile, clock + BloatRules.FLESH_DELAY)
    }

    private fun landFlesh() {
        val due = falling.filter { it.landsAt <= clock }
        if (due.isEmpty()) return
        falling.removeAll(due.toSet())
        for (flesh in due) {
            services.spotanimAt("spotanim.tob_bloat_blood_splat", flesh.tile)
            for (player in playersInRoom()) {
                if (player.coords == flesh.tile) {
                    hurt(player, BloatRules.flesh(services.random.of(21), mode))
                }
            }
        }
    }

    private fun distanceToBloat(boss: Npc, from: CoordGrid): Int {
        val dx = maxOf(boss.coords.x - from.x, 0, from.x - (boss.coords.x + BloatRules.SIZE - 1))
        val dz = maxOf(boss.coords.z - from.z, 0, from.z - (boss.coords.z + BloatRules.SIZE - 1))
        return maxOf(dx, dz)
    }

    private companion object {
        const val BASE_HP = 2000
        const val BLOAT_STAT = 120
        val START = CoordGrid(3293, 4445, 0)
    }
}
