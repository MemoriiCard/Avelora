package org.rsmod.content.minigames.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.minigames.framework.MinigameProfile
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class PcService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val npcs: NpcRepository,
    private val interactions: AiPlayerInteractions,
) {
    val match = PcMatch()
    private val portals = arrayOfNulls<Npc>(PcMatch.PORTALS)
    private val pests = LinkedHashSet<Npc>()
    private var knight: Npc? = null
    private var pestTier = 1
    private val fallen = HashSet<Int>()

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun portalIndex(npc: Npc): Int = portals.indexOfFirst { it === npc }

    fun isPest(npc: Npc): Boolean = npc in pests

    fun isKnight(npc: Npc): Boolean = npc === knight

    fun toggleQueue(player: Player) {
        val id = id(player)
        if (match.isPlaying(id)) {
            player.mes("You're already in a game.")
            return
        }
        if (match.isQueued(id)) {
            apply(match.leave(id, clock.cycle))
            player.clearSoftTimer(TIMER)
            player.mes("You leave the Pest Control lander.")
            return
        }
        if (!match.join(id, clock.cycle)) {
            player.mes("A game is under way. The next lander leaves when it ends.")
            return
        }
        player.softTimer(TIMER, 1)
        player.mes(
            "You board the lander. It departs in ${match.ticksLeft(clock.cycle) * 6 / 10} " +
                "seconds, or sooner when it is full.",
        )
    }

    fun leave(player: Player, toOutpost: Boolean = true) {
        val events = match.leave(id(player), clock.cycle)
        player.clearSoftTimer(TIMER)
        if (toOutpost && PcMap.inArena(player.coords)) teleport(player, PcMap.OUTPOST)
        apply(events)
    }

    fun onPlayerDeath(player: Player) {
        fallen += id(player)
        player.clearSoftTimer(TIMER)
        apply(match.leave(id(player), clock.cycle))
    }

    fun respawnCoords(player: Player): CoordGrid? =
        if (fallen.remove(id(player))) PcMap.OUTPOST else null

    fun onNpcKilled(npc: Npc, hero: Player) {
        val index = portalIndex(npc)
        when {
            index >= 0 -> {
                portals[index] = null
                apply(match.portalKilled(index, id(hero), clock.cycle))
            }
            pests.remove(npc) -> match.pestKilled(id(hero))
        }
    }

    fun tick() {
        val cycle = clock.cycle
        apply(match.update(cycle, pests.size))
        if (match.phase != PcPhase.Playing) return
        if (cycle % SPAWN_INTERVAL == 0 && pests.size < MAX_PESTS) spawnPests(cycle)
        if (cycle % CHASE_INTERVAL == 0) chase()
    }

    private fun spawnPests(salt: Int) {
        for (index in 0 until PcMatch.PORTALS) {
            if (!match.isPortalAlive(index)) continue
            val type = ServerCacheManager.getNpc(PcNpcs.pest(pestTier, index + salt).asRSCM(RSCMType.NPC)) ?: continue
            val npc = Npc(type, PcMap.PEST_SPAWNS[index])
            npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            pests += npc
        }
    }

    private fun chase() {
        val squad = match.squad.mapNotNull { player(it) }
        if (squad.isEmpty()) return
        for (npc in pests) {
            if (!npc.isSlotAssigned) continue
            val target = squad.minBy { it.coords.chebyshevDistance(npc.coords) }
            npc.opPlayer2(target, interactions)
        }
    }

    private fun apply(events: List<PcEvent>) {
        for (event in events) {
            when (event) {
                is PcEvent.WaitExtended -> notify(match.waiting, event.reason)
                is PcEvent.GameStarted -> start(event)
                is PcEvent.PortalDown ->
                    notify(match.squad, "A portal has been destroyed. ${match.portalsAlive} remain.")
                is PcEvent.KnightHurt ->
                    if (event.hitpoints <= KNIGHT_WARNING) {
                        notify(match.squad, "The Void Knight is badly hurt! (${event.hitpoints} left)")
                    }
                is PcEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(event: PcEvent.GameStarted) {
        clearNpcs()
        val roster = event.players.mapNotNull { player(it) }
        val average = if (roster.isEmpty()) 3 else roster.map { it.appearance.combatLevel }.average().toInt()
        pestTier = PcNpcs.tier(average)
        spawnStatics(roster.size)
        roster.forEachIndexed { index, player ->
            teleport(player, PcMap.START[index % PcMap.START.size])
            player.mes("Defend the Void Knight! Destroy all four portals.")
        }
    }

    private fun spawnStatics(squadSize: Int) {
        val hitpoints = PcMatch.portalHitpoints(squadSize)
        for (index in 0 until PcMatch.PORTALS) {
            val type = ServerCacheManager.getNpc(PcNpcs.portal(index).asRSCM(RSCMType.NPC)) ?: continue
            val npc = Npc(type, PcMap.PORTALS[index])
            npc.baseHitpointsLvl = hitpoints
            npc.hitpoints = hitpoints
            npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            portals[index] = npc
        }
        ServerCacheManager.getNpc(PcNpcs.knight().asRSCM(RSCMType.NPC))?.let { type ->
            val npc = Npc(type, PcMap.KNIGHT)
            npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            knight = npc
        }
    }

    private fun clearNpcs() {
        for (npc in pests) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        pests.clear()
        for (index in portals.indices) {
            portals[index]?.takeIf { it.isSlotAssigned }?.let { npcs.del(it, Int.MAX_VALUE) }
            portals[index] = null
        }
        knight?.takeIf { it.isSlotAssigned }?.let { npcs.del(it, Int.MAX_VALUE) }
        knight = null
    }

    private fun finish(result: PcResult) {
        clearNpcs()
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val summary = describe(result)
        for (id in result.participation.keys) {
            val player = player(id) ?: continue
            player.clearSoftTimer(TIMER)
            val reward =
                PcRewards.reward(
                    kills = result.kills[id] ?: 0,
                    portalKills = result.portalKills[id] ?: 0,
                    won = result.won,
                    botShare = BOT_SHARE,
                    firstWinToday = result.won && MinigameProfile.claimFirstWin(player, day),
                )
            if (PcMap.inArena(player.coords)) teleport(player, PcMap.OUTPOST)
            player.mes(summary)
            if (reward != PcReward.NONE) {
                addPoints(player, reward.points)
                MinigameProfile.addTokens(player, reward.tokens)
                player.mes("You receive ${reward.points} Pest Control points and ${reward.tokens} Minigame Tokens.")
            } else {
                player.mes("You didn't contribute enough to earn a reward.")
            }
        }
    }

    private fun describe(result: PcResult): String =
        when (result.ending) {
            PcEnding.PortalsDestroyed -> "All portals destroyed. The Void Knights are saved!"
            PcEnding.KnightFell -> "The Void Knight has fallen."
            PcEnding.Timeout -> "Time ran out before the portals were destroyed."
            PcEnding.Abandoned -> "Everyone left the battle."
        }

    private fun addPoints(player: Player, amount: Int) {
        val total = (player.vars[POINTS].toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, POINTS, total)
    }

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) player(id)?.mes(message)
    }

    private fun player(id: Int): Player? = players[id]

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.pest_control"
        const val POINTS = "varp.pc_points"
        const val BOT_SHARE = 0.0
        private const val SPAWN_INTERVAL = 30
        private const val CHASE_INTERVAL = 4
        private const val MAX_PESTS = 32
        private const val KNIGHT_WARNING = 100
    }
}
