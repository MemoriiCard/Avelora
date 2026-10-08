package org.rsmod.content.raids.tob.reward

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.cox.reward.CoxRewardDelivery
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TobRewardScript
@Inject
constructor(
    private val raids: TobRaids,
    private val delivery: CoxRewardDelivery,
    private val pets: PetRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(TobRewards.CHEST) { openChest(it.loc.coords) }
        onOpLoc1(TobRewards.CHEST_RARE) { openChest(it.loc.coords) }
        for (stranger in STRANGERS) {
            onOpNpc1(stranger) { startDialogue(it.npc) { talk() } }
            onOpNpc2(stranger) { startDialogue(it.npc) { claimShroud() } }
        }
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
        if (item.obj == TobLoot.PET) pets.give(player, item.obj) else delivery.give(player, item)
        val amount = if (item.count > 1) "${item.count} x " else ""
        mes("You find: $amount${delivery.displayName(item)}.")
    }

    private suspend fun Dialogue.talk() {
        chatNpc(
            neutral,
            "You have braved the Theatre ${player.tobKc} times. " +
                "Those who endure are rewarded with a shroud of the Sinhaza.",
        )
        when (choice2("Ask for a shroud", 1, "How do shrouds work?", 2)) {
            1 -> claimShroud()
            2 -> explain()
        }
    }

    private suspend fun Dialogue.explain() {
        chatNpc(
            neutral,
            "Complete the Theatre 100, 500, 1000, 1500 and 2000 times and I will give you a shroud " +
                "of the matching tier. Entry Mode does not count.",
        )
    }

    private suspend fun Dialogue.claimShroud() {
        val kc = player.tobKc
        val shroud = TobLoot.shroudFor(kc)
        if (shroud == null) {
            chatNpc(neutral, "You have only completed $kc Theatres. Return after you have finished 100.")
            return
        }
        if (TobLoot.SHROUDS.any { (_, obj) -> delivery.owns(player, obj) || player.worn.count(obj) > 0 }) {
            chatNpc(neutral, "You already hold a shroud. Come back when you have earned the next tier.")
            return
        }
        chatNpc(happy, "Wear this with pride. The Sinhaza remember their own.")
        delivery.give(player, CoxItem(shroud, 1))
    }

    private companion object {
        val STRANGERS = listOf("npc.tob_stranger", "npc.tob_stranger_1op", "npc.tob_stranger_2op")
    }
}
