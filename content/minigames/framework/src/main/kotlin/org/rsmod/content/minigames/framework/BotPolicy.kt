package org.rsmod.content.minigames.framework

/** Bots only make a game playable: they fill empty slots, smaller team first, and vanish once enough humans join. */
class BotPolicy(private val config: MinigameConfig) {
    fun botsNeeded(game: Minigame, realPlayers: Int): Int {
        if (!game.usesBots) return 0
        val settings = config.settings(game)
        if (realPlayers >= settings.botFreeAt) return 0
        return (settings.target - realPlayers).coerceAtLeast(0)
    }

    /** Bots to add to each team so the totals stay even, given the real players already on each team. */
    fun distribute(game: Minigame, realPerTeam: List<Int>): List<Int> {
        require(realPerTeam.size == game.teams) { "${game.key} has ${game.teams} teams" }
        var remaining = botsNeeded(game, realPerTeam.sum())
        val totals = realPerTeam.toMutableList()
        val bots = MutableList(realPerTeam.size) { 0 }
        while (remaining > 0) {
            val smallest = totals.indices.minBy { totals[it] }
            totals[smallest]++
            bots[smallest]++
            remaining--
        }
        return bots
    }

    fun botShare(game: Minigame, realPlayers: Int): Double {
        val bots = botsNeeded(game, realPlayers)
        val total = realPlayers + bots
        return if (total == 0) 0.0 else bots.toDouble() / total
    }
}
