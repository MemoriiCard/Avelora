package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.content.raids.toa.invocation.ToaInvocations
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

    fun onDeath(raid: ToaRaid, player: Player) {
        raid.downed += player
        player.mes("<col=ff0000>You have fallen.</col>")
        if (raid.alive.isNotEmpty()) return
        raid.downed.clear()
        if (raid.attemptsLeft == Int.MAX_VALUE) {
            for (member in raid.insiders) member.mes("Your whole team has fallen. You regroup in the Nexus.")
            return
        }
        raid.attemptsLeft--
        if (raid.attemptsLeft > 0) {
            for (member in raid.insiders) {
                member.mes("Your whole team has fallen. Attempts remaining: ${raid.attemptsLeft}.")
            }
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
    }
}
