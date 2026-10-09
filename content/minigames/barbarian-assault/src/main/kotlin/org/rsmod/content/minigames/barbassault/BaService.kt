package org.rsmod.content.minigames.barbassault

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
class BaService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val npcs: NpcRepository,
    private val interactions: AiPlayerInteractions,
) {
    val match = BaMatch()
    private val monsters = LinkedHashSet<Npc>()
    private val fallen = HashSet<Int>()

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun isMonster(npc: Npc): Boolean = npc in monsters

    fun toggleQueue(player: Player) {
        val id = id(player)
        if (match.isPlaying(id)) {
            player.mes("You're already in a run.")
            return
        }
        if (match.isQueued(id)) {
            apply(match.leave(id, clock.cycle))
            player.clearSoftTimer(TIMER)
            player.mes("You leave the Barbarian Assault queue.")
            return
        }
        if (!match.join(id, clock.cycle)) {
            player.mes("A run is already under way. Try again in a minute.")
            return
        }
        player.softTimer(TIMER, 1)
        player.mes(
            "You join the Barbarian Assault queue. The run starts in " +
                "${match.ticksLeft(clock.cycle) * 6 / 10} seconds, or sooner with a full squad.",
        )
    }

    fun leave(player: Player, toOutpost: Boolean = true) {
        val events = match.leave(id(player), clock.cycle)
        player.clearSoftTimer(TIMER)
        if (toOutpost && BaMap.inArena(player.coords)) teleport(player, BaMap.OUTPOST)
        apply(events)
    }

    fun onPlayerDeath(player: Player) {
        fallen += id(player)
        player.clearSoftTimer(TIMER)
        apply(match.leave(id(player), clock.cycle))
    }

    fun respawnCoords(player: Player): CoordGrid? =
        if (fallen.remove(id(player))) BaMap.OUTPOST else null

    fun onMonsterKilled(npc: Npc, hero: Player) {
        if (!monsters.remove(npc)) return
        apply(match.monsterKilled(id(hero), clock.cycle))
    }

    fun tick() {
        val cycle = clock.cycle
        apply(match.update(cycle))
        if (match.phase == BaPhase.Wave && cycle % CHASE_INTERVAL == 0) chase()
    }

    private fun chase() {
        val squad = match.squad.mapNotNull { player(it) }
        if (squad.isEmpty()) return
        for (npc in monsters) {
            if (!npc.isSlotAssigned) continue
            val target = squad.minBy { it.coords.chebyshevDistance(npc.coords) }
            npc.opPlayer2(target, interactions)
        }
    }

    private fun apply(events: List<BaEvent>) {
        for (event in events) {
            when (event) {
                is BaEvent.WaitExtended -> notify(match.waiting, event.reason)
                is BaEvent.GameStarted -> start(event)
                is BaEvent.WaveStarted -> spawnWave(event)
                is BaEvent.WaveCleared -> notify(match.squad, "Wave ${event.wave} cleared.")
                is BaEvent.PlayerDown -> clearIfEmpty()
                is BaEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(event: BaEvent.GameStarted) {
        for (id in event.players) {
            val player = player(id) ?: continue
            teleport(player, BaMap.ARENA_START)
            player.mes("The Penance are coming. Survive ${BaMatch.WAVES} waves.")
        }
    }

    private fun spawnWave(event: BaEvent.WaveStarted) {
        clearMonsters()
        notify(match.squad, "Wave ${event.wave}: ${event.monsters} Penance approach.")
        for (index in 0 until event.monsters) {
            val symbol = BaMonsters.symbol(event.wave, index)
            val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: continue
            val npc = Npc(type, BaMap.spawnTile(index))
            npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            monsters += npc
        }
    }

    private fun clearIfEmpty() {
        if (match.squad.isEmpty()) clearMonsters()
    }

    private fun clearMonsters() {
        for (npc in monsters) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        monsters.clear()
    }

    private fun finish(result: BaResult) {
        clearMonsters()
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        for ((id, ticks) in result.participation) {
            val player = player(id) ?: continue
            player.clearSoftTimer(TIMER)
            val reward =
                BaRewards.reward(
                    participationTicks = ticks,
                    wavesCleared = result.wavesCleared,
                    kills = result.kills[id] ?: 0,
                    cleared = result.cleared,
                    botShare = BOT_SHARE,
                    firstWinToday = result.cleared && MinigameProfile.claimFirstWin(player, day),
                )
            if (BaMap.inArena(player.coords)) teleport(player, BaMap.OUTPOST)
            val outcome = if (result.cleared) "You held the line!" else "The Penance overran you."
            player.mes("$outcome Waves cleared: ${result.wavesCleared}.")
            if (reward != BaReward.NONE) {
                addHonour(player, reward.honour)
                MinigameProfile.addTokens(player, reward.tokens)
                player.mes("You receive ${reward.honour} Honour Points and ${reward.tokens} Minigame Tokens.")
            }
        }
    }

    private fun addHonour(player: Player, amount: Int) {
        val total = (player.vars[HONOUR].toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, HONOUR, total)
    }

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) player(id)?.mes(message)
    }

    private fun player(id: Int): Player? = players[id]

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.barbassault"
        const val HONOUR = "varp.ba_honour"
        const val BOT_SHARE = 0.0
        private const val CHASE_INTERVAL = 4
    }
}
