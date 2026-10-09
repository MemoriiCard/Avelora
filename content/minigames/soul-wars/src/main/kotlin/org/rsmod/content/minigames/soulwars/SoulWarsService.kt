package org.rsmod.content.minigames.soulwars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.minigames.framework.MinigameProfile
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class SoulWarsService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val objs: ObjRepository,
    private val npcs: NpcRepository,
) {
    val match = SoulWarsMatch()
    private val avatars = HashMap<SwTeam, Npc>()

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun teamOf(player: Player): SwTeam? = match.teamOf(id(player))

    fun teamOfAvatar(npc: Npc): SwTeam? = avatars.entries.firstOrNull { it.value === npc }?.key

    fun toggleQueue(player: Player) {
        val id = id(player)
        if (match.isPlaying(id)) {
            player.mes("You're already in a game.")
            return
        }
        if (match.waiting.containsKey(id)) {
            apply(match.leave(id, clock.cycle))
            player.clearSoftTimer(TIMER)
            player.mes("You leave the Soul Wars queue.")
            return
        }
        val team = match.join(id, clock.cycle) ?: return
        player.softTimer(TIMER, 1)
        player.mes(
            "You join the ${team.name} team's queue. The game starts in " +
                "${match.ticksLeft(clock.cycle) * 6 / 10} seconds.",
        )
    }

    fun leave(player: Player, toHub: Boolean = true) {
        val events = match.leave(id(player), clock.cycle)
        strip(player)
        player.clearSoftTimer(TIMER)
        MiscOutput.clearPlayerOp(player, 1, "Attack")
        if (toHub && SoulWarsMap.inArena(player.coords)) teleport(player, SoulWarsMap.HUB)
        apply(events)
    }

    fun tick() {
        apply(match.update(clock.cycle, avatarHealth()))
    }

    fun sacrifice(player: Player) {
        val held = player.inv.count(SoulWarsItems.FRAGMENT)
        if (!isPlaying(player) || held == 0) {
            player.mes("You have no soul fragments to sacrifice.")
            return
        }
        player.invDel(player.inv, SoulWarsItems.FRAGMENT, held)
        match.sacrifice(id(player), held)?.let { apply(listOf(it)) }
    }

    fun onPlayerKill(killer: Player, victim: Player) {
        val kid = id(killer)
        val vid = id(victim)
        val kt = match.playing[kid] ?: return
        val vt = match.playing[vid] ?: return
        if (kt == vt) return
        killer.invAddOrDrop(objs, SoulWarsItems.FRAGMENT, 1)
        killer.mes("You gather a soul fragment from ${victim.displayName}.")
    }

    fun onAvatarKilled(team: SwTeam) {
        apply(match.avatarKilled(team, clock.cycle))
    }

    private fun apply(events: List<SwEvent>) {
        for (event in events) {
            when (event) {
                is SwEvent.WaitExtended -> notify(match.waiting.keys, event.reason)
                is SwEvent.GameStarted -> start(event)
                is SwEvent.Sacrificed -> {
                    val name = player(event.id)?.displayName ?: "Someone"
                    notify(
                        match.playing.keys,
                        "$name sacrifices ${event.fragments} soul fragments for the ${event.team.name} team.",
                    )
                }
                is SwEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(event: SwEvent.GameStarted) {
        for (team in SwTeam.entries) spawnAvatar(team, event.players)
        for ((id, team) in event.players) {
            val player = player(id) ?: continue
            if (player.worn[Wearpos.Back.slot] == null) {
                player.worn[Wearpos.Back.slot] = InvObj(SoulWarsItems.cape(team), 1)
            }
            teleport(player, SoulWarsMap.GRAVEYARD.getValue(team))
            MiscOutput.setPlayerOp(player, 1, "Attack", priority = true)
            player.mes("The battle begins! Destroy the ${team.other.name} avatar.")
        }
    }

    private fun spawnAvatar(team: SwTeam, roster: Map<Int, SwTeam>) {
        val type = ServerCacheManager.getNpc(SoulWarsItems.avatar(team).asRSCM(RSCMType.NPC)) ?: return
        val levels = roster.filterValues { it == team }.keys.mapNotNull { player(it)?.appearance?.combatLevel }
        val average = if (levels.isEmpty()) MIN_COMBAT else levels.average().toInt()
        val npc = Npc(type, SoulWarsMap.AVATAR.getValue(team))
        val hitpoints = (average * HP_PER_COMBAT).coerceIn(MIN_HP, MAX_HP)
        npc.baseHitpointsLvl = hitpoints
        npc.hitpoints = hitpoints
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        avatars[team] = npc
    }

    private fun avatarHealth(): Map<SwTeam, Double> =
        avatars.mapValues { (_, npc) -> npc.hitpoints.toDouble() / npc.baseHitpointsLvl.coerceAtLeast(1) }

    private fun clearAvatars() {
        for (npc in avatars.values) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        avatars.clear()
    }

    private fun finish(result: SwResult) {
        clearAvatars()
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val summary = describe(result)
        for ((id, team) in result.players) {
            val player = player(id) ?: continue
            val won = result.winner == team
            val reward =
                SoulWarsRewards.reward(
                    participationTicks = result.participation[id] ?: 0,
                    fragmentsSacrificed = result.sacrificed[id] ?: 0,
                    won = won,
                    botShare = BOT_SHARE,
                    firstWinToday = won && MinigameProfile.claimFirstWin(player, day),
                )
            strip(player)
            player.clearSoftTimer(TIMER)
            MiscOutput.clearPlayerOp(player, 1, "Attack")
            teleport(player, SoulWarsMap.HUB)
            player.mes(summary)
            if (reward != SwReward.NONE) {
                addZeal(player, reward.zeal)
                MinigameProfile.addTokens(player, reward.tokens)
                player.mes("You receive ${reward.zeal} Zeal and ${reward.tokens} Minigame Tokens.")
            } else {
                player.mes("You weren't in the game long enough to earn a reward.")
            }
        }
    }

    private fun describe(result: SwResult): String {
        val how =
            when (result.ending) {
                SwEnding.AvatarKilled -> "an avatar fell"
                SwEnding.Forfeit -> "the other team left"
                SwEnding.Timeout -> "time ran out"
            }
        val outcome = result.winner?.let { "${it.name} wins" } ?: "It's a draw"
        return "$outcome ($how)."
    }

    private fun addZeal(player: Player, amount: Int) {
        val total = (player.vars[ZEAL].toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, ZEAL, total)
    }

    private fun strip(player: Player) {
        val back = player.worn[Wearpos.Back.slot]
        if (back != null && SwTeam.entries.any { back.isType(SoulWarsItems.cape(it)) }) {
            player.worn[Wearpos.Back.slot] = null
        }
    }

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) player(id)?.mes(message)
    }

    private fun player(id: Int): Player? = players[id]

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.soul_wars"
        const val ZEAL = "varp.soul_wars_zeal"
        const val BOT_SHARE = 0.0
        private const val MIN_COMBAT = 3
        private const val HP_PER_COMBAT = 5
        private const val MIN_HP = 150
        private const val MAX_HP = 600
    }
}
