package org.rsmod.content.quest.area.lumbridge

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class SheepShearer : QuestScript(
    "quest_sheepshearer",
    "varp.sheep",
    rewards {
        xp("stat.crafting", 150.0)
        item("obj.coins", 60)
    },
    ItemRewardDisplay("obj.ball_of_wool"),
    questVarbit = "varbit.sheep_shearer_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Sheep Shearer end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the final hand-in completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.fred_the_farmer") { startDialogue(it.npc) { fredDialogue() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Farmer Fred</col> north of <col=800000>Lumbridge</col>."

    private fun delivered(player: org.rsmod.game.entity.Player): Int =
        (quest.getQuestStage(player) - 1).coerceAtLeast(0)

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        val handedIn = delivered(player.player)
        description("Farmer Fred needs <red>20 balls of wool</red> and has asked me to bring them to him. I can shear sheep with a pair of <red>shears</red> and spin the wool on a <red>spinning wheel</red>.")
        objective("I have handed in <red>$handedIn</red> of the 20 balls of wool so far.") {}
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("Farmer Fred needed twenty balls of wool, so I sheared sheep and spun the wool for him.")
        line("He paid me 60 coins for my trouble.")
    }

    private suspend fun Dialogue.fredDialogue() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> notStarted()
            QuestProgressState.IN_PROGRESS -> collecting()
            QuestProgressState.FINISHED -> {
                chatNpc(happy, "Thanks again for all that wool. My sheep are a lot lighter now!")
            }
        }
    }

    private suspend fun Dialogue.notStarted() {
        chatNpc(confused, "What are you doing on my land? You're not the one who keeps leaving my gates open, are you?")
        when (choice2("I'm looking for a quest.", 1, "I'm lost.", 2)) {
            1 -> {
                chatPlayer(quiz, "I'm looking for a quest.")
                chatNpc(happy, "You're after a quest, you say? Actually I could do with a bit of help.")
                chatNpc(happy, "My sheep are getting too woolly and I'm needing to shear them. But the thing is, I need the wool spun into balls so I can sell it.")
                chatNpc(quiz, "If you can bring me 20 balls of wool, I'll pay you for your trouble. Interested?")
                when (choice2("Yes okay. I can do that.", 1, "That doesn't sound like a very exciting quest.", 2, title = "Start the Sheep Shearer quest?")) {
                    1 -> {
                        chatPlayer(happy, "Yes okay. I can do that.")
                        quest.advanceQuestStage(access)
                        chatNpc(happy, "Fantastic! Shears are on the table in my house, and the spinning wheel is in Lumbridge Castle. Bring me 20 balls of wool.")
                    }
                    2 -> {
                        chatPlayer(bored, "That doesn't sound like a very exciting quest.")
                        chatNpc(sad, "Well, hardly any quests are exciting when you do them the sensible way.")
                    }
                }
            }
            2 -> {
                chatPlayer(sad, "I'm lost.")
                chatNpc(shifty, "How can you be lost? Just follow the road east and you'll be back in Lumbridge.")
            }
        }
    }

    private suspend fun Dialogue.collecting() {
        chatNpc(quiz, "How are you doing getting those balls of wool?")
        val have = player.inv.count(WOOL)
        if (have == 0) {
            chatPlayer(sad, "I haven't got any yet.")
            chatNpc(happy, "Well, you can't get them from me. Shear some sheep and spin the wool on a spinning wheel.")
            return
        }
        val give = minOf(have, REQUIRED - delivered(player))
        access.invDel(access.inv, WOOL, give)
        chatPlayer(happy, "I have some wool to give you.")
        quest.advanceQuestStage(access, give)
        if (quest.getQuestStage(player) >= COMPLETE_STAGE) {
            chatNpc(happy, "That's the last of it. Thank you, here's your payment.")
        } else {
            val left = REQUIRED - delivered(player)
            chatNpc(happy, "Thank you! I still need $left more.")
        }
    }

    private companion object {
        const val WOOL = "obj.ball_of_wool"
        const val REQUIRED = 20
        const val COMPLETE_STAGE = 21
    }
}
