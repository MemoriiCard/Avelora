package org.rsmod.content.minigames.pestcontrol

enum class PcPhase {
    Idle,
    Waiting,
    Playing,
}

enum class PcEnding {
    PortalsDestroyed,
    KnightFell,
    Timeout,
    Abandoned,
}

sealed interface PcEvent {
    data class WaitExtended(val reason: String) : PcEvent

    data class GameStarted(val players: Set<Int>) : PcEvent

    data class PortalDown(val index: Int, val by: Int?) : PcEvent

    data class KnightHurt(val hitpoints: Int) : PcEvent

    data class GameEnded(val result: PcResult) : PcEvent
}

data class PcResult(
    val won: Boolean,
    val ending: PcEnding,
    val kills: Map<Int, Int>,
    val portalKills: Map<Int, Int>,
    val participation: Map<Int, Int>,
)

class PcMatch(
    private val waitTicks: Int = WAIT_TICKS,
    private val gameTicks: Int = GAME_TICKS,
    private val maxPlayers: Int = MAX_PLAYERS,
    private val knightHitpoints: Int = KNIGHT_HITPOINTS,
) {
    var phase: PcPhase = PcPhase.Idle
        private set

    var knightHp: Int = knightHitpoints
        private set

    private val lobby = LinkedHashSet<Int>()
    private val active = LinkedHashSet<Int>()
    private val everyone = LinkedHashSet<Int>()
    private val kills = HashMap<Int, Int>()
    private val portalKills = HashMap<Int, Int>()
    private val outAt = HashMap<Int, Int>()
    private val portals = BooleanArray(PORTALS) { true }
    private var startedAt = 0
    private var phaseStart = 0

    val waiting: Set<Int>
        get() = lobby

    val squad: Set<Int>
        get() = active

    val portalsAlive: Int
        get() = portals.count { it }

    fun isPortalAlive(index: Int): Boolean = portals[index]

    fun isPlaying(id: Int): Boolean = id in active

    fun isQueued(id: Int): Boolean = id in lobby

    fun killsOf(id: Int): Int = kills[id] ?: 0

    fun ticksLeft(cycle: Int): Int =
        when (phase) {
            PcPhase.Waiting -> (phaseStart + waitTicks - cycle).coerceAtLeast(0)
            PcPhase.Playing -> (startedAt + gameTicks - cycle).coerceAtLeast(0)
            PcPhase.Idle -> 0
        }

    fun join(id: Int, cycle: Int): Boolean {
        if (phase == PcPhase.Playing) return false
        if (id in lobby) return true
        if (lobby.size >= maxPlayers) return false
        lobby += id
        if (phase == PcPhase.Idle) {
            phase = PcPhase.Waiting
            phaseStart = cycle
        }
        return true
    }

    fun leave(id: Int, cycle: Int): List<PcEvent> {
        if (lobby.remove(id)) {
            if (lobby.isEmpty()) phase = PcPhase.Idle
            return emptyList()
        }
        if (id !in active) return emptyList()
        active.remove(id)
        outAt[id] = cycle
        return if (active.isEmpty()) listOf(end(cycle, false, PcEnding.Abandoned)) else emptyList()
    }

    fun pestKilled(by: Int?) {
        if (phase != PcPhase.Playing || by == null || by !in active) return
        kills.merge(by, 1, Int::plus)
    }

    fun portalKilled(index: Int, by: Int?, cycle: Int): List<PcEvent> {
        if (phase != PcPhase.Playing || !portals[index]) return emptyList()
        portals[index] = false
        if (by != null && by in active) portalKills.merge(by, 1, Int::plus)
        val events = mutableListOf<PcEvent>(PcEvent.PortalDown(index, by))
        if (portalsAlive == 0) events += end(cycle, true, PcEnding.PortalsDestroyed)
        return events
    }

    fun update(cycle: Int, pestsAlive: Int = 0): List<PcEvent> =
        when (phase) {
            PcPhase.Waiting -> updateWaiting(cycle)
            PcPhase.Playing -> updatePlaying(cycle, pestsAlive)
            PcPhase.Idle -> emptyList()
        }

    private fun updateWaiting(cycle: Int): List<PcEvent> {
        if (lobby.size < maxPlayers && cycle - phaseStart < waitTicks) return emptyList()
        everyone.clear()
        active.clear()
        kills.clear()
        portalKills.clear()
        outAt.clear()
        portals.fill(true)
        knightHp = knightHitpoints
        active += lobby
        everyone += lobby
        lobby.clear()
        startedAt = cycle
        phase = PcPhase.Playing
        return listOf(PcEvent.GameStarted(active.toSet()))
    }

    private fun updatePlaying(cycle: Int, pestsAlive: Int): List<PcEvent> {
        if (cycle - startedAt >= gameTicks) return listOf(end(cycle, false, PcEnding.Timeout))
        val elapsed = cycle - startedAt
        if (elapsed == 0 || elapsed % KNIGHT_INTERVAL != 0) return emptyList()
        val damage = pestsAlive / PESTS_PER_KNIGHT_DAMAGE
        if (damage <= 0) return emptyList()
        knightHp = (knightHp - damage).coerceAtLeast(0)
        val hurt = PcEvent.KnightHurt(knightHp)
        return if (knightHp == 0) listOf(hurt, end(cycle, false, PcEnding.KnightFell)) else listOf(hurt)
    }

    private fun end(cycle: Int, won: Boolean, ending: PcEnding): PcEvent {
        val participation = everyone.associateWith { (outAt[it] ?: cycle) - startedAt }
        val result = PcResult(won, ending, kills.toMap(), portalKills.toMap(), participation)
        active.clear()
        phase = if (lobby.isEmpty()) PcPhase.Idle else PcPhase.Waiting
        if (phase == PcPhase.Waiting) phaseStart = cycle
        return PcEvent.GameEnded(result)
    }

    companion object {
        const val WAIT_TICKS = 100
        const val GAME_TICKS = 1000
        const val MAX_PLAYERS = 25
        const val PORTALS = 4
        const val KNIGHT_HITPOINTS = 250
        const val KNIGHT_INTERVAL = 10
        const val PESTS_PER_KNIGHT_DAMAGE = 6

        fun portalHitpoints(players: Int): Int = 100 + 20 * players.coerceAtMost(MAX_PLAYERS)
    }
}
