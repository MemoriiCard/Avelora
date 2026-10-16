package org.rsmod.content.minigames.barbassault

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.content.minigames.framework.HubCrates
import org.rsmod.content.minigames.framework.MinigameProfile
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

internal class BaGame(private val ctx: BaContext) {
    val match = BaMatch()

    private var region: Region? = null
    private val monsters = LinkedHashMap<Npc, BaKind>()
    private val fallen = HashSet<Int>()
    private var callIndex = 0

    val isActive: Boolean
        get() = match.phase != BaPhase.Idle

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun isQueued(player: Player): Boolean = match.isQueued(id(player))

    fun roleOf(player: Player): BaRole? = match.roleOf(id(player))

    fun isMonster(npc: Npc): Boolean = npc in monsters

    fun contains(coords: CoordGrid): Boolean =
        region?.let { BaMap.inInstance(it.southWest, coords) } ?: false

    fun join(player: Player, role: BaRole): Boolean = match.join(id(player), role, ctx.clock.cycle)

    fun leaveQueue(player: Player) {
        apply(match.leave(id(player), ctx.clock.cycle))
    }

    fun leave(player: Player, toOutpost: Boolean) {
        val events = match.leave(id(player), ctx.clock.cycle)
        if (toOutpost && contains(player.coords)) teleport(player, BaMap.OUTPOST)
        apply(events)
    }

    fun onPlayerDeath(player: Player) {
        fallen += id(player)
        apply(match.leave(id(player), ctx.clock.cycle))
    }

    fun respawnCoords(player: Player): CoordGrid? =
        if (fallen.remove(id(player))) BaMap.OUTPOST else null

    fun onMonsterKilled(npc: Npc, hero: Player) {
        val kind = monsters.remove(npc) ?: return
        apply(match.monsterKilled(kind, id(hero), ctx.clock.cycle))
    }

    fun tick(cycle: Int) {
        apply(match.update(cycle))
        if (match.phase != BaPhase.Wave) return
        if (cycle % ACT_INTERVAL == 0) act()
        if (cycle % LURE_INTERVAL == 0) lure()
        if (cycle % POISON_INTERVAL == 0) poison()
        if (cycle % HEAL_INTERVAL == 0) heal()
        if (cycle % EGG_INTERVAL == 0) deliverEggs()
        if (cycle % EGG_CHECK_INTERVAL == 0) collectEggs()
        if (cycle % CALL_INTERVAL == 0) changeCall()
    }

    private fun squad(): List<Player> = match.squad.mapNotNull { ctx.players[it] }

    private fun squad(role: BaRole): List<Player> = squad().filter { match.roleOf(id(it)) == role }

    private fun act() {
        val players = squad()
        if (players.isEmpty()) return
        val base = region?.southWest ?: return
        val escape = BaMap.toInstance(base, BaMap.ESCAPE)
        for ((npc, kind) in monsters.entries.toList()) {
            if (!npc.isSlotAssigned) continue
            when (kind) {
                BaKind.Runner -> run(npc, escape)
                BaKind.Healer -> approach(npc, players)
                else -> npc.opPlayer2(nearest(npc, players), ctx.interactions)
            }
        }
    }

    private fun nearest(npc: Npc, players: List<Player>): Player =
        players.minBy { it.coords.chebyshevDistance(npc.coords) }

    private fun approach(npc: Npc, players: List<Player>) {
        val target = nearest(npc, players)
        if (target.coords.chebyshevDistance(npc.coords) > 1) npc.walk(target.coords)
    }

    private fun run(npc: Npc, escape: CoordGrid) {
        if (npc.coords.chebyshevDistance(escape) <= 1) {
            monsters.remove(npc)
            ctx.npcs.del(npc, Int.MAX_VALUE)
            notify(match.squad, "A Penance Runner escaped!")
            apply(match.runnerEscaped(ctx.clock.cycle))
            return
        }
        npc.walk(escape)
    }

