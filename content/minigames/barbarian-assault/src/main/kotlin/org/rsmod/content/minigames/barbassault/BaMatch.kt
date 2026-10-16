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

    data class WaveStarted(val wave: Int, val kinds: List<BaKind>) : BaEvent

    data class WaveCleared(val wave: Int) : BaEvent

    data class PlayerDown(val id: Int) : BaEvent

    data class GameEnded(val result: BaResult) : BaEvent
}

data class BaResult(
    val cleared: Boolean,
    val wavesCleared: Int,
    val queenKilled: Boolean,
    val roles: Map<Int, BaRole>,
    val points: Map<Int, Map<BaRole, Int>>,
    val kills: Map<Int, Int>,
    val participation: Map<Int, Int>,
)

class BaMatch(
    private val waitTicks: Int = WAIT_TICKS,
    private val intermissionTicks: Int = INTERMISSION_TICKS,
    private val waves: Int = BaWave.FINAL,
    private val maxPlayers: Int = MAX_PLAYERS,
) {
    var phase: BaPhase = BaPhase.Idle
        private set

    var wave: Int = 0
        private set

    var wavesCleared: Int = 0
        private set

    private val lobby = LinkedHashMap<Int, BaRole>()
    private val active = LinkedHashSet<Int>()
    private val roles = LinkedHashMap<Int, BaRole>()
    private val kills = HashMap<Int, Int>()
    private val stats = HashMap<Int, BaWaveStats>()
    private val totals = HashMap<Int, MutableMap<BaRole, Int>>()
    private val outAt = HashMap<Int, Int>()
    private var startedAt = 0
    private var phaseStart = 0
    private var monstersTotal = 0
    private var monstersDown = 0
    private var queenKilled = false

    val waiting: Set<Int>
        get() = lobby.keys

    val squad: Set<Int>
        get() = active

    val monstersLeft: Int
        get() = monstersTotal - monstersDown

    fun isPlaying(id: Int): Boolean = id in active

    fun isQueued(id: Int): Boolean = id in lobby

    fun roleOf(id: Int): BaRole? = roles[id] ?: lobby[id]

    fun killsOf(id: Int): Int = kills[id] ?: 0

    fun pointsOf(id: Int, role: BaRole): Int = totals[id]?.get(role) ?: 0

    fun waveTicks(wave: Int): Int = if (wave >= BaWave.FINAL) FINAL_WAVE_TICKS else WAVE_TICKS

    fun ticksLeft(cycle: Int): Int =
        when (phase) {
            BaPhase.Waiting -> (phaseStart + waitTicks - cycle).coerceAtLeast(0)
            BaPhase.Wave -> (phaseStart + waveTicks(wave) - cycle).coerceAtLeast(0)
            BaPhase.Intermission -> (phaseStart + intermissionTicks - cycle).coerceAtLeast(0)
            BaPhase.Idle -> 0
        }

    fun join(id: Int, role: BaRole, cycle: Int): Boolean {
        if (phase != BaPhase.Idle && phase != BaPhase.Waiting) return false
        if (id in lobby) return true
        if (lobby.size >= maxPlayers) return false
        lobby[id] = role
        if (phase == BaPhase.Idle) {
            phase = BaPhase.Waiting
            phaseStart = cycle
        }
        return true
    }

    fun leave(id: Int, cycle: Int): List<BaEvent> {
        if (lobby.remove(id) != null) {
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

    fun monsterKilled(kind: BaKind, by: Int?, cycle: Int): List<BaEvent> {
        if (phase != BaPhase.Wave) return emptyList()
        if (by != null && by in active) {
            kills.merge(by, 1, Int::plus)
            if (kind.targetedBy != null && roles[by] == kind.targetedBy) statsOf(by).kills++
        }
        if (kind == BaKind.Queen) {
            queenKilled = true
            monstersDown = monstersTotal
        } else {
            monstersDown++
        }
        return completeIfDone(cycle)
    }

    fun runnerEscaped(cycle: Int): List<BaEvent> {
        if (phase != BaPhase.Wave) return emptyList()
        for (id in active) if (roles[id] == BaRole.Defender) statsOf(id).escapes++
        monstersDown++
        return completeIfDone(cycle)
    }

    fun healed(id: Int, hitpoints: Int) {
        if (id in active && phase == BaPhase.Wave && roles[id] == BaRole.Healer) {
            statsOf(id).healed += hitpoints
        }
    }

    fun eggCollected(id: Int) {
        if (id in active && phase == BaPhase.Wave && roles[id] == BaRole.Collector) statsOf(id).eggs++
    }

    fun abort(cycle: Int): List<BaEvent> = listOf(end(cycle, false))

    fun update(cycle: Int): List<BaEvent> =
        when (phase) {
            BaPhase.Waiting -> updateWaiting(cycle)
            BaPhase.Wave ->
                if (cycle - phaseStart >= waveTicks(wave)) listOf(end(cycle, false)) else emptyList()
            BaPhase.Intermission ->
                if (cycle - phaseStart >= intermissionTicks) startWave(wave + 1, cycle) else emptyList()
            BaPhase.Idle -> emptyList()
        }

    private fun statsOf(id: Int): BaWaveStats = stats.getOrPut(id) { BaWaveStats() }

    private fun completeIfDone(cycle: Int): List<BaEvent> {
        if (monstersDown < monstersTotal) return emptyList()
        wavesCleared = wave
        scoreWave()
        val events = mutableListOf<BaEvent>(BaEvent.WaveCleared(wave))
        if (wave >= waves) {
            events += end(cycle, true)
        } else {
            phase = BaPhase.Intermission
            phaseStart = cycle
        }
        return events
    }

    private fun scoreWave() {
        for (id in active) {
            val role = roles.getValue(id)
            val earned = totals.getOrPut(id) { mutableMapOf() }
            if (wave >= BaWave.FINAL) {
                for (other in BaRole.entries) {
                    val award = if (other == role) BaScoring.FINAL_WAVE_ROLE else BaScoring.FINAL_WAVE_OTHER
                    earned.merge(other, award, Int::plus)
                }
            } else {
                earned.merge(role, BaScoring.wavePoints(role, wave, statsOf(id)), Int::plus)
            }
            statsOf(id).reset()
        }
    }

    private fun updateWaiting(cycle: Int): List<BaEvent> {
        if (lobby.size < maxPlayers && cycle - phaseStart < waitTicks) return emptyList()
        active.clear()
        roles.clear()
        kills.clear()
        stats.clear()
        totals.clear()
        outAt.clear()
        queenKilled = false
        roles.putAll(lobby)
        active += lobby.keys
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
        val kinds = BaWave.compose(next, active.size)
        monstersTotal = kinds.size
        monstersDown = 0
        return listOf(BaEvent.WaveStarted(next, kinds))
    }

    private fun end(cycle: Int, cleared: Boolean): BaEvent {
        val participation = roles.keys.associateWith { (outAt[it] ?: cycle) - startedAt }
        val points = roles.keys.associateWith { totals[it]?.toMap() ?: emptyMap() }
        val result =
            BaResult(cleared, wavesCleared, queenKilled, roles.toMap(), points, kills.toMap(), participation)
        active.clear()
        phase = if (lobby.isEmpty()) BaPhase.Idle else BaPhase.Waiting
        if (phase == BaPhase.Waiting) phaseStart = cycle
        return BaEvent.GameEnded(result)
    }

    companion object {
        const val WAIT_TICKS = 100
        const val WAVE_TICKS = 400
        const val FINAL_WAVE_TICKS = 800
        const val INTERMISSION_TICKS = 25
        const val MAX_PLAYERS = 5
    }
}
