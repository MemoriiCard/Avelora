package org.rsmod.content.raids.cox.raid

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.content.raids.cox.layout.CoxLayoutGenerator
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.layout.CoxRoomType
import org.rsmod.content.raids.cox.party.CoxParties
import org.rsmod.content.raids.cox.party.CoxParty
import org.rsmod.content.raids.cox.party.CoxPartyScreens
import org.rsmod.content.raids.cox.party.CoxProgress
import org.rsmod.content.raids.cox.party.CoxScaling
import org.rsmod.content.raids.cox.party.coxBestTime
import org.rsmod.content.raids.cox.party.coxCmBestTime
import org.rsmod.content.raids.cox.party.coxCmKc
import org.rsmod.content.raids.cox.party.coxKc
import org.rsmod.content.raids.cox.party.raidsClientInDungeon
import org.rsmod.content.raids.cox.party.raidsClientPartyScore
import org.rsmod.content.raids.cox.party.raidsDied
import org.rsmod.content.raids.cox.party.raidsPlayerScore
import org.rsmod.content.raids.cox.party.raidsTimer
import org.rsmod.content.raids.cox.reward.CoxChest
import org.rsmod.content.raids.cox.reward.CoxLoot
import org.rsmod.content.raids.cox.reward.CoxRewardDelivery
import org.rsmod.content.raids.cox.reward.CoxRewardRoller
import org.rsmod.content.raids.cox.room.CoxRoomFactory
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.cox.storage.CoxItems
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class CoxRaids
@Inject
constructor(
    private val parties: CoxParties,
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val eventBus: EventBus,
    private val worldQueues: WorldQueueList,
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val roomServices: CoxRoomServices,
    private val delivery: CoxRewardDelivery,
    private val chest: CoxChest,
) {
    private val raids = mutableMapOf<Int, CoxRaid>()
    private val generator = CoxLayoutGenerator()
    private var ticking = false

    fun of(party: CoxParty): CoxRaid? = raids[party.id]

    fun containing(player: Player): CoxRaid? = raids.values.firstOrNull { player in it.insiders }

    fun at(coords: CoordGrid): CoxRaid? = raids.values.firstOrNull { coords in it }

    fun create(party: CoxParty): CoxRaid? {
        val layout = generator.generate(party.mapPool)
        val region = regions.add(CoxRaid.template(layout)) ?: return null
        regions.protect(region)
        val raid = CoxRaid(party, layout, region)
        raids[party.id] = raid
        startTicking()
        return raid
    }

    /** Builds a fresh layout for the leader, sending every other member back to the surface. */
    fun reload(raid: CoxRaid): CoxRaid? {
        val leader = raid.party.leader
        for (member in raid.insiders.toList()) {
            if (member !== leader) {
                exit(member, raid, "Your leader has reloaded the raid.")
            }
        }
        destroy(raid)
        val fresh = create(raid.party) ?: return null
        enter(leader, fresh)
        return fresh
    }

    fun enter(player: Player, raid: CoxRaid) {
        if (player !in raid.insiders) raid.insiders += player
        raid.points.putIfAbsent(player, 0)
        teleport(player, lobbyArrival(raid))
        player.raidsClientInDungeon = true
        player.raidsDied = false
        openInterfaces(player, raid)
        refreshPanels(raid.party)
    }

    fun exit(player: Player, raid: CoxRaid, message: String? = null) {
        raid.insiders.removeAll { it === player }
        claimLeftover(player, raid)
        releaseItems(player, raid)
        player.raidsClientInDungeon = false
        player.raidsTimer = 0
        player.raidsClientPartyScore = 0
        player.raidsPlayerScore = 0
        closeInterfaces(player)
        teleport(player, SURFACE_EXIT)
        if (message != null) player.mes(message)
    }

    fun start(raid: CoxRaid) {
        val party = raid.party
        raid.scaling = CoxScaling.snapshot(party)
        raid.challengeMode = party.challengeMode
        raid.startedAt = clock.cycle
        for (room in raid.layout.rooms) {
            val controller = CoxRoomFactory.create(raid, room, roomServices) ?: continue
            controller.pointSink = { player, amount -> addPoints(raid, player, amount) }
            raid.rooms += controller
            controller.spawn()
        }
        party.progress = CoxProgress.Upper
        party.advertisedAt = -1
        for (member in party.members.toList()) {
            if (member !in raid.insiders) {
                member.mes("Your party has started the raid without you.")
            }
        }
        party.members.retainAll { it in raid.insiders }
        val mode = if (raid.challengeMode) "Challenge Mode raid" else "${party.mapPool.label.lowercase()} raid"
        for (member in raid.insiders) {
            member.mes("The raid has begun! Your party has embarked on a $mode.")
        }
        refreshPanels(party)
    }

    fun reachedFloor(raid: CoxRaid, floorIndex: Int) {
        if (floorIndex <= raid.deepestFloor) return
        raid.deepestFloor = floorIndex
        val floors = raid.layout.floors.size
        raid.party.progress =
            when {
                floorIndex >= floors -> CoxProgress.Olm
                floors == 3 && floorIndex == 1 -> CoxProgress.Middle
                floors == 3 -> CoxProgress.Lower
                else -> CoxProgress.Middle
            }
        refreshPanels(raid.party)
    }

    fun addPoints(raid: CoxRaid, player: Player, amount: Int) {
        if (amount <= 0) return
        raid.points[player] = (raid.points[player] ?: 0) + amount
        raid.totalPoints += amount
        syncPoints(raid)
    }

    /** Ends the raid: stops the clock, awards the Challenge Mode bonus, and rolls the chest loot. */
    fun complete(raid: CoxRaid) {
        if (!raid.started || raid.completedAt >= 0) return
        raid.completedAt = clock.cycle
        val elapsed = raid.completedAt - raid.startedAt
        val underTarget =
            raid.challengeMode && elapsed <= CoxLoot.targetTicks(raid.scaling.partySize)
        if (underTarget) {
            raid.totalPoints += CoxLoot.UNDER_TARGET_BONUS * raid.insiders.size
            syncPoints(raid)
        }
        val random = roomServices.random
        val rolled =
            CoxRewardRoller.roll(
                points = raid.points,
                teamPoints = raid.totalPoints,
                challengeMode = raid.challengeMode,
                underTarget = underTarget,
                present = { it in raid.insiders },
                hasTablet = { delivery.owns(it, CoxLoot.TABLET) },
                hasJournal = { delivery.owns(it, CoxLoot.JOURNAL) },
                below = { random.of(it) },
            )
        raid.rewards.putAll(rolled.items)
        chest.reveal(raid)
        val duration = formatDuration(elapsed)
        for (member in raid.insiders) {
            recordCompletion(member, raid, elapsed)
            member.mes("<col=ef1020>Congratulations - your raid is complete!</col>")
            member.mes("Team size: ${raid.insiders.size} players. Duration: <col=ef1020>$duration</col>.")
            if (underTarget) member.mes("You beat the Challenge Mode target time!")
            member.mes("Open the ancient chest to claim your rewards.")
        }
        for ((winner, item) in rolled.uniques) {
            val line = "<col=ef20ff>${winner.displayName} found something special: ${delivery.displayName(item)}</col>"
            for (member in raid.insiders) member.mes(line)
        }
    }

    /** Pays out whatever [player] left unclaimed in the chest, straight into their bank. */
    fun claimLeftover(player: Player, raid: CoxRaid) {
        val loot = raid.rewards.remove(player) ?: return
        val bank = player.invMap.getOrPut("inv.bank")
        for (item in loot) delivery.give(player, item, bank)
        player.mes("Your unclaimed chest rewards have been sent to your bank.")
    }

    private fun recordCompletion(player: Player, raid: CoxRaid, elapsed: Int) {
        if (raid.challengeMode) {
            player.coxCmKc += 1
            if (player.coxCmBestTime == 0 || elapsed < player.coxCmBestTime) player.coxCmBestTime = elapsed
        } else {
            player.coxKc += 1
            if (player.coxBestTime == 0 || elapsed < player.coxBestTime) player.coxBestTime = elapsed
        }
    }

    private fun formatDuration(ticks: Int): String {
        val seconds = ticks * 6 / 10
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    fun onDeath(raid: CoxRaid, player: Player) {
        val personal = raid.points[player] ?: 0
        if (raid.totalPoints > 0 && personal * 100 < raid.totalPoints * LOW_POINTS_PERCENT) {
            val loss = raid.totalPoints * TEAM_DEATH_PENALTY_PERCENT / 100
            raid.totalPoints -= loss
        } else {
            val loss = personal * PERSONAL_DEATH_PENALTY_PERCENT / 100
            raid.points[player] = personal - loss
            raid.totalPoints -= loss
        }
        player.raidsDied = true
        syncPoints(raid)
        worldQueues.add(1) { if (player in raid.insiders) openInterfaces(player, raid) }
    }

    fun respawnCoords(raid: CoxRaid): CoordGrid {
        val floors = raid.layout.floors
        if (raid.deepestFloor == 0) return lobbyArrival(raid)
        val previous = floors[minOf(raid.deepestFloor, floors.size) - 1]
        return raid.coords(previous.end, END_ROOM_LANDING_X, END_ROOM_LANDING_Z)
    }

    fun lobbyArrival(raid: CoxRaid): CoordGrid =
        raid.coords(raid.layout.floors.first().start, LOBBY_ARRIVAL_X, LOBBY_ARRIVAL_Z)

    fun floorStartArrival(raid: CoxRaid, room: CoxRoom): CoordGrid =
        raid.coords(room, FLOOR_START_LANDING_X, FLOOR_START_LANDING_Z)

    fun endRoomArrival(raid: CoxRaid, room: CoxRoom): CoordGrid =
        if (room.type == CoxRoomType.OlmEntrance) {
            raid.coords(room, OLM_ENTRANCE_LANDING_X, OLM_ENTRANCE_LANDING_Z)
        } else {
            raid.coords(room, END_ROOM_LANDING_X, END_ROOM_LANDING_Z)
        }

    fun olmArrival(raid: CoxRaid): CoordGrid = raid.olmCoords(OLM_ARRIVAL)

    fun refreshPanels(party: CoxParty) {
        val raid = of(party)
        for (member in party.members) {
            CoxPartyScreens.writeClientState(member, party)
            if (raid != null && member in raid.insiders) {
                CoxPartyScreens.sendSidePanel(member, party)
            }
        }
    }

    fun dissolve(party: CoxParty) {
        val raid = of(party) ?: return
        for (member in raid.insiders.toList()) {
            exit(member, raid, "Your raiding party has been disbanded.")
        }
        destroy(raid)
    }

    /** Strips raid-only items and sends anything left in private storage to the player's bank. */
    private fun releaseItems(player: Player, raid: CoxRaid) {
        for (obj in player.inv.filterNotNull { true }.map { getInvObj(it).internalName }.distinct()) {
            if (CoxItems.isRaidOnly(obj)) player.invDel(player.inv, obj, count = Int.MAX_VALUE, strict = false)
        }
        val bank = player.invMap.getOrPut("inv.bank")
        for ((obj, count) in raid.storage.release(player)) {
            if (CoxItems.isRaidOnly(obj)) continue
            player.invAdd(bank, obj, count = count, strict = false)
        }
    }

    private fun openInterfaces(player: Player, raid: CoxRaid) {
        player.ifOpenOverlay(SIDE_PANEL, SIDE_PANEL_TARGET, eventBus)
        player.ifOpenOverlay(OVERLAY, OVERLAY_TARGET, eventBus)
        CoxPartyScreens.sendSidePanel(player, raid.party)
        syncPoints(raid)
    }

    private fun closeInterfaces(player: Player) {
        player.ifCloseOverlay(OVERLAY, eventBus)
        player.ifCloseOverlay(SIDE_PANEL, eventBus)
        player.ifOpenOverlay(JOURNAL, SIDE_PANEL_TARGET, eventBus)
    }

    private fun syncPoints(raid: CoxRaid) {
        for (member in raid.insiders) {
            member.raidsClientPartyScore = raid.totalPoints.coerceIn(0, MAX_CLIENT_SCORE)
            member.raidsPlayerScore = raid.points[member] ?: 0
        }
    }

    private fun teleport(player: Player, coords: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, coords)
    }

    private fun destroy(raid: CoxRaid) {
        raids.remove(raid.party.id)
        for (room in raid.rooms) room.destroy()
        raid.rooms.clear()
        regions.unprotect(raid.region)
        if (regions.isValid(raid.region)) {
            registry.unregister(raid.region)
        }
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        worldQueues.add(1) { tick() }
    }

    private fun tick() {
        for (raid in raids.values.toList()) {
            tick(raid)
        }
        if (raids.isEmpty()) {
            ticking = false
            return
        }
        worldQueues.add(1) { tick() }
    }

    private fun tick(raid: CoxRaid) {
        for (member in raid.insiders.toList()) {
            if (!member.isOnline() || member.coords !in raid) {
                removeInsider(raid, member)
            }
        }
        if (raid.started) {
            for (room in raid.rooms) room.tick()
        }
        if (raid.started && raid.completedAt < 0) {
            val elapsed = clock.cycle - raid.startedAt
            for (member in raid.insiders) {
                member.raidsTimer = elapsed.coerceAtMost(MAX_CLIENT_TIMER)
            }
        }
        if (raid.insiders.isEmpty()) {
            raid.emptyTicks++
            if (raid.started || raid.emptyTicks >= EMPTY_LOBBY_TICKS) {
                destroy(raid)
            }
        } else {
            raid.emptyTicks = 0
        }
    }

    private fun removeInsider(raid: CoxRaid, member: Player) {
        raid.insiders.removeAll { it === member }
        if (member.isOnline()) {
            claimLeftover(member, raid)
            releaseItems(member, raid)
            member.raidsClientInDungeon = false
            member.raidsTimer = 0
            closeInterfaces(member)
        }
        if (raid.started && parties.of(member) === raid.party) {
            parties.leave(member)
            CoxPartyScreens.clearClientState(member)
            refreshPanels(raid.party)
        }
    }

    private fun Player.isOnline(): Boolean = players.any { it === this }

    private companion object {
        const val SIDE_PANEL = "interface.raids_sidepanel"
        const val SIDE_PANEL_TARGET = "component.toplevel_osrs_stretch:side2"
        const val JOURNAL = "interface.side_journal"
        const val OVERLAY = "interface.raids_overlay"
        const val OVERLAY_TARGET = "component.toplevel_osrs_stretch:overlay_hud"

        val SURFACE_EXIT = CoordGrid(1233, 3572, 0)
        val OLM_ARRIVAL = CoordGrid(3233, 5722, 0)

        const val LOBBY_ARRIVAL_X = 3
        const val LOBBY_ARRIVAL_Z = 5
        const val FLOOR_START_LANDING_X = 15
        const val FLOOR_START_LANDING_Z = 14
        const val END_ROOM_LANDING_X = 13
        const val END_ROOM_LANDING_Z = 16
        const val OLM_ENTRANCE_LANDING_X = 11
        const val OLM_ENTRANCE_LANDING_Z = 21

        const val PERSONAL_DEATH_PENALTY_PERCENT = 40
        const val TEAM_DEATH_PENALTY_PERCENT = 5
        const val LOW_POINTS_PERCENT = 5

        const val EMPTY_LOBBY_TICKS = 100
        const val MAX_CLIENT_SCORE = Int.MAX_VALUE
        const val MAX_CLIENT_TIMER = 0xFFFF
    }
}
