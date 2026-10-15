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

    data class ShieldDropped(val index: Int) : PcEvent

    data class KnightHurt(val hitpoints: Int) : PcEvent

    data class ActivityLow(val player: Int) : PcEvent

    data class ActivityDepleted(val player: Int) : PcEvent

    data class GameEnded(val result: PcResult) : PcEvent
}

data class PcResult(
    val won: Boolean,
    val ending: PcEnding,
    val kills: Map<Int, Int>,
    val portalKills: Map<Int, Int>,
    val participation: Map<Int, Int>,
    val idled: Set<Int> = emptySet(),
    val forfeited: Set<Int> = emptySet(),
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
    private val activity = HashMap<Int, Int>()
    private val idled = HashSet<Int>()
    private val forfeited = HashSet<Int>()
    private val portals = BooleanArray(PORTALS) { true }
    private val shields = BooleanArray(PORTALS) { true }
    private var startedAt = 0
    private var phaseStart = 0

    val waiting: Set<Int>
        get() = lobby

    val squad: Set<Int>
        get() = active

    val portalsAlive: Int
        get() = portals.count { it }

    fun isPortalAlive(index: Int): Boolean = portals[index]

    fun isShielded(index: Int): Boolean = shields[index]

    fun isPlaying(id: Int): Boolean = id in active

    fun isQueued(id: Int): Boolean = id in lobby

    fun killsOf(id: Int): Int = kills[id] ?: 0

    fun activityOf(id: Int): Int = activity[id] ?: 0

    fun hasIdled(id: Int): Boolean = id in idled

    fun engage(id: Int, amount: Int) {
        if (phase != PcPhase.Playing || id !in active || id in idled) return
        activity[id] = ((activity[id] ?: 0) + amount).coerceAtMost(ACTIVITY_MAX)
    }

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

    fun leave(id: Int, cycle: Int, forfeit: Boolean = false): List<PcEvent> {
        if (lobby.remove(id)) {
            if (lobby.isEmpty()) phase = PcPhase.Idle
            return emptyList()
        }
        if (id !in active) return emptyList()
        active.remove(id)
        if (forfeit) forfeited += id
        outAt[id] = cycle
        return if (active.isEmpty()) listOf(end(cycle, false, PcEnding.Abandoned)) else emptyList()
    }

    fun pestKilled(by: Int?) {
        if (phase != PcPhase.Playing || by == null || by !in active) return
        kills.merge(by, 1, Int::plus)
    }

    fun portalKilled(index: Int, by: Int?, cycle: Int): List<PcEvent> {
        if (phase != PcPhase.Playing || !portals[index]) return emptyList()
        if (shields[index]) return emptyList()
        portals[index] = false
        knightHp = (knightHp + PORTAL_KNIGHT_HEAL).coerceAtMost(knightHitpoints)
        if (by != null && by in active) portalKills.merge(by, 1, Int::plus)
        val events = mutableListOf<PcEvent>(PcEvent.PortalDown(index, by))
        if (portalsAlive == 0) events += end(cycle, true, PcEnding.PortalsDestroyed)
        return events
    }

    fun abort(cycle: Int): List<PcEvent> =
        if (phase == PcPhase.Playing) listOf(end(cycle, false, PcEnding.Abandoned)) else emptyList()

    fun update(cycle: Int, knightDamage: Int = 0): List<PcEvent> =
        when (phase) {
            PcPhase.Waiting -> updateWaiting(cycle)
            PcPhase.Playing -> updatePlaying(cycle, knightDamage)
            PcPhase.Idle -> emptyList()
        }

    private fun updateWaiting(cycle: Int): List<PcEvent> {
        if (lobby.size < maxPlayers && cycle - phaseStart < waitTicks) return emptyList()
        everyone.clear()
        active.clear()
        kills.clear()
        portalKills.clear()
        outAt.clear()
        activity.clear()
        idled.clear()
        forfeited.clear()
        portals.fill(true)
        shields.fill(true)
        knightHp = knightHitpoints
        active += lobby
        everyone += lobby
        for (id in lobby) activity[id] = ACTIVITY_START
        lobby.clear()
        startedAt = cycle
        phase = PcPhase.Playing
        return listOf(PcEvent.GameStarted(active.toSet()))
    }

    private fun updatePlaying(cycle: Int, knightDamage: Int): List<PcEvent> {
        if (cycle - startedAt >= gameTicks) return listOf(end(cycle, true, PcEnding.Timeout))
        val elapsed = cycle - startedAt
        val events = mutableListOf<PcEvent>()
        shieldIndexAt(elapsed)?.takeIf { shields[it] }?.let {
            shields[it] = false
            events += PcEvent.ShieldDropped(it)
        }
        if (elapsed > 0 && elapsed % ACTIVITY_DECAY_INTERVAL == 0) events += decayActivity()
        if (elapsed == 0 || elapsed % KNIGHT_INTERVAL != 0) return events
        val damage = knightDamage
        if (damage <= 0) return events
        knightHp = (knightHp - damage).coerceAtLeast(0)
        events += PcEvent.KnightHurt(knightHp)
        if (knightHp == 0) events += end(cycle, false, PcEnding.KnightFell)
        return events
    }

    private fun decayActivity(): List<PcEvent> {
        val events = mutableListOf<PcEvent>()
        for (id in active) {
            if (id in idled) continue
            val level = (activity[id] ?: 0) - 1
            activity[id] = level.coerceAtLeast(0)
            when {
                level <= 0 -> {
                    idled += id
                    events += PcEvent.ActivityDepleted(id)
                }
                level == ACTIVITY_WARNING -> events += PcEvent.ActivityLow(id)
            }
        }
        return events
    }

    private fun shieldIndexAt(elapsed: Int): Int? {
        if (elapsed < SHIELD_FIRST_TICKS) return null
        val since = elapsed - SHIELD_FIRST_TICKS
        if (since % SHIELD_GAP_TICKS != 0) return null
        return (since / SHIELD_GAP_TICKS).takeIf { it < PORTALS }
    }

    private fun end(cycle: Int, won: Boolean, ending: PcEnding): PcEvent {
        val participation = everyone.associateWith { (outAt[it] ?: cycle) - startedAt }
        val result = PcResult(won, ending, kills.toMap(), portalKills.toMap(), participation, idled.toSet(), forfeited.toSet())
        active.clear()
        phase = if (lobby.isEmpty()) PcPhase.Idle else PcPhase.Waiting
        if (phase == PcPhase.Waiting) phaseStart = cycle
        return PcEvent.GameEnded(result)
    }

    companion object {
        const val WAIT_TICKS = 100
        const val GAME_TICKS = 2000
        const val MAX_PLAYERS = 25
        const val PORTALS = 4
        const val KNIGHT_HITPOINTS = 250
        const val KNIGHT_INTERVAL = 10
        const val ENGAGE_PEST = 2
        const val ENGAGE_PORTAL = 4
        const val ACTIVITY_MAX = 100
        const val ACTIVITY_START = 60
        const val ACTIVITY_WARNING = 15
        const val ACTIVITY_DECAY_INTERVAL = 3
        const val SHIELD_FIRST_TICKS = 25
        const val SHIELD_GAP_TICKS = 50
        const val PORTAL_KNIGHT_HEAL = 50
    }
}
