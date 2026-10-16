package org.rsmod.content.minigames.pestcontrol

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid

@Singleton
class PcService @Inject internal constructor(private val ctx: PcContext) {
    private val games = PcLander.entries.associateWith { PcGame(it, ctx) }
    private var ticking = false

    internal fun gameOf(player: Player): PcGame? = games.values.firstOrNull { it.isPlaying(player) }

    internal fun gameOf(npc: Npc): PcGame? = games.values.firstOrNull { it.owns(npc) }

    private fun queuedIn(player: Player): PcGame? = games.values.firstOrNull { it.isQueued(player) }

    fun isPlaying(player: Player): Boolean = gameOf(player) != null

    fun isKnight(npc: Npc): Boolean = gameOf(npc)?.isKnight(npc) == true

    fun isPest(npc: Npc): Boolean = gameOf(npc)?.isPest(npc) == true

    fun isShielded(npc: Npc): Boolean = gameOf(npc)?.isShielded(npc) == true

    fun portalIndex(npc: Npc): Int = gameOf(npc)?.portalIndex(npc) ?: -1

    fun toggleQueue(player: Player, lander: PcLander) {
        if (isPlaying(player)) {
            player.mes("You're already in a game.")
            return
        }
        val queued = queuedIn(player)
        if (queued != null) {
            queued.leaveQueue(player)
            player.mes("You leave the ${queued.lander.label} lander.")
            return
        }
        if (player.combatLevel < lander.minCombat) {
            player.mes("You need combat level ${lander.minCombat} to board the ${lander.label} lander.")
            return
        }
        val game = games.getValue(lander)
        if (!game.join(player)) {
            player.mes("A game is under way. The next lander leaves when it ends.")
            return
        }
        startTicking()
        player.mes(
            "You board the ${lander.label} lander. It departs in " +
                "${game.match.ticksLeft(ctx.clock.cycle) * 6 / 10} seconds, or sooner when it is full.",
        )
    }

    fun leave(player: Player, toOutpost: Boolean = true) {
        queuedIn(player)?.leaveQueue(player)
        val game = gameOf(player)
        if (game != null) {
            game.forfeit(player, toOutpost)
            return
        }
        if (toOutpost && inArena(player.coords)) teleport(player, PcMap.OUTPOST)
    }

    fun inArena(coords: CoordGrid): Boolean =
        PcMap.inArena(coords) || games.values.any { it.contains(coords) }

    fun onPlayerDeath(player: Player) {
        gameOf(player)?.onPlayerDeath(player)
    }

    fun respawnCoords(player: Player): CoordGrid? =
        games.values.firstNotNullOfOrNull { it.respawnCoords(player) }

    fun onNpcKilled(npc: Npc, hero: Player) {
        gameOf(npc)?.onNpcKilled(npc, hero)
    }

    fun engage(player: Player, amount: Int) {
        gameOf(player)?.engage(player, amount)
    }

    fun sealBlast(player: Player): Int? = gameOf(player)?.sealBlast(player)

    fun points(player: Player): Int = PcPoints.get(player)

    fun spendPoints(player: Player, amount: Int): Boolean = PcPoints.spend(player, amount)

    private fun startTicking() {
        if (ticking) return
        ticking = true
        ctx.worldQueues.add(1) { tick() }
    }

    private fun tick() {
        val cycle = ctx.clock.cycle
        for (game in games.values) game.tick(cycle)
        if (games.values.none { it.isActive }) {
            ticking = false
            return
        }
        ctx.worldQueues.add(1) { tick() }
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, ctx.collision, dest)
    }
}
