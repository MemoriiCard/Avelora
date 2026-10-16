package org.rsmod.content.quest.area.taverley

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class DruidicRitual : QuestScript(
    QUEST,
    "varp.druidquest",
    rewards {
        xp("stat.herblore", 250.0)
        extra("Access to the Herblore skill")
    },
    ItemRewardDisplay("obj.enchanted_beef", zoom = 400),
    questVarbit = "varbit.druidic_ritual_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Druidic Ritual end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.kaqemeex") { startDialogue(it.npc) { kaqemeex() } }
        onOpNpc1("npc.sanfew") { startDialogue(it.npc) { sanfew() } }
        for ((raw, enchanted) in MEATS) {
            onOpLocU("loc.cauldron_of_thunder", raw) { enchant(raw, enchanted) }
        }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Kaqemeex</col> at the Druids' stone circle north of Taverley."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Kaqemeex wants the stone circle south of Varrock cleansed, and Sanfew will help.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I should speak to <red>Sanfew</red> in the herblore shop south of Taverley.") {}
            SANFEW -> objective("Sanfew needs raw <red>bear meat</red>, <red>rat meat</red>, <red>beef</red> and <red>chicken</red> enchanted in the <red>Cauldron of Thunder</red> in Taverley Dungeon.") {
                hasItem("enchanted_bear_meat", "I have enchanted bear meat.")
                hasItem("enchanted_rat_meat", "I have enchanted rat meat.")
                hasItem("enchanted_beef", "I have enchanted beef.")
                hasItem("enchanted_chicken", "I have enchanted chicken.")
            }
            else -> objective("Sanfew has the enchanted meat. I should tell <red>Kaqemeex</red>.") {}
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I helped the druids of Taverley cleanse their stone circle and learned the basics of Herblore.")
    }

    private suspend fun Dialogue.kaqemeex() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> {
                chatNpc(neutral, "What brings you to our holy monument?")
                if (!choice2("I'm in search of a quest.", true, "Who are you?", false)) {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(neutral, "We are the druids of Guthix, who protect the balance of nature.")
                    return
                }
                chatPlayer(quiz, "I'm in search of a quest.")
                chatNpc(neutral, "Our stone circle south of Varrock has been defiled by the dark wizards. Will you help us?")
                chatNpc(neutral, "You must speak to Sanfew in the herblore shop south of Taverley. He will tell you what is needed.")
                quest.advanceQuestStage(access, STARTED)
            }
            QuestProgressState.FINISHED ->
                chatNpc(happy, "Thank you again for your help, adventurer. Guthix watches over you.")
            QuestProgressState.IN_PROGRESS ->
                if (quest.getQuestStage(player) == MEATS_GIVEN) {
                    chatPlayer(happy, "Sanfew has enchanted the meat. The stone circle can be cleansed.")
                    chatNpc(happy, "Excellent! Let me teach you the basics of Herblore as thanks.")
                    quest.advanceQuestStage(access, COMPLETE_STAGE - MEATS_GIVEN)
                } else {
                    chatNpc(neutral, "Have you spoken to Sanfew yet? He is in the herblore shop south of Taverley.")
                }
        }
    }

    private suspend fun Dialogue.sanfew() {
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) {
            chatNpc(neutral, "What can I do for you?")
            return
        }
        when (quest.getQuestStage(player)) {
            STARTED -> {
                chatPlayer(neutral, "Kaqemeex sent me to help.")
                chatNpc(neutral, "The ritual needs raw bear meat, raw rat meat, raw beef and raw chicken, enchanted in the Cauldron of Thunder in Taverley Dungeon.")
                chatNpc(neutral, "Bring them back to me once they are enchanted.")
                quest.advanceQuestStage(access, SANFEW - STARTED)
            }
            SANFEW -> {
                if (MEATS.any { (_, enchanted) -> access.inv.count(enchanted) == 0 }) {
                    chatNpc(neutral, "Bring me enchanted bear meat, rat meat, beef and chicken from the Cauldron of Thunder.")
                    return
                }
                for ((_, enchanted) in MEATS) access.invDel(access.inv, enchanted, 1)
                chatPlayer(happy, "I have the enchanted meat.")
                chatNpc(happy, "Well done! Take word of this to Kaqemeex.")
                quest.advanceQuestStage(access, MEATS_GIVEN - SANFEW)
            }
            else -> chatNpc(neutral, "Take word to Kaqemeex, the ritual is ready.")
        }
    }

    private fun ProtectedAccess.enchant(raw: String, enchanted: String) {
        if (quest.getQuestStage(player) < SANFEW) {
            mes("Nothing interesting happens.")
            return
        }
        invReplace(inv, raw, 1, enchanted)
        mes("You dip the meat in the cauldron and it turns a strange blue.")
    }

    companion object {
        const val QUEST = "quest_druidicritual"
        val MEATS = listOf(
            "obj.raw_bear_meat" to "obj.enchanted_bear_meat",
            "obj.raw_rat_meat" to "obj.enchanted_rat_meat",
            "obj.raw_beef" to "obj.enchanted_beef",
            "obj.raw_chicken" to "obj.enchanted_chicken",
        )
        const val STARTED = 1
        const val SANFEW = 2
        const val MEATS_GIVEN = 3
        const val COMPLETE_STAGE = 4
    }
}
