package org.rsmod.content.quest.area.falador

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class DoricsQuest : QuestScript(
    "quest_dorics",
    "varp.doricquest",
    rewards {
        xp("stat.mining", 1300.0)
        item("obj.coins", 180)
        extra("Use of Doric's anvils")
    },
    ItemRewardDisplay("obj.clay"),
    questVarbit = "varbit.dorics_quest_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Doric's Quest end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the hand-in completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.doric") { startDialogue(it.npc) { doricDialogue() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Doric</col> north of <col=800000>Falador</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Doric the dwarf wants to make use of his anvils but is short of metal. He asked me to bring him some ore and clay.")
        objective("I need to bring Doric <red>6 clay</red>, <red>4 copper ore</red> and <red>2 iron ore</red>.") {
            hasItem("clay", "I have the clay.")
            hasItem("copper_ore", "I have the copper ore.")
            hasItem("iron_ore", "I have the iron ore.")
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("Doric needed clay, copper ore and iron ore for his smithing, and I fetched them.")
        line("In return he paid me and lets me use his anvils whenever I like.")
    }

    private fun hasMaterials(player: Player): Boolean =
        MATERIALS.all { (item, count) -> player.inv.count(item) >= count }

    private suspend fun Dialogue.doricDialogue() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> notStarted()
            QuestProgressState.IN_PROGRESS -> collecting()
            QuestProgressState.FINISHED -> {
                chatNpc(happy, "Hello again. Feel free to use my anvils any time you like.")
            }
        }
    }

    private suspend fun Dialogue.notStarted() {
        chatNpc(happy, "Hello traveller, what brings you to my humble smithy?")
        when (choice2("I wanted to use your anvils.", 1, "I want to use your whetstone.", 2)) {
            1 -> {
                chatPlayer(quiz, "I wanted to use your anvils.")
                chatNpc(sad, "My anvils get enough work with my own use. I make pickaxes, and it takes a lot of ore. If you could get me some more, you could use them.")
                when (choice2("Yes, I will get you the materials.", 1, "No, hitting rocks is for the boring people, sorry.", 2, title = "Start Doric's Quest?")) {
                    1 -> {
                        chatPlayer(happy, "Yes, I will get you the materials.")
                        chatNpc(happy, "Clay is what I need first, six pieces. After that, four copper ore and two iron ore. Bring them all and the anvils are yours.")
                        quest.setQuestStage(access, STARTED_STAGE)
                    }
                    2 -> {
                        chatPlayer(bored, "No, hitting rocks is for the boring people, sorry.")
                        chatNpc(angry, "Fine, off you go then.")
                    }
                }
            }
            2 -> {
                chatPlayer(quiz, "I want to use your whetstone.")
                chatNpc(confused, "The whetstone is for more advanced smithing, but I could let you use it if you help me first.")
            }
        }
    }

    private suspend fun Dialogue.collecting() {
        chatNpc(quiz, "Have you got my materials yet, traveller?")
        if (!hasMaterials(player)) {
            chatPlayer(sad, "Sorry, I don't have them all yet.")
            chatNpc(neutral, "I need six clay, four copper ore and two iron ore. Come back when you have them.")
            return
        }
        chatPlayer(happy, "I have everything you asked for.")
        for ((item, count) in MATERIALS) access.invDel(access.inv, item, count)
        chatNpc(happy, "Many thanks! Take these coins for your trouble, and feel free to use my anvils whenever you want.")
        quest.completeQuest(access)
    }

    private companion object {
        const val STARTED_STAGE = 10
        const val COMPLETE_STAGE = 100
        val MATERIALS = listOf("obj.clay" to 6, "obj.copper_ore" to 4, "obj.iron_ore" to 2)
    }
}
