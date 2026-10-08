package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.content.raids.toa.invocation.ToaInvocations
import org.rsmod.content.raids.toa.layout.ToaPath
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.party.ToaParties
import org.rsmod.content.raids.toa.party.ToaParty
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class ToaRaids
@Inject
constructor(
    private val parties: ToaParties,
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val worldQueues: WorldQueueList,
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val factories: Set<ToaRoomFactory>,
) {
    private val raids = mutableMapOf<Int, ToaRaid>()
    private var ticking = false

    fun of(party: ToaParty): ToaRaid? = raids[party.id]

    fun containing(player: Player): ToaRaid? = raids.values.firstOrNull { player in it.insiders }

    fun at(coords: CoordGrid): ToaRaid? = raids.values.firstOrNull { coords in it }

    fun start(party: ToaParty, present: List<Player>): ToaRaid? {
        if (of(party) != null) return null
        val region = regions.add(ToaRaid.template()) ?: return null
        regions.protect(region)
        val raid = ToaRaid(party, region, party.invocations, present.size)
        raids[party.id] = raid
        raid.startedAt = clock.cycle
        party.started = true
        for (member in party.snapshot()) {
            if (member !in present) {
                parties.leave(member)
                member.mes("Your party has entered the tombs without you.")
            }
        }
        for (member in present) enter(member, raid)
        startTicking()
        return raid
    }

    fun enter(player: Player, raid: ToaRaid) {
        if (player !in raid.insiders) raid.insiders += player
        teleport(player, raid.arrival(raid.room))
        player.mes("Raid level: <col=ef1020>${raid.raidLevel}</col> (${raid.mode.label}).")
    }

    fun leave(player: Player, raid: ToaRaid, message: String? = null) {
        raid.insiders.removeAll { it === player }
        raid.downed.remove(player)
        parties.leave(player)
        teleport(player, LOBBY_EXIT)
        if (message != null) player.mes(message)
    }

    fun enterPath(raid: ToaRaid, path: ToaPath) {
        if (raid.room != ToaRoom.Nexus) return
        if (path in raid.clearedPaths) {
            for (member in raid.insiders) member.mes("The ${path.label} path has already been cleared.")
            return
        }
        raid.path = path
        moveTo(raid, path.puzzle)
        beginRoom(raid)
    }

    fun continuePath(raid: ToaRaid) {
        val path = raid.path ?: return
        if (raid.room != path.puzzle || !raid.roomCleared) return
        raid.controller?.destroy()
        raid.controller = null
        moveTo(raid, path.boss)
        worldQueues.add(BOSS_DELAY) {
            if (raid.room == path.boss && !raid.engaged) beginRoom(raid)
        }
    }

    fun enterWardens(raid: ToaRaid): Boolean {
        if (raid.room != ToaRoom.Nexus) return false
        if (raid.clearedPaths.size < ToaPath.entries.size) return false
        raid.path = null
        moveTo(raid, ToaRoom.WardensOne)
        worldQueues.add(BOSS_DELAY) {
            if (raid.room == ToaRoom.WardensOne && !raid.engaged) beginRoom(raid)
        }
        return true
    }

    fun clearRoom(raid: ToaRaid, room: ToaRoom) {
        if (raid.room != room || raid.roomCleared) return
        raid.roomCleared = true
        for (player in raid.downed.toList()) revive(raid, player)
        if (room == ToaRoom.WardensOne) {
            for (member in raid.insiders) member.mes("<col=ef1020>The first Warden has fallen. The throne awaits.</col>")
            worldQueues.add(RETURN_DELAY) {
                if (raid.room != ToaRoom.WardensOne) return@add
                raid.controller?.destroy()
                raid.controller = null
                moveTo(raid, ToaRoom.WardensTwo)
                worldQueues.add(BOSS_DELAY) {
                    if (raid.room == ToaRoom.WardensTwo && !raid.engaged) beginRoom(raid)
                }
            }
            return
        }
        if (room == ToaRoom.WardensTwo) {
            completeRaid(raid)
            return
        }
        val path = raid.path ?: return
        for (member in raid.insiders) member.mes("<col=ef1020>${room.label} has been cleared.</col>")
        if (room == path.boss) {
            raid.clearedPaths += path
            worldQueues.add(RETURN_DELAY) { if (raid.room == path.boss) returnToNexus(raid) }
        }
    }

    private fun completeRaid(raid: ToaRaid) {
        raid.completed = true
        for (member in raid.insiders) {
            member.mes("<col=ef1020>Congratulations! You have conquered the Tombs of Amascut.</col>")
        }
        worldQueues.add(RETURN_DELAY) {
            raid.controller?.destroy()
            raid.controller = null
            for (member in raid.insiders.toList()) leave(member, raid)
            destroy(raid)
        }
    }

    fun returnToNexus(raid: ToaRaid) {
        raid.controller?.destroy()
        raid.controller = null
        raid.path = null
        for (player in raid.downed.toList()) revive(raid, player)
        moveTo(raid, ToaRoom.Nexus)
        raid.engaged = false
        raid.roomCleared = false
        for (member in raid.insiders) {
            member.mes("Paths cleared: ${raid.clearedPaths.size}/${ToaPath.entries.size}.")
        }
    }

    private fun moveTo(raid: ToaRaid, room: ToaRoom) {
        raid.room = room
        raid.engaged = false
        raid.roomCleared = false
        for (member in raid.insiders.toList()) teleport(member, raid.arrival(room))
    }

    private fun beginRoom(raid: ToaRaid) {
        val room = raid.room
        raid.engaged = true
        raid.roomCleared = false
        val controller =
            factories.firstNotNullOfOrNull { it.create(raid, room) { clearRoom(raid, room) } }
        raid.controller = controller
        if (controller == null) {
            clearRoom(raid, room)
            return
        }
        controller.begin()
    }

    private fun restartRoom(raid: ToaRaid) {
        raid.controller?.destroy()
        raid.controller = null
        for (player in raid.downed.toList()) revive(raid, player)
        for (member in raid.insiders.toList()) teleport(member, raid.arrival(raid.room))
        beginRoom(raid)
    }

    private fun revive(raid: ToaRaid, player: Player) {
        raid.downed.remove(player)
        player.statHeal("stat.hitpoints", 0, 100)
    }

    fun onDeath(raid: ToaRaid, player: Player) {
        if (!raid.engaged || raid.roomCleared) return
        raid.downed += player
        player.mes("<col=ff0000>You have fallen. Your team must finish the room.</col>")
        if (raid.alive.isNotEmpty()) return
        raid.downed.clear()
        if (raid.attemptsLeft == Int.MAX_VALUE) {
            for (member in raid.insiders) member.mes("Your whole team has fallen. The room resets.")
            restartRoom(raid)
            return
        }
        raid.attemptsLeft--
        if (raid.attemptsLeft > 0) {
            for (member in raid.insiders) {
                member.mes("Your whole team has fallen. Attempts remaining: ${raid.attemptsLeft}.")
            }
            restartRoom(raid)
            return
        }
        for (member in raid.insiders.toList()) {
            leave(member, raid, "You have run out of attempts. The tombs cast you out.")
        }
        destroy(raid)
    }

    fun respawnCoords(raid: ToaRaid): CoordGrid = raid.arrival(raid.room)

    fun dissolve(party: ToaParty) {
        val raid = of(party) ?: return
        for (member in raid.insiders.toList()) leave(member, raid, "Your party has disbanded.")
        destroy(raid)
    }

    private fun destroy(raid: ToaRaid) {
        raid.controller?.destroy()
        raid.controller = null
        raids.remove(raid.party.id)
        parties.disband(raid.party)
        regions.unprotect(raid.region)
        if (regions.isValid(raid.region)) registry.unregister(raid.region)
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        worldQueues.add(1) { tick() }
    }

    private fun tick() {
        for (raid in raids.values.toList()) tick(raid)
        if (raids.isEmpty()) {
            ticking = false
            return
        }
        worldQueues.add(1) { tick() }
    }

    private fun tick(raid: ToaRaid) {
        for (member in raid.insiders.toList()) {
            if (!member.isOnline() || member.coords !in raid) {
                raid.insiders.removeAll { it === member }
                raid.downed.remove(member)
                parties.leave(member)
            }
        }
        raid.controller?.tick()
        checkTimeLimit(raid)
        if (raid.insiders.isEmpty()) {
            raid.emptyTicks++
            if (raid.emptyTicks >= EMPTY_TICKS) destroy(raid)
        } else {
            raid.emptyTicks = 0
        }
    }

    private fun checkTimeLimit(raid: ToaRaid) {
        if (raid.timeExpired) return
        val limit = ToaInvocations.timeLimit(raid.invocations) ?: return
        if (clock.cycle - raid.startedAt < limit.minutes * TICKS_PER_MINUTE) return
        raid.timeExpired = true
        raid.levelPenalty = limit.failPenalty
        for (member in raid.insiders) {
            member.mes(
                "<col=ff0000>You ran out of time. The raid level drops by ${limit.failPenalty} " +
                    "to ${raid.raidLevel}.</col>"
            )
        }
    }

    private fun teleport(player: Player, coords: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, coords)
    }

    private fun Player.isOnline(): Boolean = players.any { it === this }

    companion object {
        val LOBBY_EXIT = CoordGrid(3295, 2788, 0)
        const val EMPTY_TICKS = 10
        const val TICKS_PER_MINUTE = 100
        const val BOSS_DELAY = 8
        const val RETURN_DELAY = 10
    }
}
