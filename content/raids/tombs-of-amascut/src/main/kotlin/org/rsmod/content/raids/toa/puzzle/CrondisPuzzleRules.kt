package org.rsmod.content.raids.toa.puzzle

object CrondisPuzzleRules {
    const val TREES = 4
    const val CROC_HP = 30
    const val BITE_MAX = 14
    const val BITE_RATE = 4

    fun treeName(index: Int): String = "npc.toa_crondis_tree_${index + 1}"

    fun complete(watered: Set<Int>): Boolean = watered.size >= TREES
}
