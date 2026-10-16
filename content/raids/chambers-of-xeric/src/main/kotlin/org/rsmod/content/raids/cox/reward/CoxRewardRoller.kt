package org.rsmod.content.raids.cox.reward

class CoxRewardSet<P>(val items: Map<P, List<CoxItem>>, val uniques: List<Pair<P, CoxItem>>)

/** Decides who gets what from the ancient chest once the Great Olm is dead. */
object CoxRewardRoller {
    fun <P> roll(
        points: Map<P, Int>,
        teamPoints: Int,
        challengeMode: Boolean,
        underTarget: Boolean,
        present: (P) -> Boolean,
        hasTablet: (P) -> Boolean,
        hasJournal: (P) -> Boolean,
        below: (Int) -> Int,
    ): CoxRewardSet<P> {
        val uniqueWins = mutableMapOf<P, MutableList<CoxItem>>()
        val announced = mutableListOf<Pair<P, CoxItem>>()
        for (chance in CoxLoot.uniqueChances(teamPoints)) {
            if (below(UNIQUE_SCALE) >= chance) continue
            val player = CoxLoot.pickRecipient(points, below) ?: continue
            val unique = CoxLoot.pickUnique(challengeMode, below)
            val item = CoxItem(unique.obj, 1)
            if (!present(player)) continue
            uniqueWins.getOrPut(player) { mutableListOf() } += item
            announced += player to item
        }
        val items = mutableMapOf<P, List<CoxItem>>()
        for ((player, personal) in points) {
            if (!present(player)) continue
            val loot = mutableListOf<CoxItem>()
            val won = uniqueWins[player]
            if (won != null) {
                loot += won
                if (below(CoxLoot.OLMLET_ODDS) == 0) loot += CoxItem(CoxLoot.OLMLET, 1)
            } else {
                loot += commonLoot(personal, hasTablet(player), below)
                if (below(CoxLoot.CLUE_ODDS) == 0) loot += CoxItem(CoxLoot.CLUE, 1)
            }
            if (!hasJournal(player)) loot += CoxItem(CoxLoot.JOURNAL, 1)
            if (challengeMode && underTarget) {
                if (below(CoxLoot.KIT_ODDS) == 0) loot += CoxItem(CoxLoot.KIT, 1)
                if (below(CoxLoot.DUST_ODDS) == 0) loot += CoxItem(CoxLoot.DUST, 1)
            }
            items[player] = loot
        }
        return CoxRewardSet(items, announced)
    }

    private fun commonLoot(personal: Int, canGetTablet: Boolean, below: (Int) -> Int): List<CoxItem> {
        val first = CoxLoot.rollCommon(below = below)
        val second = CoxLoot.rollCommon(exclude = first, below = below)
        val rolls = mutableListOf(first, second)
        val loot = mutableListOf<CoxItem>()
        if (canGetTablet && below(CoxLoot.TABLET_ODDS) == 0) {
            rolls.removeAt(below(rolls.size))
            loot += CoxItem(CoxLoot.TABLET, 1)
        }
        for (common in rolls) loot += CoxLoot.toItem(common, personal, below)
        return loot
    }

    private const val UNIQUE_SCALE = 10_000
}
