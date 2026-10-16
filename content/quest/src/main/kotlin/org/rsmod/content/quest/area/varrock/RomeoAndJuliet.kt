package org.rsmod.content.quest.area.varrock

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class RomeoAndJuliet : QuestScript(
    "quest_romeoandjuliet",
    "varp.rjquest",
    rewards {},
    ItemRewardDisplay("obj.cadava"),
    questVarbit = "varbit.romeo_juliet_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Romeo & Juliet end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the finale completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.romeo") { startDialogue(it.npc) { romeo() } }
        onOpNpc1("npc.juliet") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.father_lawrence") { startDialogue(it.npc) { lawrence() } }
        onOpNpc1("npc.apothecary") { startDialogue(it.npc) { apothecary() } }
        onOpNpc1("npc.phillipa") { startDialogue(it.npc) { phillipa() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Romeo</col> in <col=800000>Varrock Square</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Romeo is searching Varrock for his love, Juliet, and wants me to help bring them together.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I should find <red>Juliet</red> in the <red>Leptoc mansion</red> west of Varrock and tell her Romeo still loves her.") {}
            GOT_LETTER -> objective("Juliet gave me a <red>message</red> to take back to <red>Romeo</red>.") {
                hasItem("julietmessage", "I have Juliet's message.")
            }
            DELIVERED -> objective("Romeo despairs. I should ask <red>Father Lawrence</red> in the <red>Varrock church</red> for help.") {}
            LAWRENCE -> objective("Father Lawrence suggested a <red>Cadava potion</red>. I should ask the <red>Apothecary</red>.") {}
            APOTHECARY -> objective("The Apothecary needs <red>cadava berries</red>, which grow near the <red>south-east Varrock mine</red>.") {
                hasItem("cadavaberries", "I have the cadava berries.")
            }
            POTION -> objective("I have a <red>Cadava potion</red> for <red>Juliet</red>.") {
                hasItem("cadava", "I have the Cadava potion.")
            }
            else -> objective("Juliet drank the potion. I should tell <red>Romeo</red> to collect her from the <red>Varrock crypt</red>.") {}
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I carried messages between Romeo and Juliet and helped Father Lawrence's plan along.")
        line("Romeo went to the crypt for Juliet, but fell for her cousin Phillipa instead.")
    }

    private suspend fun Dialogue.romeo() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> romeoStart()
            QuestProgressState.FINISHED -> {
                chatNpc(happy, "Phillipa and I have never been happier. Thank you for your help!")
            }
            QuestProgressState.IN_PROGRESS ->
                when (quest.getQuestStage(player)) {
                    STARTED -> {
                        chatNpc(quiz, "Have you found my Juliet yet?")
                        chatPlayer(neutral, "Not yet. I'll head to the Leptoc mansion.")
                    }
                    GOT_LETTER -> romeoLetter()
                    DELIVERED, LAWRENCE ->
                        chatNpc(sad, "Please, speak to Father Lawrence in the church. He is our only hope.")
                    APOTHECARY, POTION ->
                        chatNpc(sad, "Have you been able to arrange the potion for my Juliet?")
                    else -> romeoFinale()
                }
        }
    }

    private suspend fun Dialogue.romeoStart() {
        chatNpc(sad, "Juliet, Juliet, wherefore art thou Juliet? Have you seen my Juliet?")
        when (
            choice3(
                "Yes, I have seen her actually!",
                1,
                "No sorry, I haven't seen her.",
                2,
                "Perhaps I could help to find her for you?",
                3,
                title = "Start the Romeo & Juliet quest?",
            )
        ) {
            1 -> {
                chatPlayer(happy, "Yes, I have seen her actually!")
                romeoAskForHelp()
            }
            2 -> {
                chatPlayer(neutral, "No sorry, I haven't seen her.")
                chatNpc(sad, "Oh... if you do, please find me.")
            }
            3 -> {
                chatPlayer(quiz, "Perhaps I could help to find her for you?")
                romeoAskForHelp()
            }
        }
    }

    private suspend fun Dialogue.romeoAskForHelp() {
        chatNpc(happy, "Oh would you? That would be wonderful! Please tell her that she is the love of my life and that I long to be with her.")
        chatPlayer(quiz, "Yes, okay. I'll let her know.")
        chatNpc(happy, "Tell her that I still love her, and I will find a way for us to be together.")
        quest.advanceQuestStage(access, STARTED)
    }

    private suspend fun Dialogue.romeoLetter() {
        if (access.inv.count("obj.julietmessage") == 0) {
            chatNpc(quiz, "Did Juliet give you a message for me?")
            chatPlayer(sad, "I seem to have lost it. I'll ask her again.")
            return
        }
        chatPlayer(happy, "Romeo, I have a message from Juliet.")
        access.invDel(access.inv, "obj.julietmessage")
        chatNpc(happy, "A letter! Let me see... 'I still feel great affection for you, but unfortunately my Father opposes our marriage.'")
        chatNpc(sad, "'He will kill you if he sees you again! Our only hope is that Father Lawrence, our long time confidant, can help us in some way.'")
        chatNpc(sad, "Well, that's it then... we haven't got a chance...")
        chatPlayer(quiz, "Why don't you speak to Father Lawrence? He might know what to do.")
        chatNpc(happy, "You're right! Please, speak to Father Lawrence and see what he suggests.")
        quest.advanceQuestStage(access, DELIVERED - GOT_LETTER)
    }

    private suspend fun Dialogue.romeoFinale() {
        chatPlayer(happy, "Juliet has drunk the potion and been taken to the crypt.")
        chatNpc(sad, "But I'm scared... will you come with me?")
        chatPlayer(happy, "Of course. Let's go.")
        chatNpc(sad, "Oh dear... Juliet, you seem to be dead.")
        chatNpc(happy, "Wait... who is this? Phillipa, Juliet's cousin? Wow! You're a fox!")
        chatNpc(quiz, "Juliet? Who's Juliet?")
        quest.advanceQuestStage(access, COMPLETE_STAGE - COMPLETE_FROM)
    }

    private suspend fun Dialogue.juliet() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> {
                chatNpc(sad, "Romeo, Romeo, wherefore art thou Romeo? Oh, I'm sorry, I thought you were someone else.")
            }
            QuestProgressState.FINISHED -> {
                chatNpc(happy, "Thank you for all your help. I do hope Romeo and Phillipa will be very happy.")
            }
            QuestProgressState.IN_PROGRESS ->
                when (quest.getQuestStage(player)) {
                    STARTED -> julietMessage()
                    GOT_LETTER ->
                        chatNpc(quiz, "Please, take that message to Romeo with great haste.")
                    POTION -> julietPotion()
                    else -> chatNpc(sad, "Everything rests on Father Lawrence's plan. Please hurry.")
                }
        }
    }

    private suspend fun Dialogue.julietMessage() {
        chatPlayer(happy, "Juliet, I come from Romeo. He begs me to tell you that he cares still.")
        chatNpc(happy, "Oh how my heart soars to hear this news! Please take this message to him with great haste.")
        access.invAdd(access.inv, "obj.julietmessage", 1)
        chatNpc(happy, "Everything is explained in the letter. Would you be so kind and deliver it to him please?")
        quest.advanceQuestStage(access, GOT_LETTER - STARTED)
    }

    private suspend fun Dialogue.julietPotion() {
        if (access.inv.count("obj.cadava") == 0) {
            chatNpc(quiz, "Have you got the potion yet?")
            return
        }
        chatPlayer(happy, "I have a Cadava potion here, suggested by Father Lawrence. If you drink it, it will make you appear dead!")
        chatNpc(sad, "Ok... if this is the best we can do... hand over the potion!")
        access.invDel(access.inv, "obj.cadava")
        chatNpc(happy, "Wonderful! I just hope Romeo can remember to get me from the crypt.")
        chatNpc(sad, "Urk!")
        quest.advanceQuestStage(access, COMPLETE_FROM - POTION)
    }

    private suspend fun Dialogue.lawrence() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED, QuestProgressState.FINISHED ->
                chatNpc(angry, "Can't you see that I'm in the middle of a Sermon?!")
            QuestProgressState.IN_PROGRESS ->
                if (quest.getQuestStage(player) == DELIVERED) {
                    chatPlayer(quiz, "Romeo sent me. He says you may be able to help.")
                    chatNpc(happy, "Ah, Romeo, a fine lad. Juliet's father forbids their love, so we need a plan.")
                    chatNpc(happy, "A potion to make her appear dead! Romeo can then collect her from the crypt.")
                    chatNpc(happy, "Go to the Apothecary, tell him I sent you and that you'll need a 'Cadava' potion.")
                    quest.advanceQuestStage(access, LAWRENCE - DELIVERED)
                } else {
                    chatNpc(quiz, "Go to the Apothecary, tell him I sent you and that you'll need a 'Cadava' potion.")
                }
        }
    }

    private suspend fun Dialogue.apothecary() {
        val stage = quest.getQuestStage(player)
        val active = quest.questState(player) == QuestProgressState.IN_PROGRESS
        when {
            active && stage == LAWRENCE -> {
                chatPlayer(quiz, "Apothecary, Father Lawrence sent me. I need a Cadava potion.")
                chatNpc(neutral, "Cadava potion. It's pretty nasty. And hard to make.")
                chatNpc(neutral, "Wing of rat, tail of frog. Ear of snake and horn of dog. I have all that, but I need some Cadava berries.")
                chatNpc(neutral, "You will have to find them while I get the rest ready. Bring them here when you have them. But be careful.")
                chatNpc(neutral, "They are bright pink with green leaves and pale brown stalks. The kids found them near the south-eastern Varrock mining pit.")
                quest.advanceQuestStage(access, APOTHECARY - LAWRENCE)
            }
            active && stage == APOTHECARY -> {
                if (access.inv.count("obj.cadavaberries") == 0) {
                    chatNpc(quiz, "Have you found any cadava berries yet?")
                    chatPlayer(sad, "Not yet.")
                    return
                }
                chatPlayer(happy, "I have the cadava berries.")
                access.invDel(access.inv, "obj.cadavaberries")
                chatNpc(happy, "Well done. Here is your potion. Be careful not to drink it yourself.")
                access.invAdd(access.inv, "obj.cadava", 1)
                quest.advanceQuestStage(access, POTION - APOTHECARY)
            }
            else -> chatNpc(neutral, "I am the Apothecary. I brew potions. Do you need anything specific?")
        }
    }

    private suspend fun Dialogue.phillipa() {
        chatNpc(happy, "Hello there. I'm Juliet's cousin, Phillipa.")
    }

    private companion object {
        const val STARTED = 10
        const val GOT_LETTER = 20
        const val DELIVERED = 30
        const val LAWRENCE = 40
        const val APOTHECARY = 50
        const val POTION = 60
        const val COMPLETE_FROM = 70
        const val COMPLETE_STAGE = 100
    }
}
