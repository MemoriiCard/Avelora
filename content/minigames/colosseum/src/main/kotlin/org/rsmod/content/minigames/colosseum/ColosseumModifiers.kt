package org.rsmod.content.minigames.colosseum

internal enum class ColosseumModifier(val title: String, val label: String, val glory: Int) {
    Quartet("Quartet", "Quartet: an extra Fremennik warbander every wave.", 10),
    DynamicDuo("Dynamic Duo", "Dynamic Duo: Shockwave Colossi spawn in pairs.", 15),
    RedFlag("Red Flag", "Red Flag: minotaurs route-find around obstacles.", 10),
    ;

    companion object {
        fun offers(taken: Set<ColosseumModifier>, pick: (Int) -> Int): List<ColosseumModifier> {
            val left = entries.filter { it !in taken }.toMutableList()
            val offers = ArrayList<ColosseumModifier>()
            while (left.isNotEmpty() && offers.size < OFFER_COUNT) offers += left.removeAt(pick(left.size))
            return offers
        }

        const val OFFER_COUNT = 3
    }
}
