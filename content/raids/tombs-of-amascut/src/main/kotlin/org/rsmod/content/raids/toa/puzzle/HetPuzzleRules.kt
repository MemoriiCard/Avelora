package org.rsmod.content.raids.toa.puzzle

object HetPuzzleRules {
    const val GOAL_HITS = 3
    const val ORB_EVERY = 6
    const val ORB_MAX = 14

    fun destroyed(hits: Int): Boolean = hits >= GOAL_HITS
}
