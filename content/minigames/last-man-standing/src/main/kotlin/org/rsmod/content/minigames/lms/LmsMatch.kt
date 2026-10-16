package org.rsmod.content.minigames.lms

enum class LmsPhase {
    Idle,
    Waiting,
    Playing,
}

sealed interface LmsEvent {
    data class WaitExtended(val reason: String) : LmsEvent

    data class GameStarted(val players: Set<Int>) : LmsEvent

    data class Eliminated(val id: Int, val placement: Int, val killer: Int?) : LmsEvent

    data class GameEnded(val result: LmsResult) : LmsEvent
}

data class LmsResult(
    val winner: Int?,
    val placements: Map<Int, Int>,
    val kills: Map<Int, Int>,
    val participation: Map<Int, Int>,
)

class LmsMatch(
    private val waitTicks: Int = WAIT_TICKS,
    private val gameTicks: Int = GAME_TICKS,
    private val minPlayers: Int = MIN_PLAYERS,
    private val maxPlayers: Int = MAX_PLAYERS,
) {
    var phase: LmsPhase = LmsPhase.Idle
        private set

    private val lobby = LinkedHashSet<Int>()
    private val alive = LinkedHashSet<Int>()
    private val everyone = LinkedHashSet<Int>()
    private val placements = HashMap<Int, Int>()
    private val kills = HashMap<Int, Int>()
    private val outAt = HashMap<Int, Int>()
    private var startedAt = 0
    private var phaseStart = 0

    val waiting: Set<Int>
        get() = lobby

    val survivors: Set<Int>
        get() = alive

    fun isPlaying(id: Int): Boolean = id in alive

    fun isQueued(id: Int): Boolean = id in lobby

    fun elapsed(cycle: Int): Int = cycle - startedAt

    fun killsOf(id: Int): Int = kills[id] ?: 0

    fun ticksLeft(cycle: Int): Int =
        when (phase) {
            LmsPhase.Waiting -> (phaseStart + waitTicks - cycle).coerceAtLeast(0)
            LmsPhase.Playing -> (startedAt + gameTicks - cycle).coerceAtLeast(0)
            LmsPhase.Idle -> 0
        }

    fun join(id: Int, cycle: Int): Boolean {
        if (id in alive || id in lobby || lobby.size >= maxPlayers) return false
        lobby += id
        if (phase == LmsPhase.Idle) {
            phase = LmsPhase.Waiting
            phaseStart = cycle
        }
        return true
    }

    fun leave(id: Int, cycle: Int): List<LmsEvent> {
        lobby -= id
        if (phase == LmsPhase.Waiting && lobby.isEmpty()) phase = LmsPhase.Idle
        return if (id in alive) eliminate(id, null, cycle) else emptyList()
    }

    fun update(cycle: Int): List<LmsEvent> =
        when (phase) {
            LmsPhase.Waiting -> if (cycle - phaseStart >= waitTicks) startOrExtend(cycle) else emptyList()
            LmsPhase.Playing -> if (cycle - startedAt >= gameTicks) timeout(cycle) else emptyList()
            LmsPhase.Idle -> emptyList()
        }

    fun eliminate(id: Int, killer: Int?, cycle: Int): List<LmsEvent> {
        if (phase != LmsPhase.Playing || id !in alive) return emptyList()
        val placement = alive.size
        alive -= id
        placements[id] = placement
        outAt[id] = cycle
        val credited = killer?.takeIf { it in alive }
        if (credited != null) kills.merge(credited, 1, Int::plus)
        val events = mutableListOf<LmsEvent>(LmsEvent.Eliminated(id, placement, credited))
        if (alive.size <= 1) events += end(cycle, alive.firstOrNull())
        return events
    }

    private fun startOrExtend(cycle: Int): List<LmsEvent> {
        if (lobby.size < minPlayers) {
            phaseStart = cycle
            return listOf(LmsEvent.WaitExtended("Last Man Standing needs at least $minPlayers players."))
        }
        alive.addAll(lobby)
        everyone.clear()
        everyone.addAll(lobby)
        lobby.clear()
        placements.clear()
        kills.clear()
        outAt.clear()
        phase = LmsPhase.Playing
        startedAt = cycle
        return listOf(LmsEvent.GameStarted(alive.toSet()))
    }

    private fun timeout(cycle: Int): List<LmsEvent> {
        val best = alive.maxOf { killsOf(it) }
        val leaders = alive.filter { killsOf(it) == best }
        return end(cycle, leaders.singleOrNull())
    }

    private fun end(cycle: Int, winner: Int?): List<LmsEvent> {
        val finalPlacements = HashMap(placements)
        for (id in alive) finalPlacements[id] = 1
        val participation = everyone.associateWith { (outAt[it] ?: cycle) - startedAt }
        val result = LmsResult(winner, finalPlacements, HashMap(kills), participation)
        alive.clear()
        everyone.clear()
        phase = if (lobby.isEmpty()) LmsPhase.Idle else LmsPhase.Waiting
        phaseStart = cycle
        return listOf(LmsEvent.GameEnded(result))
    }

    companion object {
        const val WAIT_TICKS = 100
        const val GAME_TICKS = 1200
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 40
    }
}
