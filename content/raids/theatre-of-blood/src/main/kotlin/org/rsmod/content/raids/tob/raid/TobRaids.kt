package org.rsmod.content.raids.tob.raid

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobParties
import org.rsmod.content.raids.tob.party.TobParty
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class TobRaids
@Inject
constructor(
    private val parties: TobParties,
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val worldQueues: WorldQueueList,
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val factories: Set<TobRoomFactory>,
) {
    private val raids = mutableMapOf<Int, TobRaid>()
    private var ticking = false

    fun of(party: TobParty): TobRaid? = raids[party.id]

    fun containing(player: Player): TobRaid? = raids.values.firstOrNull { player in it.insiders }

    fun at(coords: CoordGrid): TobRaid? = raids.values.firstOrNull { coords in it }

    fun start(party: TobParty, present: List<Player>): TobRaid? {
        if (of(party) != null) return null
        val region = regions.add(TobRaid.template()) ?: return null
        regions.protect(region)
        val raid = TobRaid(party, region, party.mode)
        raids[party.id] = raid
        raid.startedAt = clock.cycle
        raid.sizeAtStart = present.size
        party.started = true
        for (member in party.snapshot()) {
            if (member !in present) {
                parties.leave(member)
                member.mes("Your party has entered the Theatre without you.")
            }
        }
        for (member in present) enter(member, raid)
        startTicking()
        return raid
    }

    fun enter(player: Player, raid: TobRaid) {
        if (player !in raid.insiders) raid.insiders += player
        teleport(player, raid.arrival(raid.room))
    }

    fun leave(player: Player, raid: TobRaid, message: String? = null) {
        raid.insiders.removeAll { it === player }
        raid.downed.remove(player)
        parties.leave(player)
        teleport(player, LOBBY_EXIT)
        if (message != null) player.mes(message)
    }

    /** First barrier crossing in a fight room: builds the room's controller and starts the fight. */
    fun engage(raid: TobRaid) {
        if (raid.engaged || raid.roomCleared) return
        raid.engaged = true
        val room = raid.room
        val controller =
            factories.firstNotNullOfOrNull { it.create(raid, room) { clearRoom(raid, room) } }
        raid.controller = controller
        if (controller == null) {
            clearRoom(raid, room)
            return
        }
        controller.begin()
    }

    fun clearRoom(raid: TobRaid, room: TobRoom) {
        if (raid.room != room || raid.roomCleared) return
        raid.roomCleared = true
        for (player in raid.downed.toList()) revive(raid, player)
        for (player in raid.insiders) {
            player.mes("<col=ef1020>${room.label} has been defeated.</col>")
        }
    }

    fun advance(raid: TobRaid) {
        if (!raid.roomCleared) return
        val next = raid.room.next ?: return
        raid.controller?.destroy()
        raid.controller = null
        raid.room = next
        raid.engaged = false
        raid.roomCleared = !next.isFight
        for (member in raid.insiders.toList()) {
            teleport(member, raid.arrival(next))
        }
    }

    fun onDeath(raid: TobRaid, player: Player) {
        if (!raid.engaged) return
        raid.downed += player
        player.mes("<col=ff0000>You have fallen. Your team must finish the room.</col>")
        if (raid.alive.isEmpty()) wipe(raid)
    }

    fun respawnCoords(raid: TobRaid): CoordGrid = raid.arrival(raid.room)

    private fun revive(raid: TobRaid, player: Player) {
        raid.downed.remove(player)
        player.statHeal("stat.hitpoints", 0, 100)
        player.mes("Your team has cleared the room and you are revived.")
    }

    fun wipe(raid: TobRaid) {
        for (member in raid.insiders.toList()) {
            leave(member, raid, "Your whole team has fallen. The Theatre of Blood claims you.")
        }
        destroy(raid)
    }

    fun dissolve(party: TobParty) {
        val raid = of(party) ?: return
        for (member in raid.insiders.toList()) leave(member, raid, "Your party has disbanded.")
        destroy(raid)
    }

    private fun destroy(raid: TobRaid) {
        raids.remove(raid.party.id)
        raid.controller?.destroy()
        raid.controller = null
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

    private fun tick(raid: TobRaid) {
        for (member in raid.insiders.toList()) {
            if (!member.isOnline() || member.coords !in raid) {
                raid.insiders.removeAll { it === member }
                raid.downed.remove(member)
                parties.leave(member)
            }
        }
        raid.controller?.tick()
        if (raid.insiders.isEmpty()) {
            raid.emptyTicks++
            if (raid.emptyTicks >= EMPTY_TICKS) destroy(raid)
        } else {
            raid.emptyTicks = 0
            if (raid.engaged && raid.alive.isEmpty() && !raid.roomCleared) wipe(raid)
        }
    }

    private fun teleport(player: Player, coords: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, coords)
    }

    private fun Player.isOnline(): Boolean = players.any { it === this }

    companion object {
        val LOBBY_EXIT = CoordGrid(3674, 3219, 0)
        const val EMPTY_TICKS = 10
    }
}
