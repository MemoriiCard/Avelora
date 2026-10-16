package org.rsmod.content.minigames.tempoross

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

private var Player.tetheredVar by intVarBit(TemporossService.TETHERED)
private var Player.pointsVar by intVarBit(TemporossService.POINTS)
private var Player.permitsVar by intVarBit(TemporossService.PERMITS)
private var Player.killsVar by intVarp(TemporossService.KILLS)

@Singleton
class TemporossService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val locRepo: LocRepository,
    private val npcs: NpcRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val events: EventBus,
) {
    val round = TemporossRound { random.of(it) }

    private val inside = HashSet<Int>()
    private val spots = ArrayList<Npc>()
    private val fireNpcs = arrayOfNulls<Npc>(TemporossRound.FIRE_SLOTS)
    private var countdown = -1
    private var lastTick = -1
    private var nextDeck = 0

    val waiting: Boolean
        get() = countdown >= 0

    fun isInside(player: Player): Boolean = player.slotId in inside

    fun board(player: Player) {
        if (player.fishingLvl < MIN_FISHING) {
            player.mes("You need a Fishing level of $MIN_FISHING to board the ship.")
            return
        }
        if (round.active) {
            player.mes("A storm is already raging over the cove. Wait for the next ship.")
            return
        }
        if (isInside(player)) return
        inside += player.slotId
        player.pointsVar = 0
        player.tetheredVar = 0
        player.ifOpenOverlay(OVERLAY, events)
        player.softTimer(TIMER, 1)
        teleport(player, TemporossMap.DECKS[nextDeck++ % TemporossMap.DECKS.size])
        if (countdown < 0) {
            countdown = COUNTDOWN_TICKS
            player.mes("The ship will set sail in 30 seconds.")
        }
    }

    fun leave(player: Player, toLobby: Boolean = true) {
        if (!inside.remove(player.slotId)) return
        round.forfeit(player.slotId)
        player.clearSoftTimer(TIMER)
        player.ifCloseOverlay(OVERLAY, events)
        player.tetheredVar = 0
        player.pointsVar = 0
        if (toLobby) teleport(player, TemporossMap.LOBBY_EXIT)
        if (inside.isEmpty()) shutdown()
    }

    fun tick() {
        val cycle = clock.cycle
        if (cycle == lastTick) return
        lastTick = cycle
        if (countdown > 0 && --countdown == 0) begin(cycle)
        if (round.active) apply(round.tick(cycle))
        if (round.active || round.phase == TemporossPhase.Won || round.phase == TemporossPhase.Lost) refreshOverlays()
        if (round.phase == TemporossPhase.Won) finish(won = true)
        if (round.phase == TemporossPhase.Lost) finish(won = false)
    }

    fun award(player: Player, amount: Int) {
        round.award(player.slotId, amount)
    }

    fun tether(player: Player, site: Int): Boolean =
        round.tether(player.slotId, site).also { if (it) player.tetheredVar = 1 }

    fun repair(player: Player, site: Int): Boolean =
        round.repair(player.slotId, site).also { if (it) syncSite(site) }

    fun douse(player: Player, slot: Int): Boolean =
        round.douse(player.slotId, slot).also { if (it) removeFire(slot) }

    fun fireSlot(npc: Npc): Int? = fireNpcs.indices.firstOrNull { fireNpcs[it] === npc }

    fun permitsOf(player: Player): Int = player.permitsVar

    fun spendPermits(player: Player, amount: Int) {
        player.permitsVar = (permitsOf(player) - amount).coerceAtLeast(0)
    }

    fun spiritPool(): Boolean = round.phase == TemporossPhase.Essence

    private fun begin(cycle: Int) {
        countdown = -1
        round.start(inside.size, cycle)
        spawnSpots(essence = false)
        for (site in 0 until TemporossRound.SITES) syncSite(site)
        broadcast("Tempoross has awoken! Catch harpoonfish and feed the cannons before the storm swallows the cove.")
    }

    private fun apply(list: List<TemporossEvent>) {
        for (event in list) {
            when (event) {
                is TemporossEvent.EnergyPhase -> {
                    spawnSpots(essence = false)
                    broadcast("Tempoross has recovered its energy and the storm returns!")
                }
                TemporossEvent.WaveWarning -> broadcast("A colossal wave closes in. Tether yourself to a mast or totem pole!")
                is TemporossEvent.WaveHit -> waveHit(event)
                TemporossEvent.WaveEnd -> forEachInside { it.tetheredVar = 0 }
                is TemporossEvent.Lightning -> igniteFire(event.slot)
                is TemporossEvent.TorrentStart -> {
                    swapCannon(event.cannon, disabled = true)
                    broadcast("A torrent of water slams into cannon ${event.cannon + 1}!")
                }
                is TemporossEvent.TorrentEnd -> swapCannon(event.cannon, disabled = false)
                TemporossEvent.Won,
                TemporossEvent.Lost -> Unit
            }
        }
    }

    private fun enterEssence() {
        spawnSpots(essence = true)
        clearFires()
        broadcast("Tempoross is exhausted! Fish the spirit pools to finish it off.")
    }

    fun deposit(player: Player, cooked: Boolean): Boolean {
        val before = round.phase
        val ok = round.deposit(player.slotId, cooked)
        if (ok && before == TemporossPhase.Energy && round.phase == TemporossPhase.Essence) enterEssence()
        return ok
    }

    private fun waveHit(event: TemporossEvent.WaveHit) {
        clearFires()
        for (site in event.broken) syncSite(site)
        forEachInside { player ->
            if (player.slotId in event.tethered) {
                player.mes("The wave crashes over you but your rope holds.")
            } else {
                player.mes("The wave sweeps over the deck and washes your supplies away!")
                loseSupplies(player)
            }
        }
    }

    private fun loseSupplies(player: Player) {
        val inv = player.inv
        for (slot in inv.mapSlots { _, obj -> obj != null && AT_RISK.any { obj.isType(it) } }) {
            inv[slot] = null
        }
    }

    private fun finish(won: Boolean) {
        val points = round.pointsSnapshot()
        val everyone = inside.toList()
        for (id in everyone) {
            val player = players[id] ?: continue
            if (won) {
                val earned = points[id] ?: 0
                val permits = TemporossLoot.permits(earned) { random.of(it) }
                player.killsVar += 1
                if (permits > 0) {
                    player.permitsVar = (permitsOf(player) + permits).coerceAtMost(TemporossLoot.MAX_PERMITS)
                    player.mes("Tempoross has been subdued! You earned $earned points and $permits reward permits.")
                } else {
                    player.mes("Tempoross has been subdued, but you needed ${TemporossLoot.FIRST_PERMIT_POINTS} points for a reward permit.")
                }
            } else {
                player.mes("The storm tears the ship apart and you are washed away. Your points are lost.")
            }
        }
        for (id in everyone) players[id]?.let { leave(it) }
        inside.clear()
        shutdown()
    }

    private fun shutdown() {
        round.reset()
        countdown = -1
        nextDeck = 0
        removeSpots()
        clearFires()
        for (site in 0 until TemporossRound.SITES) syncSite(site)
        for (cannon in 0 until TemporossRound.CANNONS) swapCannon(cannon, disabled = false)
    }

    private fun spawnSpots(essence: Boolean) {
        removeSpots()
        val symbols =
            if (essence) {
                listOf(SPIRIT_SPOT, SPIRIT_SPOT)
            } else {
                listOf(SPOT_SOUTH, if (random.of(SPECIAL_ODDS) == 0) SPOT_SPECIAL else SPOT_NORTH)
            }
        for ((index, coords) in TemporossMap.SPOTS.withIndex()) {
            spawnNpc(symbols[index], coords)?.let { spots += it }
        }
    }

    private fun removeSpots() {
        for (npc in spots) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        spots.clear()
    }

    private fun igniteFire(slot: Int) {
        fireNpcs[slot] = spawnNpc(FIRE_NPC, TemporossMap.FIRES[slot])
        broadcast("Lightning strikes the ship and sets it ablaze!")
    }

    private fun removeFire(slot: Int) {
        fireNpcs[slot]?.takeIf { it.isSlotAssigned }?.let { npcs.del(it, Int.MAX_VALUE) }
        fireNpcs[slot] = null
    }

    private fun clearFires() {
        for (slot in fireNpcs.indices) removeFire(slot)
    }

    private fun spawnNpc(symbol: String, coords: CoordGrid): Npc? {
        val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: return null
        return Npc(type, coords).also { npcs.add(it, Int.MAX_VALUE) }
    }

    private fun syncSite(site: Int) {
        val (whole, broken) = TemporossMap.SITE_LOCS[site]
        swap(TemporossMap.SITES[site], if (round.siteBroken(site)) broken else whole)
    }

    private fun swapCannon(cannon: Int, disabled: Boolean) {
        val symbol = "loc.tempoross_cannon_${cannon + 1}" + if (disabled) "_disabled" else ""
        swap(TemporossMap.CANNONS[cannon], symbol)
    }

    private fun swap(coords: CoordGrid, symbol: String) {
        val old = locRepo.findExact(coords, LocShape.CentrepieceStraight) ?: return
        locRepo.del(old, Int.MAX_VALUE)
        locRepo.add(old.coords, symbol, Int.MAX_VALUE, old.angle, old.shape)
    }

    private fun refreshOverlays() {
        val script = "clientscript.[clientscript,tempoross_hud_update]".asRSCM(RSCMType.CLIENTSCRIPT)
        val essence = if (round.phase == TemporossPhase.Essence) 1 else 0
        forEachInside { player ->
            val tether = round.tetherOf(player.slotId)
            val lines = IntArray(TemporossRound.SITES) { if (tether == it) 0 else 1 }
            val fires = IntArray(TemporossRound.FIRE_SLOTS) { if (round.fireAt(it)) 1 else 0 }
            player.pointsVar = round.pointsOf(player.slotId)
            player.runClientScript(
                script,
                essence,
                round.pointsOf(player.slotId),
                round.energy,
                round.essence,
                round.maxEnergy,
                round.maxEssence,
                round.storm.toInt(),
                lines[0],
                lines[1],
                lines[2],
                lines[3],
                fires[0],
                fires[1],
                fires[2],
                fires[3],
            )
        }
    }

    private fun broadcast(message: String) = forEachInside { it.mes(message) }

    private fun forEachInside(action: (Player) -> Unit) {
        for (id in inside.toList()) {
            val player = players[id]
            if (player == null) {
                inside.remove(id)
                round.forfeit(id)
                continue
            }
            action(player)
        }
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.tempoross"
        const val OVERLAY = "interface.tempoross_hud"
        const val TETHERED = "varbit.tempoross_tethered"
        const val POINTS = "varbit.tempoross_current_points"
        const val PERMITS = "varbit.tempoross_rewardpermits"
        const val KILLS = "varp.total_tempoross_kills"
        const val MIN_FISHING = 35
        const val COUNTDOWN_TICKS = 50
        const val SPECIAL_ODDS = 4
        const val SPOT_SOUTH = "npc.tempoross_harpoonfish_fishingspot_south"
        const val SPOT_NORTH = "npc.tempoross_harpoonfish_fishingspot_north"
        const val SPOT_SPECIAL = "npc.tempoross_harpoonfish_fishingspot_special"
        const val SPIRIT_SPOT = "npc.tempoross_p2_fishingspot"
        const val FIRE_NPC = "npc.tempoross_fire_target_npc"

        val AT_RISK =
            listOf(
                "obj.hammer",
                "obj.rope",
                "obj.bucket_empty",
                "obj.bucket_water",
                "obj.tempoross_raw_harpoonfish",
                "obj.tempoross_harpoonfish",
                "obj.tempoross_crystallised_harpoonfish",
            )
    }
}
