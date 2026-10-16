package org.rsmod.content.quest.area.goblinvillage

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class GoblinDiplomacy : QuestScript(
    "quest_goblindiplomacy",
    "varp.goblinquest",
    rewards {
        xp("stat.crafting", 200.0)
        item("obj.gold_bar")
    },
    ItemRewardDisplay("obj.goblin_armour_darkblue"),
    questVarbit = "varbit.goblin_diplomacy_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Goblin Diplomacy end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the blue mail hand-in completes at $COMPLETE_STAGE."
        }
        for (general in listOf("npc.general_bentnoze", "npc.general_wartface")) {
            onOpNpc1(general) { startDialogue(it.npc) { generalDialogue() } }
        }
        onOpHeldU(BLUE_DYE, MAIL) { dye(BLUE_MAIL, BLUE_DYE) }
        onOpHeldU(ORANGE_DYE, MAIL) { dye(ORANGE_MAIL, ORANGE_DYE) }
    }

    override fun subTitle(): String =
        "talking to <col=800000>General Bentnoze</col> or <col=800000>General Wartface</col> in the <col=800000>Goblin Village</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("The goblin generals <red>Wartface</red> and <red>Bentnoze</red> are arguing over the colour of their armour, and I have offered to settle it.")
        when (quest.getQuestStage(player.player)) {
            1 -> objective("They want to see a <red>plain goblin mail</red> first.") {
                hasItem("goblin_armour", "I have a goblin mail.")
            }
            2 -> objective("The plain mail was not good enough. Next I should show them an <red>orange goblin mail</red>, made with <red>orange dye</red>.") {
                hasItem("goblin_armour_orange", "I have an orange goblin mail.")
            }
            else -> objective("Orange was not the answer either. I should try a <red>blue goblin mail</red>, made with <red>blue dye</red>.") {
                hasItem("goblin_armour_darkblue", "I have a blue goblin mail.")
            }
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("The goblin generals argued over the colour of their armour.")
        line("I brought them a plain, an orange and finally a blue goblin mail, and they agreed on blue.")
    }

    private fun ProtectedAccess.dye(result: String, dye: String) {
        if (invDel(inv, dye).failure) return
        invDel(inv, MAIL)
        invAdd(inv, result)
        mes("You dye the goblin mail.")
    }

    private suspend fun Dialogue.generalDialogue() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> notStarted()
            QuestProgressState.IN_PROGRESS -> inProgress()
            QuestProgressState.FINISHED -> {
                chatNpc(happy, "Blue armour is the best armour. Thanks for sorting that out.")
            }
        }
    }

    private suspend fun Dialogue.notStarted() {
        chatNpc(angry, "Green armour best! Everyone knows it!")
        chatNpc(angry, "No, red armour best, you fool! Stop shouting at me!")
        when (choice2("Why are you arguing?", 1, "I don't want to get involved.", 2)) {
            1 -> {
                chatPlayer(quiz, "Why are you arguing?")
                chatNpc(sad, "Our goblins can't agree on what colour their armour should be. Soldiers are fighting each other instead of humans.")
                chatNpc(quiz, "Hey, human! You could help. Get us some armour in a colour we've not tried yet.")
                when (choice2("I'll see what I can do.", 1, "No thanks.", 2, title = "Start the Goblin Diplomacy quest?")) {
                    1 -> {
                        chatPlayer(happy, "I'll see what I can do.")
                        quest.advanceQuestStage(access)
                        chatNpc(happy, "Good. Start with plain goblin mail. Goblins in the village drop it, so look there.")
                    }
                    2 -> chatPlayer(bored, "No thanks, I have better things to do.")
                }
            }
            2 -> chatPlayer(worried, "I don't want to get involved.")
        }
    }

    private suspend fun Dialogue.inProgress() {
        val stage = quest.getQuestStage(player)
        val (item, wrong) = when (stage) {
            1 -> MAIL to "That's just plain goblin mail. Boring! Bring us a different colour."
            2 -> ORANGE_MAIL to "Orange? Hmm, no. Nobody likes orange. Try something else."
            else -> BLUE_MAIL to ""
        }
        chatNpc(quiz, "Well? Have you got the armour we asked for?")
        if (player.inv.count(item) == 0) {
            chatPlayer(sad, "Not yet.")
            chatNpc(angry, "Then stop wasting our time!")
            return
        }
        access.invDel(access.inv, item)
        chatPlayer(happy, "Here, take a look at this.")
        if (stage < 3) {
            chatNpc(angry, wrong)
            quest.advanceQuestStage(access)
            return
        }
        chatNpc(happy, "Blue! We both like blue. No more fighting about armour!")
        chatNpc(happy, "Take this gold as thanks, human.")
        quest.completeQuest(access)
    }

    private companion object {
        const val COMPLETE_STAGE = 6
        const val MAIL = "obj.goblin_armour"
        const val ORANGE_MAIL = "obj.goblin_armour_orange"
        const val BLUE_MAIL = "obj.goblin_armour_darkblue"
        const val BLUE_DYE = "obj.bluedye"
        const val ORANGE_DYE = "obj.orangedye"
    }
}
