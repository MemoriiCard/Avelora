package org.rsmod.content.minigames.barbassault

enum class BaPhase {
    Idle,
    Waiting,
    Wave,
    Intermission,
}

sealed interface BaEvent {
    data class WaitExtended(val reason: String) : BaEvent

    data class GameStarted(val players: Set<Int>) : BaEvent

    data class WaveStarted(val wave: Int, val monsters: Int) : BaEvent

    data class WaveCleared(val wave: Int) : BaEvent

    data class PlayerDown(val id: Int) : BaEvent

    data class GameEnded(val result: BaResult) : BaEvent
}

data class BaResult(
    val cleared: Boolean,
    val wavesCleared: Int,
    val kills: Map<Int, Int>,
    val participation: Map<Int, Int>,
)

class BaMatch(
    private val waitTicks: Int = WAIT_TICKS,
    private val waveTicks: Int = WAVE_TICKS,
    private val intermissionTicks: Int = INTERMISSION_TICKS,
    private val waves: Int = WAVES,
    private val maxPlayers: Int = MAX_PLAYERS,
) {
    var phase: BaPhase = BaPhase.Idle
        private set

    var wave: Int = 0
        private set

    var wavesCleared: Int = 0
        private set

    private val lobby = LinkedHashSet<Int>()
    private val active = LinkedHashSet<Int>()
    private val everyone = LinkedHashSet<Int>()
    private val kills = HashMap<Int, Int>()
    private val outAt = HashMap<Int, Int>()
    private var startedAt = 0
    private var phaseStart = 0
    private var monstersTotal = 0
    private var monstersDown = 0

    val waiting: Set<Int>
        get() = lobby

    val squad: Set<Int>
        get() = active

    val monstersLeft: Int
        get() = monstersTotal - monstersDown

    fun isPlaying(id: Int): Boolean = id in active

    fun isQueued(id: Int): Boolean = id in lobby

    fun killsOf(id: Int): Int = kills[id] ?: 0

    fun ticksLeft(cycle: Int): Int =
        when (phase) {
            BaPhase.Waiting -> (phaseStart + waitTicks - cycle).coerceAtLeast(0)
            BaPhase.Wave -> (phaseStart + waveTicks - cycle).coerceAtLeast(0)
            BaPhase.Intermission -> (phaseStart + intermissionTicks - cycle).coerceAtLeast(0)
            BaPhase.Idle -> 0
        }

    fun join(id: Int, cycle: Int): Boolean {
        if (phase != BaPhase.Idle && phase != BaPhase.Waiting) return false
        if (id in lobby) return true
        if (lobby.size >= maxPlayers) return false
        lobby += id
        if (phase == BaPhase.Idle) {
            phase = BaPhase.Waiting
            phaseStart = cycle
        }
        return true
    }

    fun leave(id: Int, cycle: Int): List<BaEvent> {
        if (lobby.remove(id)) {
            if (lobby.isEmpty()) phase = BaPhase.Idle
            return emptyList()
        }
        if (id !in active) return emptyList()
        active.remove(id)
        outAt[id] = cycle
        val events = mutableListOf<BaEvent>(BaEvent.PlayerDown(id))
        if (active.isEmpty()) events += end(cycle, false)
        return events
    }

    fun monsterKilled(by: Int?, cycle: Int): List<BaEvent> {
        if (phase != BaPhase.Wave) return emptyList()
        if (by != null && by in active) kills.merge(by, 1, Int::plus)
        monstersDown++
        if (monstersDown < monstersTotal) return emptyList()
        wavesCleared = wave
        val events = mutableListOf<BaEvent>(BaEvent.WaveCleared(wave))
        if (wave >= waves) {
            events += end(cycle, true)
        } else {
            phase = BaPhase.Intermission
            phaseStart = cycle
        }
        return events
    }

    fun update(cycle: Int): List<BaEvent> =
        when (phase) {
            BaPhase.Waiting -> updateWaiting(cycle)
            BaPhase.Wave ->
                if (cycle - phaseStart >= waveTicks) end(cycle, false).let { listOf(it) } else emptyList()
            BaPhase.Intermission ->
                if (cycle - phaseStart >= intermissionTicks) startWave(wave + 1, cycle) else emptyList()
            BaPhase.Idle -> emptyList()
        }

    private fun updateWaiting(cycle: Int): List<BaEvent> {
        if (lobby.size < maxPlayers && cycle - phaseStart < waitTicks) return emptyList()
        everyone.clear()
        active.clear()
        kills.clear()
        outAt.clear()
        active += lobby
        everyone += lobby
        lobby.clear()
        startedAt = cycle
        wave = 0
        wavesCleared = 0
        return listOf<BaEvent>(BaEvent.GameStarted(active.toSet())) + startWave(1, cycle)
    }

    private fun startWave(next: Int, cycle: Int): List<BaEvent> {
        wave = next
        phase = BaPhase.Wave
        phaseStart = cycle
        monstersTotal = monstersFor(next, active.size)
        monstersDown = 0
        return listOf(BaEvent.WaveStarted(next, monstersTotal))
    }

    private fun end(cycle: Int, cleared: Boolean): BaEvent {
        val participation = everyone.associateWith { (outAt[it] ?: cycle) - startedAt }
        val result = BaResult(cleared, wavesCleared, kills.toMap(), participation)
        active.clear()
        phase = if (lobby.isEmpty()) BaPhase.Idle else BaPhase.Waiting
        if (phase == BaPhase.Waiting) phaseStart = cycle
        return BaEvent.GameEnded(result)
    }

    companion object {
        const val WAIT_TICKS = 100
        const val WAVE_TICKS = 300
        const val INTERMISSION_TICKS = 25
        const val WAVES = 5
        const val MAX_PLAYERS = 3

        fun monstersFor(wave: Int, players: Int): Int = 4 + wave * 2 + (players - 1) * wave
    }
}
