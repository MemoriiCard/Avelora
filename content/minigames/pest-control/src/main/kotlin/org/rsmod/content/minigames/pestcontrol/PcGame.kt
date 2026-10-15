package org.rsmod.content.minigames.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.content.minigames.framework.MinigameProfile
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

private class PcSpawn(val kind: PcPest, val portal: Int)

internal class PcGame(val lander: PcLander, private val ctx: PcContext) {
    val match = PcMatch()

    private var region: Region? = null
    private val portals = arrayOfNulls<Npc>(PcMatch.PORTALS)
    private val pests = LinkedHashMap<Npc, PcSpawn>()
    private var knight: Npc? = null
    private var squire: Npc? = null
    private var pestTier = 1
    private val fallen = HashSet<Int>()

    val isActive: Boolean
        get() = match.phase != PcPhase.Idle

    fun id(player: Player): Int = player.slotId

    fun isPlaying(player: Player): Boolean = match.isPlaying(id(player))

    fun isQueued(player: Player): Boolean = match.isQueued(id(player))

    fun portalIndex(npc: Npc): Int = portals.indexOfFirst { it === npc }

    fun isPest(npc: Npc): Boolean = npc in pests

    fun isKnight(npc: Npc): Boolean = npc === knight

    fun owns(npc: Npc): Boolean = portalIndex(npc) >= 0 || isPest(npc) || isKnight(npc)

    fun isShielded(npc: Npc): Boolean = portalIndex(npc).let { it >= 0 && match.isShielded(it) }

    fun contains(coords: CoordGrid): Boolean =
        region?.let { PcMap.inInstance(it.southWest, coords) } ?: false

    fun join(player: Player): Boolean = match.join(id(player), ctx.clock.cycle)

    fun leaveQueue(player: Player) {
        apply(match.leave(id(player), ctx.clock.cycle))
    }

    fun forfeit(player: Player, toOutpost: Boolean) {
        val events = match.leave(id(player), ctx.clock.cycle, forfeit = true)
        if (toOutpost && contains(player.coords)) teleport(player, PcMap.OUTPOST)
        apply(events)
    }

    fun onPlayerDeath(player: Player) {
        fallen += id(player)
        apply(match.leave(id(player), ctx.clock.cycle))
    }

    fun respawnCoords(player: Player): CoordGrid? =
        if (fallen.remove(id(player))) PcMap.OUTPOST else null

    fun engage(player: Player, amount: Int) {
        match.engage(id(player), amount)
    }

    fun onNpcKilled(npc: Npc, hero: Player) {
        val index = portalIndex(npc)
        when {
            index >= 0 -> {
                portals[index] = null
                apply(match.portalKilled(index, id(hero), ctx.clock.cycle))
            }
            pests.remove(npc) != null -> match.pestKilled(id(hero))
        }
    }

    fun sealBlast(player: Player): Int {
        val targets =
            pests.keys.filter {
                it.isSlotAssigned && it.coords.chebyshevDistance(player.coords) <= SEAL_RADIUS
            }
        for (npc in targets) {
            val damage = ctx.random.of(SEAL_MIN_DAMAGE, SEAL_MAX_DAMAGE)
            npc.queueNpcHit(player, 1, HitType.Typeless, damage, ctx.npcHits)
        }
        return targets.size
    }

    fun tick(cycle: Int) {
        apply(match.update(cycle, knightPressure()))
        if (match.phase != PcPhase.Playing) return
        if (cycle % SPAWN_INTERVAL == 0 && pests.size < MAX_PESTS) spawnPests()
        if (cycle % ACT_INTERVAL == 0) act()
    }

    private fun knightPressure(): Int {
        val target = knight ?: return 0
        var total = 0
        for ((npc, spawn) in pests) {
            val kind = spawn.kind
            if (!npc.isSlotAssigned || kind.knightDamage == 0) continue
            if (npc.coords.chebyshevDistance(target.coords) <= kind.knightReach) {
                total += kind.knightDamage
            }
        }
        return total
    }

    private fun spawnPests() {
        val base = region?.southWest ?: return
        for (index in 0 until PcMatch.PORTALS) {
            if (!match.isPortalAlive(index)) continue
            val kind = PcPest.roll(ctx.random.of(PcPest.TOTAL_WEIGHT))
            val symbol = variant(kind)
            val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: continue
            val source = if (kind == PcPest.Shifter) knightSide() else PcMap.PEST_SPAWNS[index]
            val npc = Npc(type, PcMap.toInstance(base, source))
            ctx.npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            pests[npc] = PcSpawn(kind, index)
        }
    }

    private fun variant(kind: PcPest): String {
        val symbol = kind.symbol(pestTier)
        return if (kind.variants && ctx.random.of(2) == 1) symbol.dropLast(1) + "b" else symbol
    }

