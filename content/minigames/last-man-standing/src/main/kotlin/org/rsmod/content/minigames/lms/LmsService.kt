package org.rsmod.content.minigames.lms

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.content.minigames.framework.BotPolicy
import org.rsmod.content.minigames.framework.Minigame
import org.rsmod.content.minigames.framework.MinigameConfig
import org.rsmod.content.minigames.framework.MinigameProfile
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class LmsService
@Inject
constructor(
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val bots: LmsBots,
) {
    private val botPolicy = BotPolicy(MinigameConfig.load())
    val match = LmsMatch(botPlan = { botPolicy.botsNeeded(Minigame.LastManStanding, it) })
    private val looted = HashSet<Int>()
    private var lastTick = -1

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun toggleQueue(player: Player, mode: LmsMode = LmsMode.Casual) {
        val id = id(player)
        if (match.isPlaying(id)) {
            player.mes("You're already in a game.")
            return
        }
        if (match.isQueued(id)) {
            apply(match.leave(id, clock.cycle))
            player.clearSoftTimer(TIMER)
            player.mes("You leave the Last Man Standing queue.")
            return
        }
        if (!isEmptyHanded(player)) {
            player.mes("You must bank everything you're carrying and wearing before you can enter.")
            return
        }
        if (!match.join(id, clock.cycle, mode)) {
            player.mes("The queue is full or a ${match.lobbyMode.name.lowercase()} game is forming.")
            return
        }
        player.softTimer(TIMER, 1)
        player.mes(
            "You join the Last Man Standing queue. The game starts in " +
                "${match.ticksLeft(clock.cycle) * 6 / 10} seconds if enough players are waiting.",
        )
    }

    fun leave(player: Player, toLobby: Boolean = true) {
        val events = match.leave(id(player), clock.cycle)
        player.clearSoftTimer(TIMER)
        wipe(player)
        if (toLobby && LmsMap.inArena(player.coords)) teleport(player, LmsMap.LOBBY)
        apply(events)
    }

    fun tick() {
        val cycle = clock.cycle
        if (cycle == lastTick) return
        lastTick = cycle
        apply(match.update(cycle))
        if (match.phase == LmsPhase.Playing) {
            applyZone(cycle)
            bots.update(cycle, match.survivors.mapNotNull { player(it) }, match.elapsed(cycle))
        }
    }

    fun openChest(player: Player, coords: CoordGrid) {
        if (!isPlaying(player)) {
            player.mes("Nothing happens.")
            return
        }
        if (!looted.add(coords.packed)) {
            player.mes("This has already been looted.")
            return
        }
        val entry = LmsLoot.roll { random.of(it) }
        player.invAdd(player.inv, entry.obj, entry.count)
        player.mes("You find something useful.")
    }

    fun onDeath(victim: Player, killer: Player?) {
        wipe(victim)
        apply(match.eliminate(id(victim), killer?.let { id(it) }, clock.cycle))
    }

    fun isBot(npc: Npc): Boolean = bots.isBot(npc)

    fun onBotKilled(npc: Npc, hero: Player) {
        val botId = bots.remove(npc) ?: return
        apply(match.eliminate(botId, id(hero), clock.cycle))
    }

    private fun applyZone(cycle: Int) {
        val elapsed = match.elapsed(cycle)
        if (elapsed % LmsZone.DAMAGE_INTERVAL != 0) return
        val damage = LmsZone.damage(elapsed)
        for (id in match.survivors.toList()) {
            val player = player(id) ?: continue
            if (LmsZone.isOutside(player.coords.x, player.coords.z, elapsed)) {
                player.mes("The fog is hurting you. Move towards the centre of the island!")
                player.queueHit(
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = NoopPlayerHitModifier,
                )
            }
        }
    }

    private fun apply(events: List<LmsEvent>) {
        for (event in events) {
            when (event) {
                is LmsEvent.WaitExtended -> notify(match.waiting, event.reason)
                is LmsEvent.GameStarted -> start(event.players, event.bots)
                is LmsEvent.Eliminated -> eliminated(event)
                is LmsEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(ids: Set<Int>, botIds: Set<Int>) {
        looted.clear()
        val starts = LmsMap.STARTS.shuffled(java.util.Random(clock.cycle.toLong()))
        bots.spawn(botIds, starts.reversed())
        for ((index, id) in ids.withIndex()) {
            val player = player(id) ?: continue
            if (!isEmptyHanded(player)) wipe(player)
            player.invAdd(player.inv, "obj.br_rune_scimitar", 1)
            player.invAdd(player.inv, "obj.br_shark", 4)
            teleport(player, starts[index % starts.size])
            player.mes("The game has begun! Search chests for supplies and stay inside the fog line.")
        }
    }

    private fun eliminated(event: LmsEvent.Eliminated) {
        val player = player(event.id) ?: return
        player.clearSoftTimer(TIMER)
        val left = match.survivors.size
        player.mes("You came ${ordinal(event.placement)}. $left player${if (left == 1) "" else "s"} remain.")
        event.killer?.let { player(it)?.mes("You eliminated ${player.displayName}.") }
    }

    private fun finish(result: LmsResult) {
        bots.clear()
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        for ((id, placement) in result.placements) {
            val player = player(id) ?: continue
            val won = id == result.winner
            val kills = result.kills[id] ?: 0
            val reward =
                LmsRewards.reward(
                    participationTicks = result.participation[id] ?: 0,
                    kills = kills,
                    won = won,
                    botShare = result.botShare,
                    firstWinToday = won && MinigameProfile.claimFirstWin(player, day),
                )
            val competitive = result.mode == LmsMode.Competitive
            val points = if (competitive) LmsCompetitive.points(placement, kills) else 0
            player.clearSoftTimer(TIMER)
            wipe(player)
            teleport(player, LmsMap.LOBBY)
            player.mes(if (won) "You are the last one standing!" else "The game is over. You placed ${ordinal(placement)}.")
            if (won) addVar(player, WINS, 1)
            if (competitive) {
                val change = LmsCompetitive.ratingChange(placement)
                addVar(player, RATING_OFFSET, change, floor = Int.MIN_VALUE)
                player.mes("Rating ${if (change >= 0) "+" else ""}$change: ${rating(player)} (${LmsCompetitive.title(rating(player))}).")
                if (points > 0) addPoints(player, points)
            }
            if (reward != LmsReward.NONE) {
                MinigameProfile.addTokens(player, reward.tokens)
                player.mes(
                    if (points > 0) "You receive $points LMS points and ${reward.tokens} Minigame Tokens."
                    else "You receive ${reward.tokens} Minigame Tokens.",
                )
            }
        }
    }

    fun points(player: Player): Int = player.vars[POINTS]

    fun wins(player: Player): Int = player.vars[WINS]

    fun rating(player: Player): Int = LmsCompetitive.START_RATING + player.vars[RATING_OFFSET]

    fun spendPoints(player: Player, amount: Int): Boolean {
        if (amount <= 0 || points(player) < amount) return false
        VarPlayerIntMapSetter.set(player, POINTS, points(player) - amount)
        return true
    }

    private fun addVar(player: Player, varp: String, amount: Int, floor: Int = 0) {
        val total = (player.vars[varp].toLong() + amount).coerceIn(floor.toLong(), Int.MAX_VALUE.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, varp, total)
    }

    private fun addPoints(player: Player, amount: Int) = addVar(player, POINTS, amount)

    private fun isEmptyHanded(player: Player): Boolean = player.inv.isEmpty() && player.worn.isEmpty()

    private fun wipe(player: Player) {
        player.invClear(player.inv)
        player.invClear(player.worn)
    }

    private fun ordinal(n: Int): String =
        when {
            n % 100 in 11..13 -> "${n}th"
            n % 10 == 1 -> "${n}st"
            n % 10 == 2 -> "${n}nd"
            n % 10 == 3 -> "${n}rd"
            else -> "${n}th"
        }

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) player(id)?.mes(message)
    }

    private fun player(id: Int): Player? = players[id]

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, collision, dest)
    }

    companion object {
        const val TIMER = "timer.lms"
        const val POINTS = "varp.lms_points"
        const val WINS = "varp.lms_wins"
        const val RATING_OFFSET = "varp.lms_rating_offset"
    }
}
