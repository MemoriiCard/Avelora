package org.rsmod.content.quest.area.portsarim

import jakarta.inject.Inject
import org.rsmod.api.dig.DigSites
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class PiratesTreasure @Inject constructor(private val digSites: DigSites) : QuestScript(
    QUEST,
    "varp.hunt",
    rewards { item(CASKET) },
    ItemRewardDisplay(CASKET, zoom = 400),
    questVarbit = "varbit.pirates_treasure_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Pirate's Treasure end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.luthas") { startDialogue(it.npc) { luthas() } }
        onOpLoc1("loc.bananacrate") { searchBananaCrate() }
        onOpLoc2("loc.bananacrate") { fillBananaCrate() }
        onOpLocU("loc.bananacrate", RUM) { hideRum() }
        onOpLoc1("loc.piratechest") { openChest() }
        onOpHeld1(CASKET) { openCasket() }
        digSites.register(listOf(DIG_SITE)) { dig() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Redbeard Frank</col> in the <col=800000>Rusty Anchor</col> in <col=800000>Port Sarim</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Redbeard Frank wants a bottle of Karamjan rum, and in return he will tell me where One-Eyed Hector's treasure is.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I need to smuggle <red>Karamjan rum</red> from <red>Karamja</red> to <red>Redbeard Frank</red>. It can be hidden in <red>Luthas's banana crate</red>, and collected from <red>Wydin's</red> store room in Port Sarim.") {
                hasItem("karamja_rum", "I have the rum.")
            }
            KEY -> objective("Frank gave me <red>Hector's chest key</red>. The chest is in <red>Hector's old room</red> at the <red>Blue Moon Inn</red> in Varrock.") {
                hasItem("chest_key", "I have the chest key.")
            }
            else -> objective("The chest held a <red>pirate message</red>. I should dig where it says in <red>Falador Park</red>.") {
                hasItem("spade", "I have a spade.")
            }
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I smuggled rum to Redbeard Frank and dug up One-Eyed Hector's treasure in Falador Park.")
    }

    private var Player.bananas by intVarBit("varbit.pirates_treasure_bananas")
    private var Player.rumHidden by intVarBit("varbit.pirates_treasure_rum_hidden")
    private var Player.rumShipped by intVarBit("varbit.pirates_treasure_rum_shipped")
    private var Player.hired by intVarBit("varbit.pirates_treasure_hired")

    private suspend fun Dialogue.luthas() {
        chatNpc(neutral, "Hello. I'm in charge of the banana plantation here. Are you looking for work?")
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS || quest.getQuestStage(player) != STARTED) {
            chatPlayer(neutral, "Not right now, thanks.")
            return
        }
        if (!choice2("Yes, I'd like a job.", true, "No thanks.", false)) {
            chatPlayer(neutral, "No thanks.")
            return
        }
        chatPlayer(happy, "Yes, I'd like a job.")
        player.hired = 1
        chatNpc(happy, "Excellent. Fill a crate with ten bananas and I'll pay you $PAYMENT coins for it. I'll ship it to Wydin in Port Sarim.")
    }

    private fun ProtectedAccess.searchBananaCrate() {
        if (player.bananas == 0) {
            mes("The crate is empty.")
        } else if (player.bananas >= CRATE_SIZE) {
            mes("The crate is full of bananas.")
        } else {
            mes("The crate contains ${player.bananas} bananas.")
        }
    }

    private fun ProtectedAccess.fillBananaCrate() {
        val room = CRATE_SIZE - player.bananas
        if (room <= 0) {
            mes("The crate is already full.")
            return
        }
        val carrying = inv.count(BANANA)
        if (carrying == 0) {
            mes("You need some bananas to fill the crate.")
            return
        }
        val load = minOf(room, carrying)
        invDel(inv, BANANA, load)
        player.bananas = player.bananas + load
        mes("You pack $load bananas into the crate.")
        if (player.bananas < CRATE_SIZE) return
        if (player.hired == 1) {
            invAdd(inv, COINS, PAYMENT)
            mes("Luthas pays you $PAYMENT coins for filling the crate.")
        }
        if (player.rumHidden == 1) {
            player.rumShipped = 1
            mes("Luthas has the crate shipped to Port Sarim.")
        }
    }

    private fun ProtectedAccess.hideRum() {
        if (quest.getQuestStage(player) != STARTED) {
            mes("You have no reason to hide that.")
            return
        }
        if (player.bananas >= CRATE_SIZE) {
            mes("The crate is already full of bananas. There's no room for the rum.")
            return
        }
        if (player.rumHidden == 1) {
            mes("You have already hidden a bottle of rum in the crate.")
            return
        }
        invDel(inv, RUM, 1)
        player.rumHidden = 1
        mes("You hide the rum in the bottom of the crate.")
    }

    private suspend fun ProtectedAccess.openChest() {
        if (quest.getQuestStage(player) < KEY || inv.count(CHEST_KEY) == 0) {
            mes("The chest is locked.")
            return
        }
        if (quest.getQuestStage(player) > KEY) {
            mes("The chest is empty.")
            return
        }
        invAdd(inv, MESSAGE, 1)
        mes("You unlock the chest with the key. Inside is a message.")
        quest.advanceQuestStage(this, MESSAGE_STAGE - KEY)
    }

    private fun ProtectedAccess.dig() {
        if (quest.getQuestStage(player) != MESSAGE_STAGE) {
            mes("You dig a hole in the ground... but find nothing.")
            return
        }
        mes("You dig a hole in the ground and find a little chest.")
        quest.advanceQuestStage(this, COMPLETE_STAGE - MESSAGE_STAGE)
    }

    private fun ProtectedAccess.openCasket() {
        if (invDel(inv, CASKET, 1).failure) return
        invAdd(inv, GOLD_RING, 1)
        invAdd(inv, EMERALD, 1)
        invAdd(inv, COINS, CASKET_COINS)
        mes("You open the casket and find a gold ring, an emerald and $CASKET_COINS coins.")
    }

    companion object {
        const val QUEST = "quest_piratestreasure"
        const val CASKET = "obj.pirate_casket"
        const val RUM = "obj.karamja_rum"
        const val BANANA = "obj.banana"
        const val COINS = "obj.coins"
        const val CHEST_KEY = "obj.chest_key"
        const val MESSAGE = "obj.piratemessage"
        const val GOLD_RING = "obj.gold_ring"
        const val EMERALD = "obj.emerald"
        const val APRON = "obj.white_apron"
        const val PAYMENT = 30
        const val CRATE_SIZE = 10
        const val CASKET_COINS = 450
        val DIG_SITE = CoordGrid(2999, 3383, 0)
        const val STARTED = 1
        const val KEY = 2
        const val MESSAGE_STAGE = 3
        const val COMPLETE_STAGE = 4
    }
}
