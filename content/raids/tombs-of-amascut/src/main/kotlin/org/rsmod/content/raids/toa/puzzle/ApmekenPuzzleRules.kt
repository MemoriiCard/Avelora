package org.rsmod.content.raids.toa.puzzle

object ApmekenPuzzleRules {
    const val PILLARS = 4
    const val VENT_EVERY = 8
    const val VENT_MAX = 12
    const val VENT_REACH = 1

    fun complete(repaired: Set<Int>): Boolean = repaired.size >= PILLARS
}
