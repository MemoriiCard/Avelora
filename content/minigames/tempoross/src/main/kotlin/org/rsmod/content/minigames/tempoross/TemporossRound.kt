package org.rsmod.content.minigames.tempoross

enum class TemporossPhase {
    Idle,
    Energy,
    Essence,
    Won,
    Lost,
}

sealed interface TemporossEvent {
    data class EnergyPhase(val returns: Int) : TemporossEvent

    data object WaveWarning : TemporossEvent

    data class WaveHit(val tethered: Set<Int>, val broken: List<Int>) : TemporossEvent

    data object WaveEnd : TemporossEvent

    data class Lightning(val slot: Int) : TemporossEvent

    data class TorrentStart(val cannon: Int) : TemporossEvent

    data class TorrentEnd(val cannon: Int) : TemporossEvent

    data object Won : TemporossEvent

    data object Lost : TemporossEvent
}

class TemporossRound(private val roll: (Int) -> Int) {
    var phase = TemporossPhase.Idle
        private set

    var energy = 0
        private set

    var essence = 0
        private set

    var storm = 0.0
        private set

    var returns = 0
        private set

    var maxEnergy = 0
        private set

    var maxEssence = 0
        private set

    private val points = HashMap<Int, Int>()
    private val broken = BooleanArray(SITES)
    private val fires = BooleanArray(FIRE_SLOTS)
    private val fireAge = IntArray(FIRE_SLOTS)
    private val disabled = IntArray(CANNONS)
    private val tethers = HashMap<Int, Int>()

    private var nextWave = 0
    private var waveHitAt = -1
    private var waveEndAt = -1
    private var nextLightning = 0
    private var nextTorrent = 0
    private var lastCycle = 0

    val active: Boolean
        get() = phase == TemporossPhase.Energy || phase == TemporossPhase.Essence

    fun start(players: Int, cycle: Int) {
        val scaled = (PLAYER_SCALE_SOLO - SCALE_DROP * (players - 1) / (FULL_LOBBY - 1)).coerceAtLeast(PLAYER_SCALE_FULL)
        maxEnergy = scaled * players
        maxEssence = (ESSENCE_SOLO - ESSENCE_DROP * (players - 1) / (FULL_LOBBY - 1)).coerceAtLeast(ESSENCE_FULL) * players
        energy = maxEnergy
        essence = maxEssence
        storm = 0.0
        returns = 0
        points.clear()
        broken.fill(false)
        fires.fill(false)
        disabled.fill(0)
        tethers.clear()
        phase = TemporossPhase.Energy
        lastCycle = cycle
        scheduleAttacks(cycle)
    }

    fun reset() {
        phase = TemporossPhase.Idle
        points.clear()
        tethers.clear()
    }

    fun pointsOf(player: Int): Int = points[player] ?: 0

    fun award(player: Int, amount: Int) {
        if (!active) return
        points[player] = ((points[player] ?: 0) + amount).coerceAtMost(MAX_POINTS)
    }

    fun forfeit(player: Int) {
        points.remove(player)
        tethers.remove(player)
    }

    fun pointsSnapshot(): Map<Int, Int> = HashMap(points)

    fun deposit(player: Int, cooked: Boolean): Boolean {
        if (phase != TemporossPhase.Energy) return false
        energy = (energy - if (cooked) COOKED_DRAIN else RAW_DRAIN).coerceAtLeast(0)
        award(player, if (cooked) COOKED_DEPOSIT_POINTS else RAW_DEPOSIT_POINTS)
        if (energy == 0) enterEssence()
        return true
    }

    fun strike(player: Int, damage: Int): Boolean {
        if (phase != TemporossPhase.Essence) return false
        essence = (essence - damage).coerceAtLeast(0)
        award(player, SPIRIT_POINTS)
        if (essence == 0) phase = TemporossPhase.Won
        return true
    }

    fun siteBroken(site: Int): Boolean = broken[site]

    fun repair(player: Int, site: Int): Boolean {
        if (!active || !broken[site]) return false
        broken[site] = false
        award(player, REPAIR_POINTS)
        return true
    }

    fun tether(player: Int, site: Int): Boolean {
        if (!active || broken[site]) return false
        tethers[player] = site
        return true
    }

    fun tetherOf(player: Int): Int? = tethers[player]

    fun fireAt(slot: Int): Boolean = fires[slot]

    fun douse(player: Int, slot: Int): Boolean {
        if (!active || !fires[slot]) return false
        fires[slot] = false
        award(player, DOUSE_POINTS)
        return true
    }

    fun cannonDisabled(cannon: Int): Boolean = disabled[cannon] > 0

    fun tick(cycle: Int): List<TemporossEvent> {
        if (!active || cycle == lastCycle) return emptyList()
        val elapsed = cycle - lastCycle
        lastCycle = cycle
        val events = ArrayList<TemporossEvent>()
        repeat(elapsed) { step(cycle, events) }
        return events
    }

