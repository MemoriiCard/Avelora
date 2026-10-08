package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Player

/** Raid points for the food, potions and storage units the party prepares, with the wiki's caps. */
@Singleton
class CoxSupplyPoints @Inject constructor(private val raids: CoxRaids) {
    fun food(player: Player, tier: Int) {
        val raid = raids.containing(player) ?: return
        val party = raid.scaling.partySize
        if (raid.foodUnits >= FOOD_HARD_CAP * party) return
        val reduced = raid.foodUnits >= FOOD_SOFT_CAP * party
        raid.foodUnits++
        val points = FOOD_BASE + FOOD_PER_TIER * tier
        raids.addPoints(raid, player, if (reduced) points / 2 else points)
    }

    fun potion(player: Player, doses: Int, perDose: Int) {
        val raid = raids.containing(player) ?: return
        val party = raid.scaling.partySize
        val cap = POTION_CAP * party + POTION_EXTRA
        repeat(doses) {
            if (raid.potionUnits >= cap) return
            val reduced = raid.potionUnits >= POTION_CAP * party
            raid.potionUnits++
            raids.addPoints(raid, player, if (reduced) perDose / 2 else perDose)
        }
    }

    fun storage(player: Player, tiers: Int) {
        val raid = raids.containing(player) ?: return
        raids.addPoints(raid, player, tiers * STORAGE_PER_TIER)
    }

    private companion object {
        const val FOOD_BASE = 4
        const val FOOD_PER_TIER = 8
        const val FOOD_SOFT_CAP = 20
        const val FOOD_HARD_CAP = 40
        const val POTION_CAP = 5
        const val POTION_EXTRA = 5
        const val STORAGE_PER_TIER = 100
    }
}
