package org.rsmod.content.minigames.castlewars

enum class CwTeam {
    Saradomin,
    Zamorak;

    val other: CwTeam
        get() = if (this == Saradomin) Zamorak else Saradomin
}

enum class CwPhase {
    Idle,
    Waiting,
    Playing,
}

sealed interface CwEvent {
    data class GameStarted(val players: Map<Int, CwTeam>, val bots: Map<CwTeam, Int>) : CwEvent

    data class WaitExtended(val reason: String) : CwEvent

    data class FlagTaken(val taker: Int, val flagOf: CwTeam) : CwEvent

    data class FlagDropped(val flagOf: CwTeam) : CwEvent

    data class FlagReturned(val flagOf: CwTeam) : CwEvent

    data class Scored(val scorer: Int, val team: CwTeam) : CwEvent

    data class GameEnded(val result: CwResult) : CwEvent
}

data class CwResult(
    val winner: CwTeam?,
    val scores: Map<CwTeam, Int>,
    val players: Map<Int, CwTeam>,
    val participation: Map<Int, Int>,
    val botShare: Double = 0.0,
)

class CastleWarsMatch(
    private val waitTicks: Int = WAIT_TICKS,
    private val gameTicks: Int = GAME_TICKS,
    private val botPlan: (saradomin: Int, zamorak: Int) -> Map<CwTeam, Int> = { _, _ -> emptyMap() },
) {
    var phase: CwPhase = CwPhase.Idle
        private set

    private val lobbyTeams = LinkedHashMap<Int, CwTeam>()
    private val gameTeams = LinkedHashMap<Int, CwTeam>()
    private val joinedAt = HashMap<Int, Int>()
    private val carriers = HashMap<CwTeam, Int>()
    private val dropped = HashMap<CwTeam, Int>()
    private val scores = CwTeam.entries.associateWith { 0 }.toMutableMap()
    private var phaseStart = 0
    private var bots: Map<CwTeam, Int> = emptyMap()

    val waiting: Map<Int, CwTeam>
        get() = lobbyTeams

    val playing: Map<Int, CwTeam>
        get() = gameTeams

    fun teamOf(id: Int): CwTeam? = lobbyTeams[id] ?: gameTeams[id]

    fun isPlaying(id: Int): Boolean = id in gameTeams

    fun score(team: CwTeam): Int = scores.getValue(team)

    fun carrierOf(flagOf: CwTeam): Int? = carriers[flagOf]

    fun isDropped(flagOf: CwTeam): Boolean = flagOf in dropped

    fun carriedFlag(id: Int): CwTeam? = carriers.entries.firstOrNull { it.value == id }?.key

    fun ticksLeft(cycle: Int): Int =
        when (phase) {
            CwPhase.Waiting -> (phaseStart + waitTicks - cycle).coerceAtLeast(0)
            CwPhase.Playing -> (phaseStart + gameTicks - cycle).coerceAtLeast(0)
            CwPhase.Idle -> 0
        }

    fun join(id: Int, preferred: CwTeam?, cycle: Int): CwTeam? {
        if (id in gameTeams) return null
        lobbyTeams[id]?.let { return it }
        val team = pickTeam(preferred, lobbyTeams.values) ?: return null
        lobbyTeams[id] = team
        if (phase == CwPhase.Idle) {
            phase = CwPhase.Waiting
            phaseStart = cycle
        }
        return team
    }

    fun leave(id: Int, cycle: Int): List<CwEvent> {
        lobbyTeams.remove(id)
        val events = mutableListOf<CwEvent>()
        if (id in gameTeams) {
            returnFlagCarriedBy(id)?.let { events += CwEvent.FlagReturned(it) }
            val team = gameTeams.remove(id)
            joinedAt.remove(id)
            if (phase == CwPhase.Playing && team != null) {
                if (gameTeams.isEmpty()) {
                    events += end(cycle)
                } else if (gameTeams.values.none { it == team } && botsOf(team) == 0) {
                    events += end(cycle, forfeitWinner = team.other)
                }
            }
        }
        if (phase == CwPhase.Waiting && lobbyTeams.isEmpty()) phase = CwPhase.Idle
        return events
    }

    fun update(cycle: Int): List<CwEvent> =
        when (phase) {
            CwPhase.Waiting -> if (cycle - phaseStart >= waitTicks) startOrExtend(cycle) else emptyList()
            CwPhase.Playing ->
                if (cycle - phaseStart >= gameTicks) end(cycle) else expireDropped(cycle)
            CwPhase.Idle -> emptyList()
        }

    fun takeFlag(id: Int, standOf: CwTeam): CwEvent? {
        val team = gameTeams[id] ?: return null
        if (phase != CwPhase.Playing || team == standOf) return null
        if (carriers.containsKey(standOf) || standOf in dropped || carriedFlag(id) != null) return null
        carriers[standOf] = id
        return CwEvent.FlagTaken(id, standOf)
    }

    fun pickUp(id: Int, flagOf: CwTeam): CwEvent? {
        val team = gameTeams[id] ?: return null
        if (phase != CwPhase.Playing || flagOf !in dropped) return null
        if (team == flagOf) {
            dropped.remove(flagOf)
            return CwEvent.FlagReturned(flagOf)
        }
        if (carriedFlag(id) != null) return null
        dropped.remove(flagOf)
        carriers[flagOf] = id
        return CwEvent.FlagTaken(id, flagOf)
    }

    fun capture(id: Int): CwEvent? {
        val team = gameTeams[id] ?: return null
        if (phase != CwPhase.Playing) return null
        val flag = carriedFlag(id) ?: return null
        if (carriers.containsKey(team)) return null
        carriers.remove(flag)
        scores[team] = scores.getValue(team) + 1
        return CwEvent.Scored(id, team)
    }

    fun onDeath(id: Int, cycle: Int): CwEvent? {
        val flag = returnFlagCarriedBy(id) ?: return null
        dropped[flag] = cycle
        return CwEvent.FlagDropped(flag)
    }

    private fun botsOf(team: CwTeam): Int = bots[team] ?: 0

    private fun expireDropped(cycle: Int): List<CwEvent> {
        val expired = dropped.filterValues { cycle - it >= DROP_RETURN_TICKS }.keys.toList()
        expired.forEach(dropped::remove)
        return expired.map { CwEvent.FlagReturned(it) }
    }

    private fun returnFlagCarriedBy(id: Int): CwTeam? {
        val flag = carriedFlag(id) ?: return null
        carriers.remove(flag)
        return flag
    }

    private fun pickTeam(preferred: CwTeam?, current: Collection<CwTeam>): CwTeam? {
        val counts = CwTeam.entries.associateWith { team -> current.count { it == team } }
        val smaller = CwTeam.entries.minBy { counts.getValue(it) }
        if (preferred == null) return smaller
        val gap = counts.getValue(preferred) - counts.getValue(preferred.other)
        return if (gap >= MAX_TEAM_GAP) null else preferred
    }

    private fun startOrExtend(cycle: Int): List<CwEvent> {
        val saradomin = lobbyTeams.values.count { it == CwTeam.Saradomin }
        val zamorak = lobbyTeams.size - saradomin
        val plan = botPlan(saradomin, zamorak)
        val sideless = (saradomin == 0 && (plan[CwTeam.Saradomin] ?: 0) == 0) || (zamorak == 0 && (plan[CwTeam.Zamorak] ?: 0) == 0)
        if (sideless) {
            phaseStart = cycle
            return listOf(CwEvent.WaitExtended("Castle Wars needs at least one player on each team."))
        }
        bots = plan
        gameTeams.putAll(lobbyTeams)
        lobbyTeams.clear()
        gameTeams.keys.forEach { joinedAt[it] = cycle }
        carriers.clear()
        dropped.clear()
        scores.keys.forEach { scores[it] = 0 }
        phase = CwPhase.Playing
        phaseStart = cycle
        return listOf(CwEvent.GameStarted(gameTeams.toMap(), bots))
    }

    private fun end(cycle: Int, forfeitWinner: CwTeam? = null): List<CwEvent> {
        val participation = joinedAt.mapValues { (_, start) -> cycle - start }
        val sara = score(CwTeam.Saradomin)
        val zam = score(CwTeam.Zamorak)
        val winner =
            when {
                forfeitWinner != null -> forfeitWinner
                sara > zam -> CwTeam.Saradomin
                zam > sara -> CwTeam.Zamorak
                else -> null
            }
        val everyone = LinkedHashMap(gameTeams)
        val botTotal = bots.values.sum()
        val total = everyone.size + botTotal
        val botShare = if (total == 0) 0.0 else botTotal.toDouble() / total
        val result =
            CwResult(winner, mapOf(CwTeam.Saradomin to sara, CwTeam.Zamorak to zam), everyone, participation, botShare)
        gameTeams.clear()
        joinedAt.clear()
        carriers.clear()
        dropped.clear()
        bots = emptyMap()
        phase = if (lobbyTeams.isEmpty()) CwPhase.Idle else CwPhase.Waiting
        phaseStart = cycle
        return listOf(CwEvent.GameEnded(result))
    }

    companion object {
        const val WAIT_TICKS = 100
        const val GAME_TICKS = 1000
        const val MAX_TEAM_GAP = 2
        const val DROP_RETURN_TICKS = 100
    }
}