    private fun lure() {
        val defenders = squad(BaRole.Defender)
        if (defenders.isEmpty()) return
        for ((npc, kind) in monsters) {
            if (kind != BaKind.Runner || !npc.isSlotAssigned) continue
            val catcher =
                defenders.firstOrNull {
                    val range = BaLevels.lureRange(BaLevels.level(stored(it, BaRole.Defender)))
                    it.coords.chebyshevDistance(npc.coords) <= range
                } ?: continue
            npc.queueNpcHit(catcher, 1, HitType.Typeless, npc.hitpoints, ctx.npcHits)
        }
    }

    private fun poison() {
        val healers = squad(BaRole.Healer)
        if (healers.isEmpty()) return
        val targets = monsters.filter { it.value == BaKind.Healer && it.key.isSlotAssigned }.keys
        for (medic in healers) {
            val target =
                targets
                    .filter { it.coords.chebyshevDistance(medic.coords) <= POISON_RANGE }
                    .minByOrNull { it.coords.chebyshevDistance(medic.coords) } ?: continue
            target.queueNpcHit(medic, 1, HitType.Typeless, POISON_DAMAGE, ctx.npcHits)
        }
    }

    private fun heal() {
        for (medic in squad(BaRole.Healer)) {
            val level = BaLevels.level(stored(medic, BaRole.Healer))
            val target =
                squad()
                    .filter { it.coords.chebyshevDistance(medic.coords) <= HEAL_RANGE }
                    .filter { it.stat("stat.hitpoints") < it.statBase("stat.hitpoints") }
                    .minByOrNull { it.stat("stat.hitpoints") } ?: continue
            val before = target.stat("stat.hitpoints")
            target.statHeal("stat.hitpoints", BaLevels.healAmount(level), 0)
            val gained = target.stat("stat.hitpoints") - before
            if (gained > 0) match.healed(id(medic), gained)
        }
    }

    private fun stored(player: Player, role: BaRole): Int = BaPoints.get(player, role)

    private fun deliverEggs() {
        for (collector in squad(BaRole.Collector)) {
            val colour = BaEggs.COLOURS[ctx.random.of(BaEggs.COLOURS.size)]
            collector.invAddOrDrop(ctx.objs, colour.obj, 1)
        }
    }

    private fun collectEggs() {
        val call = BaEggs.COLOURS[callIndex]
        for (collector in squad(BaRole.Collector)) {
            val held = collector.inv.count(call.obj)
            if (held <= 0) continue
            collector.invDel(collector.inv, call.obj, held)
            repeat(held) { match.eggCollected(id(collector)) }
        }
    }

    private fun changeCall() {
        for (collector in squad(BaRole.Collector)) {
            for (colour in BaEggs.COLOURS) {
                val held = collector.inv.count(colour.obj)
                if (held <= 0) continue
                collector.invDel(collector.inv, colour.obj, held)
                collector.queueHit(1, HitType.Typeless, held * EGG_BLAST, ctx.playerHits)
                collector.mes("Your ${colour.label} eggs exploded!")
            }
        }
        callIndex = (callIndex + 1) % BaEggs.COLOURS.size
        val call = BaEggs.COLOURS[callIndex]
        for (collector in squad(BaRole.Collector)) collector.mes("The Collector call is now ${call.label}.")
    }

