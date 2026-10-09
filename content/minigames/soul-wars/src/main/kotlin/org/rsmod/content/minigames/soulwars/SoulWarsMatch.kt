package org.rsmod.content.minigames.soulwars

enum class SwTeam {
    Blue,
    Red;

    val other: SwTeam
        get() = if (this == Blue) Red else Blue
}

enum class SwPhase {
    Idle,
    Waiting,
    Playing,
}

sealed interface SwEvent {
    data class WaitExtended(val reason: String) : SwEvent

    data class GameStarted(val players: Map<Int, SwTeam>) : SwEvent

    data class Sacrificed(val id: Int, val fragments: Int, val team: SwTeam) : SwEvent

    data class GameEnded(val result: SwResult) : SwEvent
}

enum class SwEnding {
    AvatarKilled,
    Forfeit,
    Timeout,
}

data class SwResult(
    val winner: SwTeam?,
    val ending: SwEnding,
    val players: Map<Int, SwTeam>,
    val participation: Map<Int, Int>,
    val sacrificed: Map<Int, Int>,
)

class SoulWarsMatch(
    private val waitTicks: Int = WAIT_TICKS,
    private val gameTicks: Int = GAME_TICKS,
) {
    var phase: SwPhase = SwPhase.Idle
        private set

    private val lobbyTeams = LinkedHashMap<Int, SwTeam>()
    private val gameTeams = LinkedHashMap<Int, SwTeam>()
    private val joinedAt = HashMap<Int, Int>()
    private val sacrificed = HashMap<Int, Int>()
    private var phaseStart = 0

    val waiting: Map<Int, SwTeam>
        get() = lobbyTeams

    val playing: Map<Int, SwTeam>
        get() = gameTeams

    fun teamOf(id: Int): SwTeam? = lobbyTeams[id] ?: gameTeams[id]

    fun isPlaying(id: Int): Boolean = id in gameTeams

    fun fragmentsFor(team: SwTeam): Int =
        gameTeams.filterValues { it == team }.keys.sumOf { sacrificed[it] ?: 0 }

    fun ticksLeft(cycle: Int): Int =
        when (phase) {
            SwPhase.Waiting -> (phaseStart + waitTicks - cycle).coerceAtLeast(0)
            SwPhase.Playing -> (phaseStart + gameTicks - cycle).coerceAtLeast(0)
            SwPhase.Idle -> 0
        }

    fun join(id: Int, cycle: Int): SwTeam? {
        if (id in gameTeams) return null
        lobbyTeams[id]?.let { return it }
        val blue = lobbyTeams.values.count { it == SwTeam.Blue }
        val red = lobbyTeams.size - blue
        val team = if (blue <= red) SwTeam.Blue else SwTeam.Red
        lobbyTeams[id] = team
        if (phase == SwPhase.Idle) {
            phase = SwPhase.Waiting
            phaseStart = cycle
        }
        return team
    }

    fun leave(id: Int, cycle: Int): List<SwEvent> {
        lobbyTeams.remove(id)
        val team = gameTeams.remove(id)
        joinedAt.remove(id)
        val events = mutableListOf<SwEvent>()
        if (team != null && phase == SwPhase.Playing && gameTeams.values.none { it == team }) {
            events += end(cycle, SwEnding.Forfeit, team.other)
        }
        if (phase == SwPhase.Waiting && lobbyTeams.isEmpty()) phase = SwPhase.Idle
        return events
    }

    fun update(cycle: Int, avatarHp: Map<SwTeam, Double>): List<SwEvent> =
        when (phase) {
            SwPhase.Waiting -> if (cycle - phaseStart >= waitTicks) startOrExtend(cycle) else emptyList()
            SwPhase.Playing -> if (cycle - phaseStart >= gameTicks) timeout(cycle, avatarHp) else emptyList()
            SwPhase.Idle -> emptyList()
        }

    fun sacrifice(id: Int, fragments: Int): SwEvent? {
        val team = gameTeams[id] ?: return null
        if (phase != SwPhase.Playing || fragments <= 0) return null
        sacrificed.merge(id, fragments, Int::plus)
        return SwEvent.Sacrificed(id, fragments, team)
    }

    fun avatarKilled(team: SwTeam, cycle: Int): List<SwEvent> {
        if (phase != SwPhase.Playing) return emptyList()
        return end(cycle, SwEnding.AvatarKilled, team.other)
    }

    private fun startOrExtend(cycle: Int): List<SwEvent> {
        val blue = lobbyTeams.values.count { it == SwTeam.Blue }
        val red = lobbyTeams.size - blue
        if (blue == 0 || red == 0) {
            phaseStart = cycle
            return listOf(SwEvent.WaitExtended("Soul Wars needs at least one player on each team."))
        }
        gameTeams.putAll(lobbyTeams)
        lobbyTeams.clear()
        gameTeams.keys.forEach { joinedAt[it] = cycle }
        sacrificed.clear()
        phase = SwPhase.Playing
        phaseStart = cycle
        return listOf(SwEvent.GameStarted(gameTeams.toMap()))
    }

    private fun timeout(cycle: Int, avatarHp: Map<SwTeam, Double>): List<SwEvent> {
        val blueHp = avatarHp[SwTeam.Blue] ?: 1.0
        val redHp = avatarHp[SwTeam.Red] ?: 1.0
        val winner =
            when {
                blueHp > redHp -> SwTeam.Blue
                redHp > blueHp -> SwTeam.Red
                else -> fragmentWinner()
            }
        return end(cycle, SwEnding.Timeout, winner)
    }

    private fun fragmentWinner(): SwTeam? {
        val blue = fragmentsFor(SwTeam.Blue)
        val red = fragmentsFor(SwTeam.Red)
        return when {
            blue > red -> SwTeam.Blue
            red > blue -> SwTeam.Red
            else -> null
        }
    }

    private fun end(cycle: Int, ending: SwEnding, winner: SwTeam?): List<SwEvent> {
        val participation = joinedAt.mapValues { (_, start) -> cycle - start }
        val result =
            SwResult(winner, ending, LinkedHashMap(gameTeams), participation, HashMap(sacrificed))
        gameTeams.clear()
        joinedAt.clear()
        sacrificed.clear()
        phase = if (lobbyTeams.isEmpty()) SwPhase.Idle else SwPhase.Waiting
        phaseStart = cycle
        return listOf(SwEvent.GameEnded(result))
    }

    companion object {
        const val WAIT_TICKS = 100
        const val GAME_TICKS = 1000
    }
}
