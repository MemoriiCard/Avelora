package org.rsmod.content.minigames.trawler

internal enum class TrawlerPhase {
    Idle,
    Trawling,
    Sunk,
    Done,
}

internal sealed interface TrawlerEvent {
    data class Leak(val hull: Int) : TrawlerEvent

    data class NetTorn(val net: Int) : TrawlerEvent

    data object Sunk : TrawlerEvent

    data object TimeUp : TrawlerEvent
}

internal class TrawlerRound(private val roll: (Int) -> Int) {
    var phase = TrawlerPhase.Idle
        private set

    var players = 0
        private set

    var water = 0
        private set

    var ticks = 0
        private set

    private var intactTicks = 0
    private val leaks = BooleanArray(HULLS)
    private val nets = booleanArrayOf(true, true)
    private val points = HashMap<Int, Int>()

    val leakCount: Int
        get() = leaks.count { it }

    val waterPercent: Int
        get() = water / 10

    fun isLeaking(hull: Int): Boolean = leaks[hull]

    fun netIntact(net: Int): Boolean = nets[net]

    fun pointsOf(slot: Int): Int = points[slot] ?: 0

    fun pointsSnapshot(): Map<Int, Int> = HashMap(points)

    fun start(playerCount: Int) {
        phase = TrawlerPhase.Trawling
        players = playerCount.coerceAtLeast(1)
        water = 0
        ticks = 0
        intactTicks = 0
        leaks.fill(false)
        nets.fill(true)
        points.clear()
    }

    fun reset() {
        phase = TrawlerPhase.Idle
        players = 0
        water = 0
        ticks = 0
        intactTicks = 0
        leaks.fill(false)
        nets.fill(true)
        points.clear()
    }

    fun forfeit(slot: Int) {
        points.remove(slot)
    }

    fun tick(): List<TrawlerEvent> {
        if (phase != TrawlerPhase.Trawling) return emptyList()
        val events = ArrayList<TrawlerEvent>()
        ticks++
        if (nets.all { it }) intactTicks++
        if (ticks % leakInterval() == 0) spawnLeak()?.let { events += TrawlerEvent.Leak(it) }
        for (net in nets.indices) {
            if (nets[net] && roll(netRipOdds()) == 0) {
                nets[net] = false
                events += TrawlerEvent.NetTorn(net)
            }
        }
        if (ticks % LEAK_PERIOD == 0) water += leakCount * LEAK_TENTHS
        if (water >= MAX_WATER) {
            phase = TrawlerPhase.Sunk
            events += TrawlerEvent.Sunk
        } else if (ticks >= TRAWL_TICKS) {
            phase = TrawlerPhase.Done
            events += TrawlerEvent.TimeUp
        }
        return events
    }

    fun patch(slot: Int, hull: Int): Boolean {
        if (phase != TrawlerPhase.Trawling || !leaks[hull]) return false
        leaks[hull] = false
        award(slot, PATCH_POINTS)
        return true
    }

    fun repairNet(slot: Int, net: Int): Boolean {
        if (phase != TrawlerPhase.Trawling || nets[net]) return false
        nets[net] = true
        award(slot, NET_POINTS)
        return true
    }

    fun bail(slot: Int): Boolean {
        if (phase != TrawlerPhase.Trawling || water <= 0) return false
        water = (water - BAIL_TENTHS).coerceAtLeast(0)
        award(slot, BAIL_POINTS)
        return true
    }

    fun uptime(): Double = if (ticks == 0) 0.0 else intactTicks.toDouble() / ticks

    fun fishFor(slot: Int): Int {
        val earned = pointsOf(slot)
        if (earned < MIN_POINTS) return 0
        val base = if (players == 1) SOLO_BASE else GROUP_BASE
        val bonus = 1.0 + 0.5 * (earned.coerceAtMost(MAX_POINTS) - MIN_POINTS) / (MAX_POINTS - MIN_POINTS)
        return Math.round(base * uptime() * bonus).toInt()
    }

    private fun award(slot: Int, amount: Int) {
        points[slot] = ((points[slot] ?: 0) + amount).coerceAtMost(MAX_POINTS)
    }

    private fun leakInterval(): Int = (BASE_LEAK_INTERVAL / players).coerceAtLeast(MIN_LEAK_INTERVAL)

    private fun netRipOdds(): Int = (BASE_NET_ODDS / players).coerceAtLeast(MIN_NET_ODDS)

    private fun spawnLeak(): Int? {
        val free = leaks.indices.filter { !leaks[it] }
        if (free.isEmpty()) return null
        val hull = free[roll(free.size)]
        leaks[hull] = true
        return hull
    }

    companion object {
        const val HULLS = 16
        const val TRAWL_TICKS = 500
        const val MAX_WATER = 1000
        const val LEAK_PERIOD = 3
        const val LEAK_TENTHS = 1
        const val BAIL_TENTHS = 20
        const val BASE_LEAK_INTERVAL = 40
        const val MIN_LEAK_INTERVAL = 10
        const val BASE_NET_ODDS = 150
        const val MIN_NET_ODDS = 40
        const val PATCH_POINTS = 5
        const val NET_POINTS = 7
        const val BAIL_POINTS = 2
        const val MIN_POINTS = 50
        const val MAX_POINTS = 255
        const val SOLO_BASE = 14
        const val GROUP_BASE = 9
    }
}
