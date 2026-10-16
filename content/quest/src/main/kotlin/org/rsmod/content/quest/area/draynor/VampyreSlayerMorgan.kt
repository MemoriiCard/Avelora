package org.rsmod.content.quest.area.draynor

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.quest.area.draynor.VampyreSlayer.Companion.COMPLETE_STAGE
import org.rsmod.content.quest.area.draynor.VampyreSlayer.Companion.GARLIC
import org.rsmod.content.quest.area.draynor.VampyreSlayer.Companion.QUEST
import org.rsmod.content.quest.area.draynor.VampyreSlayer.Companion.STAKE_GIVEN
import org.rsmod.content.quest.area.draynor.VampyreSlayer.Companion.STARTED
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.game.entity.Player

object VampyreSlayerMorgan {
    var Player.countSlain by intVarBit("varbit.vampyre_slayer_count_slain")

    suspend fun Dialogue.talk(): Boolean {
        val quest = Quest.get(QUEST) ?: return false
        return when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> {
                start()
                true
            }
            QuestProgressState.FINISHED -> false
            QuestProgressState.IN_PROGRESS -> {
                if (player.countSlain == 1) {
                    chatPlayer(happy, "The Count is dead!")
                    chatNpc(happy, "You've saved us all! The village is in your debt.")
                    quest.advanceQuestStage(access, COMPLETE_STAGE - quest.getQuestStage(player))
                } else {
                    chatNpc(worried, "Please hurry! I'm sure Dr Harlow in Varrock's Blue Moon Inn can help.")
                    if (quest.getQuestStage(player) >= STAKE_GIVEN && access.inv.count(GARLIC) == 0) {
                        chatNpc(neutral, "Take some garlic. I keep it in the cupboard upstairs, just in case the Count comes and tries anything.")
                    }
                }
                true
            }
        }
    }

    private suspend fun Dialogue.start() {
        val quest = Quest.get(QUEST) ?: return
        chatNpc(worried, "Could it be? A bold adventurer! Please, you must help us!")
        if (!choice2("What's the problem?", true, "Sorry, I can't help.", false)) {
            chatPlayer(neutral, "Sorry, I can't help.")
            return
        }
        chatPlayer(quiz, "What's the problem?")
        chatNpc(worried, "Count Draynor has terrorised this village from his manor to the north for years. Will you stop him?")
        chatNpc(neutral, "I'm sure Dr Harlow in the Blue Moon Inn in Varrock can help.")
        quest.advanceQuestStage(access, STARTED)
    }
}
