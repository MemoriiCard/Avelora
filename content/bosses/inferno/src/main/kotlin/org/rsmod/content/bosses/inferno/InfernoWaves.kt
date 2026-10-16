package org.rsmod.content.bosses.inferno

internal enum class InfernoMonster(val npc: String, val size: Int) {
    Nibbler("npc.inferno_nibbler", 1),
    Bat("npc.inferno_creature_harpie", 2),
    Blob("npc.inferno_creature_splitter", 3),
    Meleer("npc.inferno_creature_melee", 4),
    Ranger("npc.inferno_creature_ranger", 3),
    Mager("npc.inferno_creature_mager", 4),
    Jad("npc.inferno_jad", 5),
}

internal object InfernoWaves {
    const val FINAL_WAVE = 68
    const val TOKKUL_NUMERATOR = 7

    private val ORDER =
        listOf(
            InfernoMonster.Nibbler,
            InfernoMonster.Bat,
            InfernoMonster.Blob,
            InfernoMonster.Meleer,
            InfernoMonster.Ranger,
            InfernoMonster.Mager,
            InfernoMonster.Jad,
        )

    private val TABLE =
        listOf(
            "3 1 0 0 0 0 0",
            "3 2 0 0 0 0 0",
            "6 0 0 0 0 0 0",
            "3 0 1 0 0 0 0",
            "3 1 1 0 0 0 0",
            "3 2 1 0 0 0 0",
            "3 0 2 0 0 0 0",
            "6 0 0 0 0 0 0",
            "3 0 0 1 0 0 0",
            "3 1 0 1 0 0 0",
            "3 2 0 1 0 0 0",
            "3 0 1 1 0 0 0",
            "3 1 1 1 0 0 0",
            "3 2 1 1 0 0 0",
            "3 0 2 1 0 0 0",
            "3 0 0 2 0 0 0",
            "6 0 0 0 0 0 0",
            "3 0 0 0 1 0 0",
            "3 1 0 0 1 0 0",
            "3 2 0 0 1 0 0",
            "3 0 1 0 1 0 0",
            "3 1 1 0 1 0 0",
            "3 2 1 0 1 0 0",
            "3 0 2 0 1 0 0",
            "3 0 0 1 1 0 0",
            "3 1 0 1 1 0 0",
            "3 2 0 1 1 0 0",
            "3 0 1 1 1 0 0",
            "3 1 1 1 1 0 0",
            "3 2 1 1 1 0 0",
            "3 0 2 1 1 0 0",
            "3 0 0 2 1 0 0",
            "3 0 0 0 2 0 0",
            "6 0 0 0 0 0 0",
            "3 0 0 0 0 1 0",
            "3 1 0 0 0 1 0",
            "3 2 0 0 0 1 0",
            "3 0 1 0 0 1 0",
            "3 1 1 0 0 1 0",
            "3 2 1 0 0 1 0",
            "3 0 2 0 0 1 0",
            "3 0 0 1 0 1 0",
            "3 1 0 1 0 1 0",
            "3 2 0 1 0 1 0",
            "3 0 1 1 0 1 0",
            "3 1 1 1 0 1 0",
            "3 2 1 1 0 1 0",
            "3 0 2 1 0 1 0",
            "3 0 0 2 0 1 0",
            "3 0 0 0 1 1 0",
            "3 1 0 0 1 1 0",
            "3 2 0 0 1 1 0",
            "3 0 1 0 1 1 0",
            "3 1 1 0 1 1 0",
            "3 2 1 0 1 1 0",
            "3 0 2 0 1 1 0",
            "3 0 0 1 1 1 0",
            "3 1 0 1 1 1 0",
            "3 2 0 1 1 1 0",
            "3 0 1 1 1 1 0",
            "3 1 1 1 1 1 0",
            "3 2 1 1 1 1 0",
            "3 0 2 1 1 1 0",
            "3 0 0 2 1 1 0",
            "3 0 0 0 2 1 0",
            "3 0 0 0 0 2 0",
            "0 0 0 0 0 0 1",
            "0 0 0 0 0 0 3",
        )

    private val waves: List<List<InfernoMonster>> =
        TABLE.map { row ->
            val counts = row.split(" ").map { it.toInt() }
            ORDER.indices.reversed().flatMap { i -> List(counts[i]) { ORDER[i] } }
        }

    fun wave(number: Int): List<InfernoMonster> = waves[number - 1]

    fun tokkulFor(wavesCompleted: Int): Int = wavesCompleted * (wavesCompleted + 1) * TOKKUL_NUMERATOR / 2
}
