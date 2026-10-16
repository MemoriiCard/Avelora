package org.rsmod.content.minigames.castlewars

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CastleWarsShop
@Inject
constructor(private val shops: Shops, private val shopOps: ShopOperationMap) : PluginScript() {
    override fun ScriptContext.startup() {
        shopOps.costOf(CURRENCY, ::ticketCost)
        onOpNpc1(JUDGE) { open(player) }
        onOpNpc3(JUDGE) { open(player) }
    }

    private fun open(player: Player) {
        shops.open(
            player = player,
            title = "Castle Wars Ticket Exchange",
            shopInv = SHOP_INV,
            buyPercentage = 100.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = CURRENCY,
        )
    }

    companion object {
        const val JUDGE = "npc.castlewars_judge"
        const val SHOP_INV = "inv.castlewars_ticket_shop"
        const val CURRENCY = "currency.castlewars_ticket"

        val PRICES =
            mapOf(
                "obj.castlewars_sword" to 20,
                "obj.castlewars_med_helm" to 25,
                "obj.castlewars_armour_legs" to 35,
                "obj.castlewars_armour_body" to 45,
                "obj.castlewars_shield" to 30,
                "obj.castlewars_hood_saradomin_prize" to 60,
                "obj.castlewars_cloak_saradomin_prize" to 80,
                "obj.castlewars_hood_zamorak_prize" to 60,
                "obj.castlewars_cloak_zamorak_prize" to 80,
            )

        private fun ticketCost(type: ItemServerType): Int {
            val name = RSCM.getReverseMapping(RSCMType.OBJ, type.id)
            return PRICES[name] ?: type.cost.coerceAtLeast(1)
        }
    }
}
