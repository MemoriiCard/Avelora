package org.rsmod.content.minigames.gotr

enum class GotrPhase {
    Idle,
    Active,
    Won,
    Lost,
}

sealed interface GotrEvent {
    data class Rotated(val elemental: GotrRune, val catalytic: GotrRune) : GotrEvent

    data class GuardiansExpired(val count: Int) : GotrEvent

    data object Won : GotrEvent

    data object Lost : GotrEvent
}

class GotrReward(val elementalPoints: Int, val catalyticPoints: Int, val xpBonus: Boolean)

class GotrRound(private val roll: (Int) -> Int) {
    var phase = GotrPhase.Idle
        private set

    var power = 0.0
        private set

    var maxPower = 0
        private set

    var elementalRune = GotrRune.Air
        private set

    var catalyticRune = GotrRune.Mind
        private set

    var nextRotation = 0
        private set

    private val elemental = HashMap<Int, Int>()
    private val catalytic = HashMap<Int, Int>()
    private val guardians = ArrayList<Int>()
    private val tileReady = HashMap<Int, Int>()
    private var lastCycle = 0

    val guardianCount: Int
        get() = guardians.size

    val active: Boolean
        get() = phase == GotrPhase.Active

    fun start(players: Int, cycle: Int) {
        maxPower = POWER_PER_PLAYER * players
        power = maxPower * START_FRACTION
        elemental.clear()
        catalytic.clear()
        guardians.clear()
        tileReady.clear()
        lastCycle = cycle
        rotate(cycle)
        phase = GotrPhase.Active
    }

    fun reset() {
        phase = GotrPhase.Idle
        elemental.clear()
        catalytic.clear()
        guardians.clear()
        tileReady.clear()
    }

    fun energyOf(player: Int, kind: GotrKind): Int =
        (if (kind == GotrKind.Elemental) elemental else catalytic)[player] ?: 0

    fun totalEnergy(player: Int): Int = energyOf(player, GotrKind.Elemental) + energyOf(player, GotrKind.Catalytic)

    fun forfeit(player: Int) {
        elemental.remove(player)
        catalytic.remove(player)
    }

    fun gain(player: Int, kind: GotrKind, amount: Int): Int {
        if (!active || amount <= 0) return 0
        val book = if (kind == GotrKind.Elemental) elemental else catalytic
        val room = (TYPE_CAP - (book[player] ?: 0)).coerceAtMost(TOTAL_CAP - totalEnergy(player)).coerceAtLeast(0)
        val added = amount.coerceAtMost(room)
        book[player] = (book[player] ?: 0) + added
        return added
    }

    fun rune(kind: GotrKind): GotrRune = if (kind == GotrKind.Elemental) elementalRune else catalyticRune

    fun assemble(player: Int, kind: GotrKind, cycle: Int): Int {
        if (!active || guardians.size >= GUARDIAN_LIMIT) return -1
        guardians += cycle + GUARDIAN_LIFE
        val energy = rune(kind).tier.guardianEnergy
        gain(player, kind, energy)
        return energy
    }

    fun placeCell(player: Int, tile: Int, tier: GotrTier, cycle: Int): Boolean {
        if (!active || (tileReady[tile] ?: 0) > cycle) return false
        tileReady[tile] = cycle + TILE_COOLDOWN
        gain(player, GotrKind.Elemental, tier.cellEnergy)
        gain(player, GotrKind.Catalytic, tier.cellEnergy)
        return true
    }

    fun deposit(stones: Int, polyStones: Int = 0) {
        if (!active) return
        power = (power + stones * STONE_ENERGY + polyStones * POLY_ENERGY).coerceAtMost(maxPower.toDouble())
        if (power >= maxPower) phase = GotrPhase.Won
    }

    fun tick(cycle: Int): List<GotrEvent> {
        if (!active || cycle == lastCycle) return emptyList()
        val elapsed = cycle - lastCycle
        lastCycle = cycle
        val events = ArrayList<GotrEvent>()
        power -= maxPower * DECAY_PER_TICK * elapsed
        if (power <= 0.0) {
            power = 0.0
            phase = GotrPhase.Lost
            events += GotrEvent.Lost
            return events
        }
        val expired = guardians.count { it <= cycle }
        if (expired > 0) {
            guardians.removeAll { it <= cycle }
            events += GotrEvent.GuardiansExpired(expired)
        }
        if (cycle >= nextRotation) {
            rotate(cycle)
            events += GotrEvent.Rotated(elementalRune, catalyticRune)
        }
        return events
    }

    fun reward(player: Int): GotrReward {
        val ele = energyOf(player, GotrKind.Elemental)
        val cat = energyOf(player, GotrKind.Catalytic)
        if (ele + cat < MIN_REWARD_ENERGY) return GotrReward(0, 0, false)
        return GotrReward(points(ele), points(cat), true)
    }

    private fun points(energy: Int): Int = energy / 100 + if (roll(100) < energy % 100) 1 else 0

    private fun rotate(cycle: Int) {
        elementalRune = pick(GotrRune.elemental, elementalRune)
        catalyticRune = pick(GotrRune.catalytic, catalyticRune)
        nextRotation = cycle + ROTATION_TICKS
    }

    private fun pick(pool: List<GotrRune>, current: GotrRune): GotrRune {
        val options = pool.filter { it != current }
        return options[roll(options.size)]
    }

    companion object {
        const val POWER_PER_PLAYER = 500
        const val START_FRACTION = 0.3
        const val DECAY_PER_TICK = 0.0005
        const val STONE_ENERGY = 2
        const val POLY_ENERGY = 3
        const val TYPE_CAP = 1000
        const val TOTAL_CAP = 1200
        const val MIN_REWARD_ENERGY = 300
        const val GUARDIAN_LIMIT = 10
        const val GUARDIAN_LIFE = 150
        const val TILE_COOLDOWN = 60
        const val ROTATION_TICKS = 230
    }
}