    private fun step(cycle: Int, events: MutableList<TemporossEvent>) {
        if (phase == TemporossPhase.Essence) {
            energy = (energy + (maxEnergy / ENERGY_REGEN_DIVISOR).coerceAtLeast(1)).coerceAtMost(maxEnergy)
            if (energy >= maxEnergy) {
                returns++
                if (returns > MAX_RETURNS) {
                    phase = TemporossPhase.Lost
                    events += TemporossEvent.Lost
                    return
                }
                phase = TemporossPhase.Energy
                storm = 0.0
                scheduleAttacks(cycle)
                events += TemporossEvent.EnergyPhase(returns)
            }
            return
        }
        storm += STORM_BASE + STORM_PER_FIRE * fires.count { it }
        if (storm >= STORM_MAX) {
            phase = TemporossPhase.Lost
            events += TemporossEvent.Lost
            return
        }
        spreadFires(cycle, events)
        for (cannon in 0 until CANNONS) {
            if (disabled[cannon] > 0 && --disabled[cannon] == 0) events += TemporossEvent.TorrentEnd(cannon)
        }
        if (waveEndAt in 0..cycle) {
            waveEndAt = -1
            tethers.clear()
            events += TemporossEvent.WaveEnd
        }
        if (waveHitAt in 0..cycle) {
            waveHitAt = -1
            waveEndAt = cycle + WAVE_DURATION
            events += hitWave()
        }
        if (energy * 100 <= maxEnergy * ATTACK_FLOOR_PERCENT) return
        if (cycle >= nextWave && waveHitAt < 0 && waveEndAt < 0) {
            waveHitAt = cycle + WAVE_WARNING
            nextWave = cycle + WAVE_MIN + roll(WAVE_SPREAD)
            events += TemporossEvent.WaveWarning
        }
        if (cycle >= nextLightning) {
            nextLightning = cycle + LIGHTNING_MIN + roll(LIGHTNING_SPREAD)
            ignite(cycle, events)
        }
        if (cycle >= nextTorrent) {
            nextTorrent = cycle + TORRENT_MIN + roll(TORRENT_SPREAD)
            val cannon = roll(CANNONS)
            if (disabled[cannon] == 0) {
                disabled[cannon] = TORRENT_DURATION
                events += TemporossEvent.TorrentStart(cannon)
            }
        }
    }

    private fun hitWave(): TemporossEvent.WaveHit {
        val survivors = tethers.keys.toSet()
        val chance = BREAK_MIN_PERCENT + ((BREAK_MAX_PERCENT - BREAK_MIN_PERCENT) * storm / STORM_MAX).toInt()
        val newlyBroken = ArrayList<Int>()
        for (site in 0 until SITES) {
            if (!broken[site] && roll(100) < chance) {
                broken[site] = true
                newlyBroken += site
            }
        }
        fires.fill(false)
        for (player in survivors) award(player, WAVE_POINTS)
        return TemporossEvent.WaveHit(survivors, newlyBroken)
    }

    private fun ignite(cycle: Int, events: MutableList<TemporossEvent>) {
        val free = (0 until FIRE_SLOTS).filter { !fires[it] }
        if (free.isEmpty()) return
        val slot = free[roll(free.size)]
        fires[slot] = true
        fireAge[slot] = cycle
        events += TemporossEvent.Lightning(slot)
    }

    private fun spreadFires(cycle: Int, events: MutableList<TemporossEvent>) {
        for (slot in 0 until FIRE_SLOTS) {
            if (fires[slot] && cycle - fireAge[slot] >= FIRE_SPREAD_TICKS) {
                fireAge[slot] = cycle
                ignite(cycle, events)
            }
        }
    }

    private fun enterEssence() {
        phase = TemporossPhase.Essence
        storm = 0.0
        fires.fill(false)
        tethers.clear()
        waveHitAt = -1
        waveEndAt = -1
    }

    private fun scheduleAttacks(cycle: Int) {
        nextWave = cycle + WAVE_FIRST
        nextLightning = cycle + LIGHTNING_MIN
        nextTorrent = cycle + TORRENT_MIN
        waveHitAt = -1
        waveEndAt = -1
    }

    companion object {
        const val SITES = 4
        const val FIRE_SLOTS = 4
        const val CANNONS = 4
        const val MAX_POINTS = 32_000
        const val MAX_RETURNS = 3
        const val RAW_DRAIN = 10
        const val COOKED_DRAIN = 15
        const val PLAYER_SCALE_SOLO = 260
        const val PLAYER_SCALE_FULL = 180
        const val SCALE_DROP = PLAYER_SCALE_SOLO - PLAYER_SCALE_FULL
        const val ESSENCE_SOLO = 250
        const val ESSENCE_FULL = 170
        const val ESSENCE_DROP = ESSENCE_SOLO - ESSENCE_FULL
        const val FULL_LOBBY = 40
        const val ENERGY_REGEN_DIVISOR = 200
        const val STORM_MAX = 350.0
        const val STORM_BASE = 0.55
        const val STORM_PER_FIRE = 0.15
        const val ATTACK_FLOOR_PERCENT = 10
        const val WAVE_FIRST = 30
        const val WAVE_MIN = 40
        const val WAVE_SPREAD = 20
        const val WAVE_WARNING = 4
        const val WAVE_DURATION = 6
        const val LIGHTNING_MIN = 16
        const val LIGHTNING_SPREAD = 14
        const val TORRENT_MIN = 45
        const val TORRENT_SPREAD = 30
        const val TORRENT_DURATION = 14
        const val FIRE_SPREAD_TICKS = 20
        const val BREAK_MIN_PERCENT = 15
        const val BREAK_MAX_PERCENT = 25
        const val RAW_DEPOSIT_POINTS = 20
        const val COOKED_DEPOSIT_POINTS = 65
        const val FISH_POINTS = 5
        const val COOK_POINTS = 10
        const val REPAIR_POINTS = 40
        const val DOUSE_POINTS = 40
        const val WAVE_POINTS = 10
        const val SPIRIT_POINTS = 55
    }
}
