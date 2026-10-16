package org.rsmod.content.quest.area.alkharid

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.area.alkharid.PrinceAliRescue.Companion.KEY
import org.rsmod.content.quest.area.alkharid.PrinceAliRescue.Companion.KEY_MADE
import org.rsmod.content.quest.area.alkharid.PrinceAliRescue.Companion.OSMAN
import org.rsmod.content.quest.area.alkharid.PrinceAliRescue.Companion.QUEST
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestProgressState

object PrinceAliLeela {

    suspend fun Dialogue.talk(): Boolean {
        val quest = Quest.get(QUEST) ?: return false
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) return false
        when (quest.getQuestStage(player)) {
            OSMAN -> plan()
            KEY_MADE -> keyAndDisguise()
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.plan() {
        chatPlayer(quiz, "I'm helping Osman free the Prince. What is the plan?")
        chatNpc(neutral, "Get a copy of Keli's key made, disguise the Prince as her, then break him out.")
        chatNpc(neutral, "Keli wears the key on a chain. Pose as a recruit to get close, then press soft clay against it.")
        chatNpc(neutral, "For the disguise: Ned in Draynor makes wigs from three balls of wool, which you then dye yellow. Aggie makes the skin paste. A pink skirt is sold in Varrock.")
    }

    private suspend fun Dialogue.keyAndDisguise() {
        val inv = access.inv
        if (inv.count(KEY) == 0) {
            chatNpc(happy, "Osman has sent the key. Here it is.")
            access.invAdd(inv, KEY, 1)
            chatNpc(neutral, "Now get Joe drunk, tie up Keli with a rope and free the Prince with the disguise.")
            return
        }
        chatNpc(quiz, "How are you getting on?")
        chatPlayer(neutral, "I'm still working on it.")
        chatNpc(neutral, "Deal with the guard Joe without violence, tie up Keli with rope, then give Ali the disguise and the key.")
    }
}
