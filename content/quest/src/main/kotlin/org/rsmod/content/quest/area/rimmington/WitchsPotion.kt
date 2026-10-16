package org.rsmod.content.quest.area.rimmington

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class WitchsPotion : QuestScript(
    "quest_witchspotion",
    "varp.hetty",
    rewards { xp("stat.magic", 325.0) },
    ItemRewardDisplay("obj.eye_of_newt"),
    questVarbit = "varbit.witchs_potion_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Witch's Potion end state is ${quest.maxSteps} in the cache dbrow, " +
                "but drinking from the cauldron completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.hetty") { startDialogue(it.npc) { hettyDialogue(it.npc) } }
        onOpLoc1("loc.hettycauldron") { drink() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Hetty</col> in <col=800000>Rimmington</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Hetty, a witch in <red>Rimmington</red>, is brewing a potion that will make her more powerful in magic.")
        when (quest.getQuestStage(player.player)) {
            1 -> objective("She needs <red>burnt meat</red>, an <red>eye of newt</red>, an <red>onion</red> and a <red>rat's tail</red>. I should bring them to her.") {
                hasItem("burnt_meat", "I have some burnt meat.")
                hasItem("eye_of_newt", "I have an eye of newt.")
                hasItem("onion", "I have an onion.")
                hasItem("rats_tail", "I have a rat's tail.")
            }
            else -> objective("Hetty has her ingredients. I should <red>drink from her cauldron</red> to receive the magic.") {}
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("Hetty needed some strange ingredients for her potion, and I fetched them for her.")
        line("When I drank from her cauldron I felt a surge of magical power.")
    }

    private fun hasIngredients(player: Player): Boolean = INGREDIENTS.all { player.inv.count(it) > 0 }

    private suspend fun Dialogue.hettyDialogue(npc: Npc) {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> notStarted()
            QuestProgressState.IN_PROGRESS ->
                if (quest.getQuestStage(player) == 1) gathering() else cauldronReady()
            QuestProgressState.FINISHED -> {
                chatNpc(happy, "How goes your adventuring, dear? That potion did you good, didn't it?")
                chatPlayer(happy, "It certainly did. Thank you!")
            }
        }
    }

    private suspend fun Dialogue.notStarted() {
        chatNpc(happy, "What could you want with an old woman like me?")
        when (
            choice2(
                "I am in search of a quest.",
                1,
                "I've heard that you are a witch.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "I am in search of a quest.")
                chatNpc(happy, "Hmmm... Maybe I can think of something for you.")
                chatNpc(quiz, "Would you like to become more proficient in the dark arts?")
                when (
                    choice2("Yes, help me become one with my darker side.", 1, "No, I have my principles and honour.", 2, title = "Start the Witch's Potion quest?")
                ) {
                    1 -> {
                        chatPlayer(happy, "Yes, help me become one with my darker side.")
                        chatNpc(happy, "Excellent. I'm brewing a potion and I need a few things for it: an eye of newt, a rat's tail, an onion and a piece of burnt meat.")
                        chatNpc(happy, "Bring them to me and the cauldron's magic is yours to drink.")
                        quest.advanceQuestStage(access)
                    }
                    2 -> {
                        chatPlayer(angry, "No, I have my principles and honour.")
                        chatNpc(sad, "Suit yourself, but you're missing out.")
                    }
                }
            }
            2 -> {
                chatPlayer(quiz, "I've heard that you are a witch.")
                chatNpc(laugh, "Heh, heh, heh. If you've heard that, you've heard more than most.")
            }
        }
    }

    private suspend fun Dialogue.gathering() {
        chatNpc(quiz, "Have you got my ingredients yet?")
        if (!hasIngredients(player)) {
            chatPlayer(sad, "Not all of them. Remind me what you need?")
            chatNpc(happy, "An eye of newt, a rat's tail, an onion and a piece of burnt meat. Burn some meat on a fire or range if you have to.")
            return
        }
        chatPlayer(happy, "Yes, I have everything right here.")
        for (item in INGREDIENTS) access.invDel(access.inv, item)
        chatNpc(happy, "Wonderful! Into the pot they go.")
        quest.advanceQuestStage(access)
        chatNpc(happy, "Now drink from the cauldron and you'll feel the magic flow through you.")
    }

    private suspend fun Dialogue.cauldronReady() {
        chatNpc(happy, "Well, what are you waiting for? Drink from the cauldron!")
    }

    private fun ProtectedAccess.drink() {
        when (quest.getQuestStage(player)) {
            2 -> {
                mes("You drink from the cauldron. It tastes horrible...")
                quest.advanceQuestStage(this)
            }
            else -> mes("The cauldron's contents bubble ominously. It doesn't seem wise to drink them.")
        }
    }

    private companion object {
        const val COMPLETE_STAGE = 3
        val INGREDIENTS = listOf("obj.burnt_meat", "obj.eye_of_newt", "obj.onion", "obj.rats_tail")
    }
}