    private fun knightSide(): CoordGrid =
        PcMap.KNIGHT.translate(ctx.random.of(-2, 2), ctx.random.of(-2, 2))

    private fun act() {
        val squad = match.squad.mapNotNull { ctx.players[it] }
        val anchor = knight?.coords
        for ((npc, spawn) in pests.entries.toList()) {
            if (!npc.isSlotAssigned) continue
            when (spawn.kind) {
                PcPest.Spinner -> tendPortal(npc, spawn)
                PcPest.Shifter -> anchor?.let { approach(npc, it, 1) }
                PcPest.Splatter -> rush(npc, squad)
                PcPest.Brawler -> nearest(npc, squad, Int.MAX_VALUE)?.let { chase(npc, it) }
                PcPest.Ravager,
                PcPest.Torcher,
                PcPest.Defiler -> {
                    val target = nearest(npc, squad, AGGRO_RANGE)
                    when {
                        target != null -> chase(npc, target)
                        anchor != null -> approach(npc, anchor, spawn.kind.knightReach - 1)
                    }
                }
            }
        }
    }

    private fun nearest(npc: Npc, squad: List<Player>, range: Int): Player? =
        squad
            .filter { it.coords.chebyshevDistance(npc.coords) <= range }
            .minByOrNull { it.coords.chebyshevDistance(npc.coords) }

    private fun chase(npc: Npc, target: Player) {
        npc.opPlayer2(target, ctx.interactions)
    }

    private fun approach(npc: Npc, dest: CoordGrid, within: Int) {
        if (npc.coords.chebyshevDistance(dest) > within) npc.walk(dest)
    }

    private fun rush(npc: Npc, squad: List<Player>) {
        val target = nearest(npc, squad, Int.MAX_VALUE) ?: return
        if (target.coords.chebyshevDistance(npc.coords) <= 1) {
            detonate(npc, SPLATTER_MIN, SPLATTER_MAX, 1)
            return
        }
        chase(npc, target)
    }

    private fun tendPortal(npc: Npc, spawn: PcSpawn) {
        val portal = portals[spawn.portal]
        if (portal == null || !portal.isSlotAssigned) {
            detonate(npc, SPINNER_BLAST_MIN, SPINNER_BLAST_MAX, SPINNER_BLAST_RADIUS)
            return
        }
        if (npc.coords.chebyshevDistance(portal.coords) > SPINNER_REACH) {
            npc.walk(portal.coords)
            return
        }
        val ceiling = portal.baseHitpointsLvl
        portal.hitpoints = (portal.hitpoints + SPINNER_HEAL).coerceAtMost(ceiling)
    }

    private fun detonate(npc: Npc, min: Int, max: Int, radius: Int) {
        for (id in match.squad) {
            val player = ctx.players[id] ?: continue
            if (player.coords.chebyshevDistance(npc.coords) > radius) continue
            player.queueHit(1, HitType.Typeless, ctx.random.of(min, max), ctx.playerHits)
        }
        pests.remove(npc)
        if (npc.isSlotAssigned) ctx.npcs.del(npc, Int.MAX_VALUE)
    }

    private fun apply(events: List<PcEvent>) {
        for (event in events) {
            when (event) {
                is PcEvent.WaitExtended -> notify(match.waiting, event.reason)
                is PcEvent.GameStarted -> start(event)
                is PcEvent.ShieldDropped ->
                    notify(match.squad, "The ${PcNpcs.PORTAL_NAMES[event.index]} portal shield has dropped!")
                is PcEvent.PortalDown ->
                    notify(match.squad, "A portal has been destroyed. ${match.portalsAlive} remain.")
                is PcEvent.KnightHurt ->
                    if (event.hitpoints <= KNIGHT_WARNING) {
                        notify(match.squad, "The Void Knight is badly hurt! (${event.hitpoints} left)")
                    }
                is PcEvent.ActivityLow ->
                    notify(listOf(event.player), "You are going idle. Attack pests or portals to stay in the fight.")
                is PcEvent.ActivityDepleted ->
                    notify(listOf(event.player), "You have been idle too long and will earn no reward this game.")
                is PcEvent.GameEnded -> finish(event.result)
            }
        }
    }