    private fun apply(events: List<BaEvent>) {
        for (event in events) {
            when (event) {
                is BaEvent.WaitExtended -> notify(match.waiting, event.reason)
                is BaEvent.GameStarted -> start(event)
                is BaEvent.WaveStarted -> spawnWave(event)
                is BaEvent.WaveCleared -> notify(match.squad, "Wave ${event.wave} cleared.")
                is BaEvent.PlayerDown -> if (match.squad.isEmpty()) clearMonsters()
                is BaEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(event: BaEvent.GameStarted) {
        clearMonsters()
        val roster = event.players.mapNotNull { ctx.players[it] }
        val allocated = ctx.regions.add(BaMap.template())
        if (allocated == null) {
            notify(event.players, "The cavern can't be prepared right now. Try again shortly.")
            apply(match.abort(ctx.clock.cycle))
            return
        }
        ctx.regions.protect(allocated)
        region = allocated
        callIndex = 0
        roster.forEachIndexed { index, player ->
            val tile = BaMap.ARENA_START.translate(index - roster.size / 2, 0)
            teleport(player, BaMap.toInstance(allocated.southWest, tile))
            player.mes("The Penance are coming. You are the ${match.roleOf(id(player))?.label}.")
        }
    }

    private fun spawnWave(event: BaEvent.WaveStarted) {
        clearMonsters()
        val base = region?.southWest ?: return
        notify(match.squad, "Wave ${event.wave}: ${event.kinds.size} Penance approach.")
        event.kinds.forEachIndexed { index, kind ->
            val symbol = BaMonsters.symbol(kind, event.wave)
            val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: return@forEachIndexed
            val source = if (kind == BaKind.Queen) BaMap.QUEEN else BaMap.spawnTile(index)
            val npc = Npc(type, BaMap.toInstance(base, source))
            ctx.npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            monsters[npc] = kind
        }
    }

    private fun clearMonsters() {
        for (npc in monsters.keys) if (npc.isSlotAssigned) ctx.npcs.del(npc, Int.MAX_VALUE)
        monsters.clear()
    }

    private fun finish(result: BaResult) {
        clearMonsters()
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        for ((id, ticks) in result.participation) {
            val player = ctx.players[id] ?: continue
            for (colour in BaEggs.COLOURS) {
                val held = player.inv.count(colour.obj)
                if (held > 0) player.invDel(player.inv, colour.obj, held)
            }
            if (contains(player.coords)) teleport(player, BaMap.OUTPOST)
            val outcome = if (result.cleared) "You held the line!" else "The Penance overran you."
            player.mes("$outcome Waves cleared: ${result.wavesCleared}.")
            if (!BaRewards.eligible(ticks, result.wavesCleared)) continue
            val tokens =
                BaRewards.tokens(
                    participationTicks = ticks,
                    wavesCleared = result.wavesCleared,
                    cleared = result.cleared,
                    botShare = BOT_SHARE,
                    firstWinToday = result.cleared && MinigameProfile.claimFirstWin(player, day),
                )
            for ((role, amount) in result.points[id].orEmpty()) {
                if (amount <= 0) continue
                BaPoints.add(player, role, amount)
                player.mes("You earn $amount ${role.label} points.")
            }
            if (result.queenKilled) BaPoints.addQueenKill(player)
            BaPoints.recordProgress(player, result.wavesCleared)
            MinigameProfile.addTokens(player, tokens)
            HubCrates.award(player, ctx.objs, ctx.random, BOT_SHARE, result.cleared)
            player.mes("You receive $tokens Minigame Tokens.")
        }
        release()
    }

    private fun release() {
        val allocated = region ?: return
        region = null
        ctx.regions.unprotect(allocated)
        if (ctx.regions.isValid(allocated)) ctx.registry.unregister(allocated)
    }

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) ctx.players[id]?.mes(message)
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, ctx.collision, dest)
    }

    companion object {
        const val BOT_SHARE = 0.0
        private const val ACT_INTERVAL = 4
        private const val LURE_INTERVAL = 2
        private const val POISON_INTERVAL = 3
        private const val HEAL_INTERVAL = 4
        private const val EGG_INTERVAL = 6
        private const val EGG_CHECK_INTERVAL = 2
        private const val CALL_INTERVAL = 50
        private const val POISON_RANGE = 6
        private const val POISON_DAMAGE = 3
        private const val HEAL_RANGE = 8
        private const val EGG_BLAST = 5
    }
}
