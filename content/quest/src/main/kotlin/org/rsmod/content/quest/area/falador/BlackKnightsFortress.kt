package org.rsmod.content.quest.area.falador

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class BlackKnightsFortress : QuestScript(
    QUEST,
    "varp.spy",
    rewards { item("obj.coins", COINS) },
    ItemRewardDisplay("obj.coins", zoom = 400),
    questVarbit = "varbit.black_knights_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Black Knights' Fortress end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.sir_amik_varze") { startDialogue(it.npc) { amik() } }
        onOpLoc1("loc.witchgrill") { listen() }
        onOpLocU("loc.blackknighthole", CABBAGE) { sabotage() }
        onOpLocU("loc.blackknighthole", MAGIC_CABBAGE) { wrongCabbage() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Sir Amik Varze</col> in <col=800000>Falador castle</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Sir Amik Varze wants me to find out what the Black Knights' secret weapon is, and ruin it.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I should infiltrate the <red>Black Knights' fortress</red> north of Falador and listen in on their plans through the <red>grill</red>.") {}
            LISTENED -> objective("The witch is brewing a potion that needs a <red>Draynor Manor cabbage</red>. I should tell <red>Sir Amik Varze</red>.") {}
            REPORTED -> objective("I should drop an ordinary <red>cabbage</red> through the <red>hole</red> above the witch's cauldron.") {
                hasItem("cabbage", "I have a cabbage.")
            }
            else -> objective("The potion is ruined. I should tell <red>Sir Amik Varze</red>.") {}
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I sabotaged the Black Knights' secret weapon and Sir Amik Varze paid me for it.")
    }

    private suspend fun Dialogue.amik() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> amikStart()
            QuestProgressState.FINISHED ->
                chatNpc(happy, "Thank you for your help, adventurer. The Black Knights will trouble us no more.")
            QuestProgressState.IN_PROGRESS ->
                when (quest.getQuestStage(player)) {
                    STARTED -> {
                        chatNpc(quiz, "Have you managed to find out what the Black Knights are up to?")
                        chatPlayer(neutral, "Not yet. I'll try listening at the grill in their fortress.")
                    }
                    LISTENED -> amikReport()
                    REPORTED -> {
                        chatNpc(neutral, "Get an ordinary cabbage and drop it down the hole above their cauldron. Not a Draynor Manor one.")
                    }
                    else -> amikFinish()
                }
        }
    }

    private suspend fun Dialogue.amikStart() {
        chatNpc(neutral, "I am Sir Amik Varze, the leader of the White Knights of Falador. Why do you seek my audience?")
        when (
            choice2(
                "I seek a quest!",
                1,
                "I don't, I'm just looking around.",
                2,
                title = "Start the Black Knights' Fortress quest?",
            )
        ) {
            1 -> {
                chatPlayer(happy, "I seek a quest!")
                if (player.vars["varp.qp"] < QP_REQUIRED) {
                    chatNpc(sad, "I'm afraid you are not experienced enough. Come back when you have at least $QP_REQUIRED quest points.")
                    return
                }
                chatNpc(neutral, "Well, the Black Knights have made a secret weapon in their fortress north of here. We need someone to infiltrate it and find out what it is.")
                chatNpc(neutral, "Take this dossier. It will help you.")
                if (access.invAdd(access.inv, DOSSIER, 1).failure) {
                    chatNpc(sad, "You don't have room for it. Come back with some free space.")
                    return
                }
                quest.advanceQuestStage(access, STARTED)
            }
            2 -> chatPlayer(neutral, "I don't, I'm just looking around.")
        }
    }

    private suspend fun Dialogue.amikReport() {
        chatPlayer(happy, "I've listened in. The witch is making a potion, and it needs a Draynor Manor cabbage.")
        chatNpc(happy, "Excellent work. Then you must sabotage it. Drop an ordinary cabbage down the hole above the cauldron while they meet.")
        quest.advanceQuestStage(access, REPORTED - LISTENED)
    }

    private suspend fun Dialogue.amikFinish() {
        chatPlayer(happy, "The potion has been ruined.")
        chatNpc(happy, "Excellent! The Black Knights have withdrawn their demands. Here is your payment.")
        quest.advanceQuestStage(access, COMPLETE_STAGE - SABOTAGED)
    }

    private suspend fun ProtectedAccess.listen() {
        if (quest.getQuestStage(player) < STARTED) {
            mes("You hear muffled voices but can't make out what is being said.")
            return
        }
        mes("You hear a witch say: 'It's taken me FIVE YEARS, but it's almost ready.'")
        mes("'Only a Draynor Manor cabbage will do for the final ingredient!'")
        if (quest.getQuestStage(player) == STARTED) quest.advanceQuestStage(this, LISTENED - STARTED)
    }

    private suspend fun ProtectedAccess.sabotage() {
        if (quest.getQuestStage(player) != REPORTED) {
            mes("You should find out what to do before throwing food around.")
            return
        }
        invDel(inv, CABBAGE, 1)
        mes("The cabbage falls down the hole into the potion, ruining it.")
        quest.advanceQuestStage(this, SABOTAGED - REPORTED)
    }

    private fun ProtectedAccess.wrongCabbage() {
        mes("This is the wrong sort of cabbage!")
    }

    companion object {
        const val QUEST = "quest_blackknightsfortress"
        const val COINS = 2500
        const val QP_REQUIRED = 12
        const val DOSSIER = "obj.bk_dossier"
        const val CABBAGE = "obj.cabbage"
        const val MAGIC_CABBAGE = "obj.draynor_magic_cabbage"
        const val STARTED = 1
        const val LISTENED = 2
        const val REPORTED = 3
        const val SABOTAGED = 4
        const val COMPLETE_STAGE = 5
    }
}