    private fun start(event: PcEvent.GameStarted) {
        clearNpcs()
        val roster = event.players.mapNotNull { ctx.players[it] }
        val allocated = ctx.regions.add(PcMap.template())
        if (allocated == null) {
            notify(event.players, "The lander can't depart right now. Try again shortly.")
            apply(match.abort(ctx.clock.cycle))
            return
        }
        ctx.regions.protect(allocated)
        region = allocated
        val average = if (roster.isEmpty()) 3 else roster.map { it.combatLevel }.average().toInt()
        pestTier = PcNpcs.tier(average)
        spawnStatics(allocated.southWest)
        roster.forEachIndexed { index, player ->
            teleport(player, PcMap.toInstance(allocated.southWest, PcMap.START[index % PcMap.START.size]))
            player.mes("Defend the Void Knight! Destroy all four portals.")
        }
    }

    private fun spawnStatics(base: CoordGrid) {
        for (index in 0 until PcMatch.PORTALS) {
            val type = ServerCacheManager.getNpc(PcNpcs.portal(index).asRSCM(RSCMType.NPC)) ?: continue
            val npc = Npc(type, PcMap.toInstance(base, PcMap.PORTALS[index]))
            npc.baseHitpointsLvl = lander.portalHitpoints
            npc.hitpoints = lander.portalHitpoints
            ctx.npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            portals[index] = npc
        }
        knight = spawn(PcNpcs.knight(), PcMap.toInstance(base, PcMap.KNIGHT))
        squire = spawn(lander.squire, PcMap.toInstance(base, PcMap.SQUIRE))
    }

    private fun spawn(symbol: String, at: CoordGrid): Npc? {
        val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: return null
        val npc = Npc(type, at)
        ctx.npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        return npc
    }

    private fun clearNpcs() {
        for (npc in pests.keys) if (npc.isSlotAssigned) ctx.npcs.del(npc, Int.MAX_VALUE)
        pests.clear()
        for (index in portals.indices) {
            portals[index]?.takeIf { it.isSlotAssigned }?.let { ctx.npcs.del(it, Int.MAX_VALUE) }
            portals[index] = null
        }
        knight?.takeIf { it.isSlotAssigned }?.let { ctx.npcs.del(it, Int.MAX_VALUE) }
        knight = null
        squire?.takeIf { it.isSlotAssigned }?.let { ctx.npcs.del(it, Int.MAX_VALUE) }
        squire = null
    }

    private fun finish(result: PcResult) {
        clearNpcs()
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val summary = describe(result)
        for (id in result.participation.keys) {
            val player = ctx.players[id] ?: continue
            if (contains(player.coords)) teleport(player, PcMap.OUTPOST)
            if (id in result.forfeited || result.ending == PcEnding.Abandoned) continue
            player.mes(summary)
            if (id in result.idled) {
                player.mes("You were idle for too long, so you earn no reward.")
                continue
            }
            val reward =
                PcRewards.reward(
                    lander = lander,
                    combatLevel = player.combatLevel,
                    portalKills = result.portalKills[id] ?: 0,
                    won = result.won,
                    botShare = BOT_SHARE,
                    firstWinToday = result.won && MinigameProfile.claimFirstWin(player, day),
                )
            PcPoints.add(player, reward.points)
            MinigameProfile.addTokens(player, reward.tokens)
            if (reward.coins > 0) player.invAddOrDrop(ctx.objs, "obj.coins", reward.coins)
            player.mes("You receive ${reward.points} Pest Control points and ${reward.tokens} Minigame Tokens.")
        }
        release()
    }

    private fun release() {
        val allocated = region ?: return
        region = null
        ctx.regions.unprotect(allocated)
        if (ctx.regions.isValid(allocated)) ctx.registry.unregister(allocated)
    }

    private fun describe(result: PcResult): String =
        when (result.ending) {
            PcEnding.PortalsDestroyed -> "All portals destroyed. The Void Knights are saved!"
            PcEnding.KnightFell -> "The Void Knight has fallen."
            PcEnding.Timeout -> "You held the line until the lander returned. The Void Knights are saved!"
            PcEnding.Abandoned -> "Everyone left the battle."
        }

    private fun notify(ids: Collection<Int>, message: String) {
        for (id in ids) ctx.players[id]?.mes(message)
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, ctx.collision, dest)
    }

    companion object {
        const val BOT_SHARE = 0.0
        private const val SPAWN_INTERVAL = 30
        private const val ACT_INTERVAL = 4
        private const val MAX_PESTS = 32
        private const val KNIGHT_WARNING = 100
        private const val AGGRO_RANGE = 6
        private const val SPINNER_REACH = 3
        private const val SPINNER_HEAL = 6
        private const val SPINNER_BLAST_MIN = 4
        private const val SPINNER_BLAST_MAX = 8
        private const val SPINNER_BLAST_RADIUS = 2
        private const val SPLATTER_MIN = 5
        private const val SPLATTER_MAX = 24
        const val SEAL_RADIUS = 3
        const val SEAL_MIN_DAMAGE = 8
        const val SEAL_MAX_DAMAGE = 12
    }
}
