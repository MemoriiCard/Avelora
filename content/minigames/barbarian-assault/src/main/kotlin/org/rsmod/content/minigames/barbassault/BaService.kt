package org.rsmod.content.minigames.barbassault

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid

@Singleton
class BaService @Inject internal constructor(private val ctx: BaContext) {
    private val games = mutableListOf<BaGame>()
    private var ticking = false

    internal fun gameOf(player: Player): BaGame? = games.firstOrNull { it.isPlaying(player) }

    internal fun gameOf(npc: Npc): BaGame? = games.firstOrNull { it.isMonster(npc) }

    private fun queuedIn(player: Player): BaGame? = games.firstOrNull { it.isQueued(player) }

    fun isPlaying(player: Player): Boolean = gameOf(player) != null

    fun isQueued(player: Player): Boolean = queuedIn(player) != null

    fun isMonster(npc: Npc): Boolean = gameOf(npc) != null

    fun leaveQueue(player: Player) {
        val queued = queuedIn(player) ?: return
        queued.leaveQueue(player)
        player.mes("You leave the Barbarian Assault queue.")
    }

    fun joinQueue(player: Player, role: BaRole) {
        if (isPlaying(player)) {
            player.mes("You're already in a run.")
            return
        }
        val game = openGame()
        if (game == null || !game.join(player, role)) {
            player.mes("Every cavern is busy. Try again in a minute.")
            return
        }
        startTicking()
        player.mes(
            "You join the queue as ${role.label}. The run starts in " +
                "${game.match.ticksLeft(ctx.clock.cycle) * 6 / 10} seconds, or sooner with a full squad.",
        )
    }

    private fun openGame(): BaGame? {
        games
            .firstOrNull {
                (it.match.phase == BaPhase.Waiting || it.match.phase == BaPhase.Idle) &&
                    it.match.waiting.size < BaMatch.MAX_PLAYERS
            }
            ?.let { return it }
        if (games.size >= MAX_GAMES) return null
        return BaGame(ctx).also { games += it }
    }

    fun leave(player: Player, toOutpost: Boolean = true) {
        queuedIn(player)?.leaveQueue(player)
        val game = gameOf(player)
        if (game != null) {
            game.leave(player, toOutpost)
            return
        }
        if (toOutpost && inArena(player.coords)) teleport(player, BaMap.OUTPOST)
    }

    fun inArena(coords: CoordGrid): Boolean =
        BaMap.inArena(coords) || games.any { it.contains(coords) }

    fun onPlayerDeath(player: Player) {
        gameOf(player)?.onPlayerDeath(player)
    }

    fun respawnCoords(player: Player): CoordGrid? = games.firstNotNullOfOrNull { it.respawnCoords(player) }

    fun onMonsterKilled(npc: Npc, hero: Player) {
        gameOf(npc)?.onMonsterKilled(npc, hero)
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        ctx.worldQueues.add(1) { tick() }
    }

    private fun tick() {
        val cycle = ctx.clock.cycle
        for (game in games) game.tick(cycle)
        if (games.none { it.isActive }) {
            ticking = false
            return
        }
        ctx.worldQueues.add(1) { tick() }
    }

    private fun teleport(player: Player, dest: CoordGrid) {
        PathingEntityCommon.telejump(player, ctx.collision, dest)
    }

    companion object {
        private const val MAX_GAMES = 4
    }
}
