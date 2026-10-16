package org.rsmod.content.raids.cox.rimor

object CoxCapes {
    val TIERS: List<Pair<Int, String>> =
        listOf(
            100 to "obj.cox_challenge_cape_t1",
            500 to "obj.cox_challenge_cape_t2",
            1_000 to "obj.cox_challenge_cape_t3",
            1_500 to "obj.cox_challenge_cape_t4",
            2_000 to "obj.cox_challenge_cape_t5",
        )

    /** The best cape [challengeCompletions] earns, if any. */
    fun earned(challengeCompletions: Int): String? =
        TIERS.lastOrNull { challengeCompletions >= it.first }?.second

    fun next(challengeCompletions: Int): Pair<Int, String>? =
        TIERS.firstOrNull { challengeCompletions < it.first }
}
