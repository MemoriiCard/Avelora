package org.rsmod.content.quest.area.falador

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class KnightsSword : QuestScript(
    KEY,
    "varp.squire",
    rewards { xp("stat.smithing", SMITHING_XP) },
    ItemRewardDisplay(BLURITE_SWORD, zoom = 400),
    questVarbit = "varbit.knights_sword_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Knight's Sword end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.squire") { startDialogue(it.npc) { squire() } }
        onOpNpc1("npc.reldo") { startDialogue(it.npc) { reldo() } }
        onOpLoc1("loc.vyvincupboardshut") { searchCupboard() }
        onOpLoc1("loc.vyvincupboardopen") { searchCupboard() }
    }

    override fun subTitle(): String =
        "talking to the <col=800000>Squire</col> in <col=800000>Falador castle</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("The Squire of Falador lost Sir Vyvin's family sword and needs it replaced before the knight notices.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I should ask <red>Reldo</red> in <red>Varrock Palace's library</red> about the <red>Imcando dwarves</red> who made the sword.") {}
            RELDO -> objective("Reldo suggests the dwarf <red>Thurgo</red> near <red>Port Sarim</red>, who loves <red>redberry pies</red>.") {
                hasItem("redberry_pie", "I have a redberry pie.")
            }
            PIE -> objective("Thurgo ate the pie but needs a <red>picture of the sword</red>. <red>Sir Vyvin</red> may keep one in his <red>cupboard</red>.") {
                hasItem("knights_portrait", "I have the portrait.")
            }
            GOT_PORTRAIT -> objective("I have the portrait. I should show it to <red>Thurgo</red>.") {}
            SHOWN -> objective("Thurgo needs <red>1 blurite ore</red> and <red>2 iron bars</red> to forge the sword.") {
                hasItem("blurite_ore", "I have the blurite ore.")
                hasItem("iron_bar", "I have the iron bars.")
            }
            else -> objective("Thurgo forged the sword. I should take it to the <red>Squire</red> in <red>Falador castle</red>.") {
                hasItem("faladian_sword", "I have the sword.")
            }
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("Thurgo forged a replica of Sir Vyvin's sword and the Squire's secret is safe.")
    }

    private suspend fun Dialogue.squire() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> squireStart()
            QuestProgressState.FINISHED -> chatNpc(happy, "Thank you for all your help, adventurer. Sir Vyvin never found out.")
            QuestProgressState.IN_PROGRESS ->
                if (quest.getQuestStage(player) == FORGED) squireFinish() else squireWaiting()
        }
    }

    private suspend fun Dialogue.squireStart() {
        chatNpc(sad, "Hello. I am the squire to Sir Vyvin.")
        chatPlayer(quiz, "And how is life as a squire?")
        chatNpc(sad, "Well, Sir Vyvin is a good guy to work for, however, I'm in a bit of trouble.")
        when (
            choice2(
                "Why, what happened?",
                1,
                "Sorry, I can't help right now.",
                2,
                title = "Start the Knight's Sword quest?",
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Why, what happened?")
                chatNpc(sad, "Well, you know the swords the knights use? One of them was Sir Vyvin's family sword and it was passed down through his family.")
                chatNpc(sad, "I was cleaning it, and... well, I accidentally broke it in half. If he finds out I'll be in big trouble.")
                chatPlayer(quiz, "Is there any way I can help?")
                chatNpc(happy, "Well, if I could get the sword re-forged, he would never find out. The Imcando dwarves made it, but nobody knows where they went.")
                chatPlayer(happy, "I'll see what I can find out.")
                quest.advanceQuestStage(access, STARTED)
            }
            2 -> chatPlayer(neutral, "Sorry, I can't help right now.")
        }
    }

    private suspend fun Dialogue.squireWaiting() {
        chatNpc(quiz, "So how are you doing getting a sword?")
        if (quest.getQuestStage(player) == STARTED) {
            chatPlayer(neutral, "I'm still looking. Perhaps Reldo in Varrock will know about the Imcando dwarves.")
        } else {
            chatPlayer(neutral, "I'm working on it. Thurgo is making progress.")
        }
    }

    private suspend fun Dialogue.squireFinish() {
        if (access.inv.count(BLURITE_SWORD) == 0) {
            chatNpc(quiz, "So how are you doing getting a sword?")
            chatPlayer(neutral, "I've got the sword from Thurgo, but I've misplaced it. I'll go back for it.")
            return
        }
        chatNpc(happy, "How are you doing getting a sword?")
        chatPlayer(happy, "I have the sword!")
        access.invDel(access.inv, BLURITE_SWORD)
        chatNpc(happy, "Thank you, thank you, thank you! I was so worried about what Sir Vyvin would do.")
        quest.advanceQuestStage(access, COMPLETE_STAGE - FORGED)
    }

    private suspend fun Dialogue.reldo() {
        if (quest.getQuestStage(player) == STARTED) {
            chatPlayer(quiz, "What do you know about the Imcando dwarves?")
            chatNpc(neutral, "The Imcando dwarves, you say? They were the greatest smiths this world has seen, but have long since disappeared.")
            chatNpc(happy, "I do know one still lives. Thurgo is south of Port Sarim. He loves redberry pies, so I would bring him one as a gift.")
            quest.advanceQuestStage(access, RELDO - STARTED)
            return
        }
        chatNpc(happy, "Hello stranger.")
        chatPlayer(neutral, "Hello. Just browsing the library.")
    }

    private suspend fun ProtectedAccess.searchCupboard() {
        arriveDelay()
        if (quest.getQuestStage(player) != PIE || inv.count(PORTRAIT_OBJ) > 0) {
            mes("You search the cupboard but find nothing interesting.")
            return
        }
        invAdd(inv, PORTRAIT_OBJ, 1)
        mes("You find a small portrait in here which you take.")
        quest.advanceQuestStage(this, GOT_PORTRAIT - PIE)
    }

    companion object {
        const val KEY = "quest_knightssword"
        const val SMITHING_XP = 12725.0
        const val BLURITE_ORE = "obj.blurite_ore"
        const val IRON_BAR = "obj.iron_bar"
        const val REDBERRY_PIE = "obj.redberry_pie"
        const val PORTRAIT_OBJ = "obj.knights_portrait"
        const val BLURITE_SWORD = "obj.faladian_sword"
        const val STARTED = 1
        const val RELDO = 2
        const val PIE = 3
        const val GOT_PORTRAIT = 4
        const val SHOWN = 5
        const val FORGED = 6
        const val COMPLETE_STAGE = 7
    }
}
