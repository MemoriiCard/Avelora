package org.rsmod.content.minigames.castlewars

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.minigames.framework.MinigameProfile
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class CastleWarsService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val objs: ObjRepository,
) {
    val match = CastleWarsMatch()

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun teamOf(player: Player): CwTeam? = match.teamOf(id(player))

    fun enter(player: Player, preferred: CwTeam?) {
        if (match.teamOf(id(player)) != null) {
            player.mes("You are already in a Castle Wars game.")
            return
        }
        if (player.worn[Wearpos.Back.slot] != null || player.worn[Wearpos.Hat.slot] != null) {
            player.mes("You can't bring a cape or helmet into Castle Wars; the team colours are handed out inside.")
            return
        }
        val team = match.join(id(player), preferred, clock.cycle)
        if (team == null) {
            player.mes("That team is full. Try the other portal or the random one.")
            return
        }
        player.worn[Wearpos.Back.slot] = InvObj(CastleWarsItems.cloak(team), 1)
        player.softTimer(TIMER, 1)
        teleport(player, CastleWarsMap.WAITING_ROOM.getValue(team))
        val side = team.name
        player.mes("You join the $side team. The game starts in ${match.ticksLeft(clock.cycle) * 6 / 10} seconds.")
    }

    fun leave(player: Player, toLobby: Boolean = true) {
        val events = match.leave(id(player), clock.cycle)
        strip(player)
        player.clearSoftTimer(TIMER)
        if (toLobby && CastleWarsMap.inside(player.coords)) teleport(player, CastleWarsMap.LOBBY)
        apply(events)
    }

    fun tick() {
        apply(match.update(clock.cycle))
    }

    fun takeFlag(player: Player, standOf: CwTeam) {
        val team = match.teamOf(id(player)) ?: return
        if (!match.isPlaying(id(player))) return
        if (team == standOf) {
            capture(player)
            return
        }
        if (player.worn[Wearpos.RightHand.slot] != null) {
            player.mes("You must remove your weapon before you can carry the flag.")
            return
        }
        val event = match.takeFlag(id(player), standOf)
        if (event == null) {
            player.mes("The ${standOf.name} flag isn't there to take.")
            return
        }
        apply(listOf(event))
    }

    fun capture(player: Player) {
        val id = id(player)
        if (!match.isPlaying(id)) return
        if (match.carriedFlag(id) == null) {
            player.mes("You need to be carrying the enemy flag to score here.")
            return
        }
        val event = match.capture(id)
        if (event == null) {
            player.mes("Your own flag must be at your base before you can score.")
            return
        }
        apply(listOf(event))
    }

    fun onDeath(player: Player) {
        match.onDeath(id(player))?.let { apply(listOf(it)) }
    }

    private fun apply(events: List<CwEvent>) {
        for (event in events) {
            when (event) {
                is CwEvent.GameStarted -> start(event)
                is CwEvent.WaitExtended -> notify(match.waiting.keys, event.reason)
                is CwEvent.FlagTaken -> {
                    holdBanner(event.taker, event.flagOf)
                    val name = player(event.taker)?.displayName ?: "Someone"
                    notify(everyone(), "$name has taken the ${event.flagOf.name} flag!")
                }
                is CwEvent.FlagReturned -> {
                    dropBanners()
                    notify(everyone(), "The ${event.flagOf.name} flag has been returned to its base.")
                }
                is CwEvent.Scored -> {
                    dropBanners()
                    val name = player(event.scorer)?.displayName ?: "Someone"
                    val sara = match.score(CwTeam.Saradomin)
                    val zam = match.score(CwTeam.Zamorak)
                    notify(everyone(), "$name scores for ${event.team.name}! Saradomin $sara - $zam Zamorak.")
                }
                is CwEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(event: CwEvent.GameStarted) {
        for ((id, team) in event.players) {
            val player = player(id) ?: continue
            teleport(player, CastleWarsMap.SPAWN.getValue(team))
            MiscOutput.setPlayerOp(player, 1, "Attack", priority = true)
            player.mes("The battle begins! Capture the enemy flag and bring it to your own stand.")
        }
    }

    private fun finish(result: CwResult) {
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val summary = describe(result)
        for ((id, team) in result.players) {
            val player = player(id) ?: continue
            val won = result.winner == team
            val reward =
                CastleWarsRewards.reward(
                    participationTicks = result.participation[id] ?: 0,
                    won = won,
                    botShare = BOT_SHARE,
                    firstWinToday = won && MinigameProfile.claimFirstWin(player, day),
                )
            strip(player)
            player.clearSoftTimer(TIMER)
            MiscOutput.clearPlayerOp(player, 1, "Attack")
            teleport(player, CastleWarsMap.LOBBY)
            player.mes(summary)
            if (reward != CwReward.NONE) {
                player.invAddOrDrop(objs, CastleWarsItems.TICKET, reward.tickets)
                MinigameProfile.addTokens(player, reward.tokens)
                player.mes("You receive ${reward.tickets} Castle Wars tickets and ${reward.tokens} Minigame Tokens.")
            } else {
                player.mes("You weren't in the game long enough to earn a reward.")
            }
        }
    }

    private fun describe(result: CwResult): String {
        val sara = result.scores.getValue(CwTeam.Saradomin)
        val zam = result.scores.getValue(CwTeam.Zamorak)
        val outcome = result.winner?.let { "${it.name} wins" } ?: "It's a draw"
        return "$outcome! Final score: Saradomin $sara - $zam Zamorak."
    }

    private fun holdBanner(id: Int, flagOf: CwTeam) {
        player(id)?.let { it.worn[Wearpos.RightHand.slot] = InvObj(CastleWarsItems.banner(flagOf), 1) }
    }

    private fun dropBanners() {
        for (id in match.playing.keys) {
            val player = player(id) ?: continue
            if (match.carriedFlag(id) != null) continue
            val held = player.worn[Wearpos.RightHand.slot]
            if (held != null && CwTeam.entries.any { held.isType(CastleWarsItems.banner(it)) }) {
                player.worn[Wearpos.RightHand.slot] = null
            }
        }
    }

    private fun strip(player: Player) {
        val back = player.worn[Wearpos.Back.slot]
        if (back != null && CwTeam.entries.any { back.isType(CastleWarsItems.cloak(it)) }) {
            player.worn[Wearpos.Back.slot] = null
        }
        val held = player.worn[Wearpos.RightHand.slot]
        if (held != null && CwTeam.entries.any { held.isType(CastleWarsItems.banner(it)) }) {
            player.worn[Wearpos.RightHand.slot] = null
        }
    }

    private fun everyone(): Set<Int> = match.playing.keys + match.waiting.keys

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) player(id)?.mes(message)
    }

    private fun player(id: Int): Player? = players[id]

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.castle_wars"
        const val BOT_SHARE = 0.0
    }
}
