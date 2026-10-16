package org.rsmod.content.quest.area.falador

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.BLURITE_ORE
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.BLURITE_SWORD
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.FORGED
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.GOT_PORTRAIT
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.IRON_BAR
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.KEY
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.PIE
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.PORTRAIT_OBJ
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.REDBERRY_PIE
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.RELDO
import org.rsmod.content.quest.area.falador.KnightsSword.Companion.SHOWN
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestProgressState

object KnightsSwordThurgo {
    suspend fun Dialogue.talk(): Boolean {
        val quest = Quest.get(KEY) ?: return false
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) return false
        when (quest.getQuestStage(player)) {
            RELDO -> pie()
            PIE -> needPortrait()
            GOT_PORTRAIT -> showPortrait()
            SHOWN -> forge()
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.pie() {
        val quest = Quest.get(KEY) ?: return
        chatNpc(neutral, "What do you want? I'm busy.")
        chatPlayer(quiz, "I'm looking for a dwarf who can forge Sir Vyvin's sword.")
        if (access.inv.count(REDBERRY_PIE) == 0) {
            chatNpc(angry, "Mmm... Imcando dwarves would do anything for a redberry pie. Come back with one.")
            return
        }
        chatPlayer(happy, "Would you like a redberry pie?")
        mesbox("You see Thurgo's eyes light up.")
        chatNpc(happy, "I'd never say no to a redberry pie! We Imcando dwarves love them - they're GREAT!")
        access.invDel(access.inv, REDBERRY_PIE, 1)
        objbox(REDBERRY_PIE, zoom = 400, "You hand over the pie. Thurgo eats the pie. Thurgo pats his stomach.")
        chatNpc(happy, "By Guthix! THAT was good pie! Anyone who makes pie like THAT has got to be alright!")
        chatPlayer(quiz, "Can you make me a replica of Sir Vyvin's sword?")
        chatNpc(neutral, "I'd need a picture of the sword to copy. Sir Vyvin must have one somewhere in his castle.")
        quest.advanceQuestStage(access, PIE - RELDO)
    }

    private suspend fun Dialogue.needPortrait() {
        chatNpc(quiz, "Have you got a picture of the sword for me yet?")
        chatPlayer(neutral, "Not yet. I'll search Sir Vyvin's castle.")
    }

    private suspend fun Dialogue.showPortrait() {
        val quest = Quest.get(KEY) ?: return
        if (access.inv.count(PORTRAIT_OBJ) == 0) {
            chatNpc(quiz, "Have you got a picture of the sword for me yet?")
            chatPlayer(neutral, "I seem to have lost it. I'll look again.")
            return
        }
        chatPlayer(happy, "I have the portrait of the sword.")
        access.invDel(access.inv, PORTRAIT_OBJ, 1)
        chatNpc(neutral, "Thanks. Let me see... Yes, I can copy that. I'll need blurite ore and two iron bars.")
        chatNpc(neutral, "Blurite is hard to come by. You'll find some in the cave under the Asgarnian Ice Dungeon.")
        quest.advanceQuestStage(access, SHOWN - GOT_PORTRAIT)
    }

    private suspend fun Dialogue.forge() {
        val quest = Quest.get(KEY) ?: return
        chatNpc(quiz, "Have you got the blurite ore and two iron bars?")
        val inv = access.inv
        if (inv.count(BLURITE_ORE) < 1 || inv.count(IRON_BAR) < 2) {
            chatPlayer(neutral, "Not yet. I'll go and get them.")
            return
        }
        access.invDel(inv, BLURITE_ORE, 1)
        access.invDel(inv, IRON_BAR, 2)
        access.invAdd(inv, BLURITE_SWORD, 1)
        objbox(BLURITE_SWORD, zoom = 400, "You give Thurgo the ore and bars. Thurgo forges the sword.")
        chatNpc(happy, "Here you go. Tell the squire it's the best I can do.")
        quest.advanceQuestStage(access, FORGED - SHOWN)
    }
}
