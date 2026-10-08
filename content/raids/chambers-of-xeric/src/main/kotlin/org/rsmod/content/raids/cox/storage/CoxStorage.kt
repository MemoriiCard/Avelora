package org.rsmod.content.raids.cox.storage

import org.rsmod.game.entity.Player

/**
 * A raid's storage units. The tier is shared by every unit in the raid. Shared storage is one pool
 * the whole party can donate to and take from; private storage is a per-player set of slots.
 */
class CoxStorage {
    var tier = 0
        private set

    private val shared = linkedMapOf<String, Int>()
    private val personal = mutableMapOf<Player, LinkedHashMap<String, Int>>()

    val sharedCapacity: Int
        get() = SHARED_CAPACITY[tier]

    val privateCapacity: Int
        get() = PRIVATE_CAPACITY[tier]

    val sharedTotal: Int
        get() = shared.values.sum()

    fun sharedItems(): Map<String, Int> = shared

    fun privateItems(player: Player): Map<String, Int> = personal[player] ?: emptyMap()

    fun privateSlots(player: Player): Int =
        privateItems(player).entries.sumOf { (obj, count) -> slotsFor(obj, count) }

    fun upgrade(to: Int) {
        tier = to.coerceIn(0, MAX_TIER)
    }

    /** Adds up to [count] of [obj] to the shared pool; returns how many fit. */
    fun donate(obj: String, count: Int): Int {
        if (sharedCapacity == 0) return 0
        val accepted = minOf(count, sharedCapacity - sharedTotal)
        if (accepted <= 0) return 0
        shared.merge(obj, accepted, Int::plus)
        return accepted
    }

    fun takeShared(obj: String, count: Int): Int = take(shared, obj, count)

    /** Stores up to [count] of [obj] in [player]'s slots; returns how many fit. */
    fun store(player: Player, obj: String, count: Int): Int {
        val items = personal.getOrPut(player) { linkedMapOf() }
        val free = privateCapacity - privateSlots(player)
        val accepted =
            when {
                CoxItems.isStackable(obj) && obj in items -> count
                CoxItems.isStackable(obj) -> if (free > 0) count else 0
                else -> minOf(count, free)
            }
        if (accepted <= 0) return 0
        items.merge(obj, accepted, Int::plus)
        return accepted
    }

    fun takePrivate(player: Player, obj: String, count: Int): Int {
        val items = personal[player] ?: return 0
        return take(items, obj, count)
    }

    /** Removes and returns everything [player] left in private storage. */
    fun release(player: Player): Map<String, Int> = personal.remove(player) ?: emptyMap()

    private fun take(items: MutableMap<String, Int>, obj: String, count: Int): Int {
        val held = items[obj] ?: return 0
        val taken = minOf(held, count)
        if (taken == held) items.remove(obj) else items[obj] = held - taken
        return taken
    }

    private fun slotsFor(obj: String, count: Int): Int = if (CoxItems.isStackable(obj)) 1 else count

    companion object {
        const val MAX_TIER = 4
        val SHARED_CAPACITY = listOf(0, 250, 500, 1000, 1500)
        val PRIVATE_CAPACITY = listOf(25, 30, 60, 90, 120)
        val PLANKS = listOf(0, 2, 4, 6, 8)
        val CONSTRUCTION = listOf(0, 30, 60, 90, 99)
        val NAMES = listOf("tiny", "small", "medium", "large", "massive")
        val UNITS =
            listOf(
                "loc.raids_storage_hotspot",
                "loc.raids_storage_1",
                "loc.raids_storage_2",
                "loc.raids_storage_3",
                "loc.raids_storage_4",
            )
    }
}
