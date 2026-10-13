package org.rsmod.content.minigames.wintertodt

enum class BrazierState(val client: Int) {
    Unlit(0),
    Lit(1),
    Broken(2),
}

enum class AttackKind {
    Cold,
    Snow,
    Extinguish,
    Shatter,
}

sealed interface WintertodtEvent {
    data object Started : WintertodtEvent

    data class Attack(val kind: AttackKind, val brazier: Int?) : WintertodtEvent

    data class Ended(val points: Map<Int, Int>) : WintertodtEvent
}

class WintertodtRound(private val nextInt: (Int) -> Int) {
    var active = false
        private set

    var energy = 0.0
        private set

    private val braziers = Array(BRAZIERS) { BrazierState.Unlit }
    private val points = LinkedHashMap<Int, Int>()
    private var restUntil = 0
    private var nextAttack = 0

    val members: Set<Int>
        get() = points.keys

    fun brazier(index: Int): BrazierState = braziers[index]

    fun pointsOf(id: Int): Int = points[id] ?: 0

    fun join(id: Int) {
        points.putIfAbsent(id, 0)
    }

    fun leave(id: Int) {
        points.remove(id)
    }

    fun light(index: Int, id: Int): Boolean {
        if (!active || id !in points || braziers[index] != BrazierState.Unlit) return false
        braziers[index] = BrazierState.Lit
        award(id, LIGHT_POINTS)
        return true
    }

    fun repair(index: Int, id: Int): Boolean {
        if (!active || id !in points || braziers[index] != BrazierState.Broken) return false
        braziers[index] = BrazierState.Unlit
        award(id, REPAIR_POINTS)
        return true
    }

    fun feed(index: Int, id: Int, value: Int): Boolean {
        if (!active || id !in points || braziers[index] != BrazierState.Lit) return false
        award(id, value)
        energy = (energy - value * FEED_DAMAGE).coerceAtLeast(0.0)
        return true
    }

    fun tick(cycle: Int): List<WintertodtEvent> {
        if (!active) {
            if (points.isNotEmpty() && cycle >= restUntil) return start(cycle)
            return emptyList()
        }
        val lit = braziers.count { it == BrazierState.Lit }
        energy = if (lit == 0) (energy + REGEN).coerceAtMost(ENERGY_MAX) else energy - lit * DRAIN
        if (energy <= 0.0) return end(cycle)
        if (cycle >= nextAttack) {
            nextAttack = cycle + ATTACK_BASE + (energy / ENERGY_MAX * ATTACK_SPREAD).toInt()
            return listOf(attack())
        }
        return emptyList()
    }

    private fun award(id: Int, amount: Int) {
        points[id] = pointsOf(id) + amount
    }

    private fun start(cycle: Int): List<WintertodtEvent> {
        active = true
        energy = ENERGY_MAX
        braziers.fill(BrazierState.Unlit)
        points.keys.forEach { points[it] = 0 }
        nextAttack = cycle + ATTACK_BASE
        return listOf(WintertodtEvent.Started)
    }

    private fun end(cycle: Int): List<WintertodtEvent> {
        active = false
        energy = 0.0
        restUntil = cycle + RESPAWN_TICKS
        braziers.fill(BrazierState.Unlit)
        val result = points.toMap()
        points.keys.forEach { points[it] = 0 }
        return listOf(WintertodtEvent.Ended(result))
    }

    private fun attack(): WintertodtEvent {
        val roll = nextInt(100)
        val lit = braziers.indices.filter { braziers[it] == BrazierState.Lit }
        return when {
            roll < COLD_PERCENT -> WintertodtEvent.Attack(AttackKind.Cold, null)
            roll < COLD_PERCENT + SNOW_PERCENT -> WintertodtEvent.Attack(AttackKind.Snow, null)
            lit.isEmpty() -> WintertodtEvent.Attack(AttackKind.Cold, null)
            else -> {
                val target = lit[nextInt(lit.size)]
                if (nextInt(2) == 0) {
                    braziers[target] = BrazierState.Unlit
                    WintertodtEvent.Attack(AttackKind.Extinguish, target)
                } else {
                    braziers[target] = BrazierState.Broken
                    WintertodtEvent.Attack(AttackKind.Shatter, target)
                }
            }
        }
    }

    companion object {
        const val BRAZIERS = 4
        const val ENERGY_MAX = 3500.0
        const val MIN_POINTS = 500
        const val LIGHT_POINTS = 25
        const val REPAIR_POINTS = 25
        const val ROOT_POINTS = 10
        const val KINDLING_POINTS = 25
        const val DRAIN = 1.0
        const val REGEN = 1.0
        const val FEED_DAMAGE = 1.0
        const val RESPAWN_TICKS = 100
        const val ATTACK_BASE = 8
        const val ATTACK_SPREAD = 18
        const val COLD_PERCENT = 55
        const val SNOW_PERCENT = 25
    }
}
