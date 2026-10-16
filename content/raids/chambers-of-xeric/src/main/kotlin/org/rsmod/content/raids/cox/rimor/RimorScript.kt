package org.rsmod.content.raids.cox.rimor

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.raids.cox.party.coxCmKc
import org.rsmod.content.raids.cox.party.coxLevelScalingOff
import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.cox.reward.CoxRewardDelivery
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RimorScript @Inject constructor(private val delivery: CoxRewardDelivery) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RIMOR) { startDialogue(it.npc) { menu(it.npc) } }
        onOpNpc3(RIMOR) { startDialogue(it.npc) { claimCape() } }
        onOpNpc4(RIMOR) { startDialogue(it.npc) { toggleScaling() } }
    }

    private suspend fun Dialogue.menu(npc: Npc) {
        chatNpc(happy, "Welcome to the Chambers of Xeric, adventurer. What can I do for you?")
        when (
            choice3(
                "Claim a Challenge Mode cape",
                1,
                "Change the level scaling",
                2,
                "How does level scaling work?",
                3,
            )
        ) {
            1 -> claimCape()
            2 -> toggleScaling()
            3 -> explainScaling()
        }
    }

    private suspend fun Dialogue.claimCape() {
        val completions = player.coxCmKc
        val cape = CoxCapes.earned(completions)
        if (cape == null) {
            val next = CoxCapes.next(completions)!!
            chatNpc(
                neutral,
                "You have completed $completions Challenge Mode raids. Finish ${next.first} " +
                    "and I'll have your first cape ready.",
            )
            return
        }
        if (CoxCapes.TIERS.any { (_, obj) -> delivery.owns(player, obj) || player.worn.count(obj) > 0 }) {
            chatNpc(neutral, "You already have a cape from me. Come back when you've earned the next one.")
            return
        }
        chatNpc(happy, "Well fought! Take this as proof of your Challenge Mode mastery.")
        delivery.give(player, CoxItem(cape, 1))
    }

    private suspend fun Dialogue.toggleScaling() {
        val off = player.coxLevelScalingOff
        val prompt =
            if (off) "Level scaling is off. Turn it back on?" else "Level scaling is on. Turn it off?"
        chatNpc(neutral, prompt)
        when (choice2("Yes", 1, "No", 2)) {
            1 -> {
                player.coxLevelScalingOff = !off
                chatNpc(
                    happy,
                    if (off) "Done. The Chambers will scale to your party's levels again."
                    else "Done. The Chambers will treat you as a maxed adventurer.",
                )
            }
        }
    }

    private suspend fun Dialogue.explainScaling() {
        chatNpc(
            neutral,
            "If nobody in your party has a combat level of 115 or more, the Chambers weaken " +
                "their creatures to match. Turn scaling off and they'll always fight at full strength.",
        )
    }

    private companion object {
        const val RIMOR = "npc.raids_temple_captain"
    }
}
