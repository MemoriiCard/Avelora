package org.rsmod.content.raids.toa.reward

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.cox.reward.CoxRewardDelivery
import org.rsmod.content.raids.toa.raid.ToaRaids
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRewardScript
@Inject
constructor(
    private val raids: ToaRaids,
    private val delivery: CoxRewardDelivery,
    private val pets: PetRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ToaRewards.CHEST) { openChest(it.loc.coords) }
        onOpLoc1(ToaRewards.SARCOPHAGUS_RARE) { openChest(it.loc.coords) }
        onOpLoc1("loc.toa_lobby_cape_chest") { claimShroud() }
        onOpLoc1("loc.toa_rewards_chest_lobby_closed") { mes("Unclaimed rewards are sent to your bank when you leave.") }
        onOpLoc1("loc.toa_rewards_chest_lobby_open") { mes("Unclaimed rewards are sent to your bank when you leave.") }
        onOpLoc1("loc.toa_lobby_gravestone_chest") { mes("Your items are always kept safe inside the tombs.") }
    }

    private fun ProtectedAccess.openChest(at: CoordGrid) {
        val raid = raids.containing(player)
        if (raid == null) {
            mes("The chest is sealed.")
            return
        }
        if (raid.chests[at] !== player) {
            mes("This isn't your chest.")
            return
        }
        val loot = raid.rewards.remove(player)
        if (loot == null) {
            mes("There is nothing left in the chest for you.")
            return
        }
        for (item in loot) give(item)
    }

    private fun ProtectedAccess.give(item: CoxItem) {
        if (item.obj in ToaLoot.PETS) pets.give(player, item.obj) else delivery.give(player, item)
        val amount = if (item.count > 1) "${item.count} x " else ""
        mes("You find: $amount${delivery.displayName(item)}.")
    }

    private fun ProtectedAccess.claimShroud() {
        val total = player.toaKc + player.toaExpertKc
        val shroud = ToaLoot.shroudFor(total)
        if (shroud == null) {
            mes("You have completed $total Tombs of Amascut. Return after 100 to earn a shroud.")
            return
        }
        if (ToaLoot.SHROUDS.any { (_, obj) -> delivery.owns(player, obj) || player.worn.count(obj) > 0 }) {
            mes("You already hold a shroud. Return when you have earned the next tier.")
            return
        }
        delivery.give(player, CoxItem(shroud, 1))
        if (ToaLoot.hoodUnlocked(total) && !delivery.owns(player, HOOD)) delivery.give(player, CoxItem(HOOD, 1))
        mes("You take an Icthlarin's shroud from the chest.")
    }

    private companion object {
        const val HOOD = "obj.icthlarins_hood"
    }
}
