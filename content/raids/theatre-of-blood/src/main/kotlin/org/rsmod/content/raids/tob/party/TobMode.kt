package org.rsmod.content.raids.tob.party

enum class TobMode(val id: Int, val label: String, val hpPercent: Int, val damagePercent: Int) {
    Entry(0, "Entry Mode", 80, 70),
    Normal(1, "Regular Mode", 100, 100),
    Hard(2, "Hard Mode", 115, 125);

    val awardsUniques: Boolean
        get() = this != Entry

    companion object {
        operator fun get(id: Int): TobMode = entries.firstOrNull { it.id == id } ?: Normal
    }
}
