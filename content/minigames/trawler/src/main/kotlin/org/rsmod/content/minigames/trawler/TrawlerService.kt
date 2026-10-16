package org.rsmod.content.minigames.trawler

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
internal class TrawlerService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val objs: ObjRepository,
) {
    val round = TrawlerRound { random.of(it) }

    private val inside = HashSet<Int>()
    private var countdown = -1
    private var lastTick = -1
    private var hullSyncDirty = true

    fun isInside(player: Player): Boolean = player.slotId in inside

    val active: Boolean
        get() = round.phase == TrawlerPhase.Trawling

    fun board(player: Player) {
        if (isInside(player)) return
        if (active) {
            player.mes("A trawl is already under way. Wait for the next boat.")
            return
        }
        inside += player.slotId
        player.softTimer(TIMER, 1)
        syncHulls(player)
        PathingEntityCommon.telejump(player, collision, DECK)
        if (countdown < 0) {
            countdown = COUNTDOWN_TICKS
            player.mes("The boat will sail in one minute.")
        }
    }

    fun leave(player: Player, toDock: Boolean = true) {
        if (!inside.remove(player.slotId)) return
        round.forfeit(player.slotId)
        player.clearSoftTimer(TIMER)
        clearHulls(player)
        if (toDock) PathingEntityCommon.telejump(player, collision, DOCK)
        if (inside.isEmpty()) shutdown()
    }

    fun tick() {
        val cycle = clock.cycle
        if (cycle == lastTick) return
        lastTick = cycle
        if (countdown > 0 && --countdown == 0) begin()
        if (active) apply(round.tick())
        if (active && cycle % REPORT_EVERY == 0) {
            broadcast("The hold is ${round.waterPercent}% full of water.")
        }
        if (round.phase == TrawlerPhase.Sunk) finish(sunk = true)
        if (round.phase == TrawlerPhase.Done) finish(sunk = false)
    }

    fun patch(player: Player, hull: Int): Boolean =
        round.patch(player.slotId, hull).also { if (it) forEachInside { p -> syncHulls(p) } }

    fun bail(player: Player): Boolean = round.bail(player.slotId)

    fun repairNet(player: Player, net: Int): Boolean = round.repairNet(player.slotId, net)

    fun syncHulls(player: Player) {
        for (hull in 0 until TrawlerRound.HULLS) {
            player.setHull(hull, if (round.isLeaking(hull)) HULL_LEAK else HULL_DEFAULT)
        }
    }

    private fun clearHulls(player: Player) {
        for (hull in 0 until TrawlerRound.HULLS) player.setHull(hull, HULL_DEFAULT)
    }

    private fun begin() {
        countdown = -1
        round.start(inside.size)
        forEachInside { syncHulls(it) }
        broadcast("The trawl begins. Keep the nets mended and the hull dry.")
    }

    private fun apply(events: List<TrawlerEvent>) {
        for (event in events) {
            when (event) {
                is TrawlerEvent.Leak -> {
                    forEachInside { syncHulls(it) }
                    broadcast("A leak has sprung in the hull.")
                }
                is TrawlerEvent.NetTorn -> broadcast("A net has torn. No fish are being caught.")
                TrawlerEvent.Sunk,
                TrawlerEvent.TimeUp -> Unit
            }
        }
    }

    private fun finish(sunk: Boolean) {
        val everyone = inside.toList()
        for (id in everyone) {
            val player = players[id] ?: continue
            if (sunk) {
                player.mes("The boat has sunk! You swim to shore.")
            } else {
                collect(player, id)
            }
        }
        for (id in everyone) players[id]?.let { leave(it) }
        inside.clear()
        shutdown()
    }

    private fun collect(player: Player, slot: Int) {
        val points = round.pointsOf(slot)
        val fishing = player.fishingLvl
        if (points < TrawlerRound.MIN_POINTS) {
            player.mes("You didn't contribute enough ($points points) to earn a share of the catch.")
            return
        }
        if (fishing < TrawlerLoot.MIN_FISHING) {
            player.mes("You need level ${TrawlerLoot.MIN_FISHING} Fishing to receive a share of the catch.")
            return
        }
        val rewardInv = player.invMap.getOrPut(REWARD_INV)
        val caught = HashMap<String, Int>()
        var xp = 0.0
        repeat(round.fishFor(slot)) {
            val fish = TrawlerLoot.pick(fishing) { random.of(it) } ?: return@repeat
            caught.merge(fish.obj, 1, Int::plus)
            xp += fish.xp
            repeat(TrawlerLoot.junkCount { random.of(it) }) {
                val junk = TrawlerLoot.junk[random.of(TrawlerLoot.junk.size)]
                caught.merge(junk, 1, Int::plus)
            }
        }
        for ((obj, count) in caught) {
            player.invAddOrDrop(objs, obj, count, coords = DOCK, inv = rewardInv)
        }
        TrawlerLoot.anglerPiece({ owns(player, it) }) { random.of(it) }?.let {
            player.invAddOrDrop(objs, it, 1, coords = DOCK, inv = rewardInv)
            player.mes("The sea has given up a piece of an angler's outfit.")
        }
        val petOdds = if (points >= TrawlerRound.MAX_POINTS) TrawlerLoot.PET_ODDS_FULL else TrawlerLoot.PET_ODDS
        if (random.of(petOdds) == 0) {
            player.invAddOrDrop(objs, TrawlerLoot.PET, 1, coords = DOCK, inv = rewardInv)
            player.mes("A heron lands on the dock beside you.")
        }
        if (xp > 0) player.statAdvance("stat.fishing", xp)
        player.mes("The trawl is over. Your catch is waiting in the net on the dock.")
    }

    private fun owns(player: Player, obj: String): Boolean =
        player.inv.contains(obj) || obj in player.worn || player.invMap.getOrPut(REWARD_INV).contains(obj)

    private fun shutdown() {
        round.reset()
        countdown = -1
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

    internal companion object {
        const val TIMER = "timer.trawler"
        const val REWARD_INV = "inv.trawler_rewardinv"
        const val COUNTDOWN_TICKS = 100
        const val REPORT_EVERY = 100
        const val HULL_DEFAULT = 0
        const val HULL_LEAK = 2
        val DECK = CoordGrid(1886, 4824, 1)
        val DOCK = CoordGrid(2677, 3170, 1)
    }
}
