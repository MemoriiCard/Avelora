package org.rsmod.content.quest.area.portsarim

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.quest.area.portsarim.PiratesTreasure.Companion.APRON
import org.rsmod.content.quest.area.portsarim.PiratesTreasure.Companion.CHEST_KEY
import org.rsmod.content.quest.area.portsarim.PiratesTreasure.Companion.KEY
import org.rsmod.content.quest.area.portsarim.PiratesTreasure.Companion.QUEST
import org.rsmod.content.quest.area.portsarim.PiratesTreasure.Companion.RUM
import org.rsmod.content.quest.area.portsarim.PiratesTreasure.Companion.STARTED
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.game.entity.Player

object PiratesTreasureHooks {
    private var Player.rumHidden by intVarBit("varbit.pirates_treasure_rum_hidden")
    private var Player.rumShipped by intVarBit("varbit.pirates_treasure_rum_shipped")
    private var Player.bananas by intVarBit("varbit.pirates_treasure_bananas")

    suspend fun Dialogue.frank(): Boolean {
        val quest = Quest.get(QUEST) ?: return false
        return when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> {
                start()
                true
            }
            QuestProgressState.FINISHED -> false
            QuestProgressState.IN_PROGRESS -> {
                when (quest.getQuestStage(player)) {
                    STARTED -> rum()
                    KEY -> replaceKey()
                    else -> chatNpc(neutral, "Arr, you've got the message. Dig where it says, matey.")
                }
                true
            }
        }
    }

    private suspend fun Dialogue.start() {
        val quest = Quest.get(QUEST) ?: return
        chatNpc(laugh, "Arr, Matey!")
        if (!choice2("I'm in search of treasure.", true, "Arr!", false)) {
            chatPlayer(laugh, "Arr!")
            chatNpc(laugh, "Arr!")
            return
        }
        chatPlayer(neutral, "I'm in search of treasure.")
        chatNpc(quiz, "Arr, treasure you be after, eh? Well, I might be able to help, but I need a bottle of rum first, a good Karamjan rum.")
        chatNpc(neutral, "Bring me one and I'll tell you what I know. But be careful, the customs officer on Karamja takes a dim view of smuggling rum.")
        quest.advanceQuestStage(access, STARTED)
    }

    private suspend fun Dialogue.rum() {
        val quest = Quest.get(QUEST) ?: return
        chatNpc(quiz, "Arr, have ye brought me some Karamjan rum?")
        if (access.inv.count(RUM) == 0) {
            chatPlayer(neutral, "Not yet.")
            chatNpc(neutral, "Hmph. Smuggle a bottle past the customs man and come back.")
            return
        }
        chatPlayer(happy, "Yes, I have a bottle.")
        access.invDel(access.inv, RUM, 1)
        chatNpc(happy, "Now a deal's a deal. I'll tell ye about One-Eyed Hector's treasure. His chest is in his old room at the Blue Moon Inn in Varrock. Here's his key.")
        access.invAdd(access.inv, CHEST_KEY, 1)
        quest.advanceQuestStage(access, KEY - STARTED)
    }

    private suspend fun Dialogue.replaceKey() {
        if (access.inv.count(CHEST_KEY) > 0) {
            chatNpc(neutral, "Ye've got the key. Get to the Blue Moon Inn and open Hector's chest.")
            return
        }
        chatPlayer(sad, "I've lost the chest key.")
        chatNpc(neutral, "Arr, ye be careless. Here's another.")
        access.invAdd(access.inv, CHEST_KEY, 1)
    }

    fun ProtectedAccess.searchCrate(): Boolean {
        val quest = Quest.get(QUEST) ?: return false
        if (quest.getQuestStage(player) != STARTED || player.rumShipped == 0) return false
        if (player.worn.count(APRON) == 0) {
            mes("You can't rummage around Wydin's store room without an apron.")
            return true
        }
        invAdd(inv, RUM, 1)
        player.rumShipped = 0
        player.rumHidden = 0
        player.bananas = 0
        mes("You search the crate and find the bottle of rum among the bananas.")
        return true
    }

    suspend fun ProtectedAccess.confiscateRum(): Boolean {
        if (inv.count(RUM) == 0) return false
        invDel(inv, RUM, inv.count(RUM))
        mesbox("The customs officer confiscates your rum.")
        return true
    }
}
