package org.rsmod.content.quest.area.alkharid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class PrinceAliRescue : QuestScript(
    QUEST,
    "varp.princequest",
    rewards { item("obj.coins", COINS) },
    ItemRewardDisplay("obj.coins", zoom = 400),
    questVarbit = "varbit.prince_ali_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Prince Ali Rescue end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        onOpNpc1("npc.hassan") { startDialogue(it.npc) { hassan() } }
        onOpNpc1("npc.osman") { startDialogue(it.npc) { osman() } }
        onOpNpc1("npc.joe") { startDialogue(it.npc) { joe() } }
        onOpNpc1("npc.lady_keli") { startDialogue(it.npc) { keli() } }
        onOpNpc1("npc.prince_ali_prison") { startDialogue(it.npc) { prince() } }
        onOpNpcU(npcType("npc.joe"), itemType(BEER)) { startDialogue(it.npc) { giveBeer() } }
        onOpNpcU(npcType("npc.lady_keli"), itemType(ROPE)) { startDialogue(it.npc) { tieKeli() } }
        onOpNpcU(npcType("npc.ned"), itemType(WOOL)) { startDialogue(it.npc) { nedWig() } }
        onOpNpcU(npcType("npc.aggie"), itemType(ASHES)) { startDialogue(it.npc) { aggiePaste() } }
        onOpNpcU(npcType("npc.aggie"), itemType(FLOUR)) { startDialogue(it.npc) { aggiePaste() } }
        onOpNpcU(npcType("npc.aggie"), itemType(WATER)) { startDialogue(it.npc) { aggiePaste() } }
        onOpNpcU(npcType("npc.aggie"), itemType(REDBERRIES)) { startDialogue(it.npc) { aggiePaste() } }
        onOpHeldU(YELLOW_DYE, PLAIN_WIG) { dyeWig() }
        onOpLoc1("loc.alidoor") { passDoor(it.loc) }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Chancellor Hassan</col> in <col=800000>Al Kharid palace</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Chancellor Hassan wants Prince Ali freed from Lady Keli's bandits.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I should find <red>Osman</red>, the spymaster, outside the palace.") {}
            OSMAN -> objective("I need to give <red>Osman</red> a <red>key imprint</red> from <red>Lady Keli</red> and a <red>bronze bar</red>.") {
                hasItem("keyprint", "I have a key imprint.")
                hasItem("bronze_bar", "I have a bronze bar.")
            }
            KEY_MADE -> objective("I need the key from <red>Leela</red>, a disguise (wig, skin paste, pink skirt), to tie up <red>Keli</red> and to get <red>Joe</red> drunk.") {
                hasItem("princeskey", "I have the key.")
                hasItem("blondwig", "I have a blonde wig.")
                hasItem("skinpaste", "I have the skin paste.")
                hasItem("pink_skirt", "I have a pink skirt.")
            }
            else -> objective("Prince Ali is free. I should tell <red>Chancellor Hassan</red>.") {}
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I freed Prince Ali from Lady Keli's jail with a disguise and a copied key.")
    }

    private var Player.keyImprint by intVarBit("varbit.prince_ali_imprint")
    var Player.beers by intVarBit("varbit.prince_ali_beers")
    var Player.keliTied by intVarBit("varbit.prince_ali_keli_tied")

    private suspend fun Dialogue.hassan() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> hassanStart()
            QuestProgressState.FINISHED ->
                chatNpc(happy, "Greetings, friend. The Emir and I are grateful for your help with the Prince.")
            QuestProgressState.IN_PROGRESS ->
                when (quest.getQuestStage(player)) {
                    STARTED -> {
                        chatNpc(quiz, "Have you found Osman yet? He is waiting for you outside the palace.")
                    }
                    FREED -> hassanFinish()
                    else -> chatNpc(neutral, "Keep going. Prince Ali is still in the hands of the bandits.")
                }
        }
    }

    private suspend fun Dialogue.hassanStart() {
        chatNpc(neutral, "Greetings. Prince Ali has been captured by the evil Lady Keli and her bandits. Can you help us?")
        when (
            choice2(
                "Yes, I'll help rescue the Prince.",
                1,
                "No, I don't have time for that.",
                2,
                title = "Start the Prince Ali Rescue quest?",
            )
        ) {
            1 -> {
                chatPlayer(happy, "Yes, I'll help rescue the Prince.")
                chatNpc(happy, "Good. You must speak to Osman, our spymaster, who is waiting outside the palace.")
                quest.advanceQuestStage(access, STARTED)
            }
            2 -> chatPlayer(neutral, "No, I don't have time for that.")
        }
    }

    private suspend fun Dialogue.hassanFinish() {
        chatPlayer(happy, "The Prince is safe!")
        chatNpc(happy, "The Emir is very grateful. Please accept this payment for your efforts.")
        player.keyImprint = 0
        player.beers = 0
        player.keliTied = 0
        quest.advanceQuestStage(access, COMPLETE_STAGE - FREED)
    }

    private suspend fun Dialogue.osman() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED ->
                chatNpc(neutral, "I cannot discuss palace business with strangers.")
            QuestProgressState.FINISHED ->
                chatNpc(happy, "The Prince is safe, and Hassan has your payment. Well done.")
            QuestProgressState.IN_PROGRESS ->
                when (quest.getQuestStage(player)) {
                    STARTED -> osmanBriefing()
                    OSMAN -> osmanKey()
                    FREED -> chatNpc(happy, "You have saved the Prince. Hassan will pay you.")
                    else -> chatNpc(neutral, "Leela will give you the key. Get the disguise and deal with Keli and Joe.")
                }
        }
    }

    private suspend fun Dialogue.osmanBriefing() {
        chatPlayer(quiz, "Hassan sent me. What must I do?")
        chatNpc(neutral, "Prince Ali is being held by Lady Keli and her bandits in the abandoned jail east of Draynor Village.")
        chatNpc(neutral, "We must disguise the Prince as Keli, and Keli herself must be dealt with first, without bloodshed.")
        chatNpc(neutral, "Keli alone has the key to his cell. Bring me an imprint of it in soft clay and a bronze bar, and I will have a copy made for Leela.")
        quest.advanceQuestStage(access, OSMAN - STARTED)
    }

    private suspend fun Dialogue.osmanKey() {
        if (access.inv.count(KEY_PRINT) == 0 || access.inv.count(BRONZE_BAR) == 0) {
            chatNpc(quiz, "Bring me an imprint of the key in soft clay and a bronze bar, and I will have a copy made.")
            return
        }
        chatPlayer(happy, "I have the key imprint and a bronze bar.")
        access.invDel(access.inv, KEY_PRINT)
        access.invDel(access.inv, BRONZE_BAR)
        chatNpc(happy, "Excellent. I will have the key made and sent to Leela, who waits near Draynor Village.")
        quest.advanceQuestStage(access, KEY_MADE - OSMAN)
    }

    private suspend fun Dialogue.joe() {
        if (quest.getQuestStage(player) < OSMAN) {
            chatNpc(angry, "I'm not allowed to talk about my duties. Now clear off.")
            return
        }
        if (player.beers >= BEERS_NEEDED) {
            chatNpc(happy, "Hic... go in peace, friend.")
            return
        }
        chatNpc(angry, "I'm busy guarding the prisoner.")
        if (access.inv.count(BEER) == 0) {
            chatPlayer(quiz, "Perhaps something to drink might loosen your tongue.")
            return
        }
        giveBeer()
    }

    private suspend fun Dialogue.giveBeer() {
        if (player.beers >= BEERS_NEEDED) {
            chatNpc(happy, "Hic... go in peace, friend.")
            return
        }
        if (access.invDel(access.inv, BEER, 1).failure) return
        player.beers = player.beers + 1
        if (player.beers < BEERS_NEEDED) {
            chatNpc(happy, "Cheers, that's a good beer. I could murder another.")
        } else {
            chatNpc(happy, "Hic... that's the last one. I feel rather sleepy.")
        }
    }

    private suspend fun Dialogue.keli() {
        if (quest.getQuestStage(player) < OSMAN || player.keliTied == 1) {
            chatNpc(angry, "What do you want? Get out of my sight!")
            return
        }
        chatNpc(angry, "What do you want? I'm busy!")
        chatPlayer(quiz, "I want to join your gang of bandits.")
        chatNpc(neutral, "Prove yourself first. Prince Ali is worth a great deal, and the cell key is the only one there is. I keep it close.")
        if (player.keyImprint == 1 || access.inv.count(KEY_PRINT) > 0 || access.inv.count(SOFT_CLAY) == 0) {
            return
        }
        chatPlayer(quiz, "Could I touch the key for a moment?")
        chatNpc(neutral, "Fine. Look but don't be clumsy.")
        access.invDel(access.inv, SOFT_CLAY)
        access.invAdd(access.inv, KEY_PRINT, 1)
        player.keyImprint = 1
        mesbox("You press the soft clay against the key on Keli's chain and take an imprint.")
    }

    private suspend fun Dialogue.tieKeli() {
        if (quest.getQuestStage(player) < KEY_MADE) {
            chatPlayer(quiz, "I should have a plan before trying that.")
            return
        }
        if (player.keliTied == 1) {
            chatPlayer(neutral, "She's already tied up.")
            return
        }
        access.invDel(access.inv, ROPE)
        player.keliTied = 1
        mesbox("You overpower Lady Keli, tie her up with the rope and place her in a cupboard.")
    }

    private suspend fun Dialogue.prince() {
        if (quest.getQuestStage(player) != KEY_MADE) {
            chatNpc(sad, "I can't leave until someone frees me.")
            return
        }
        val inv = access.inv
        val ready =
            player.keliTied == 1 &&
                inv.count(KEY) > 0 &&
                inv.count(WIG) > 0 &&
                inv.count(SKIN_PASTE) > 0 &&
                inv.count(PINK_SKIRT) > 0
        if (!ready) {
            chatNpc(sad, "You must bring me the key and a full disguise: a blonde wig, skin paste and a pink skirt.")
            return
        }
        chatPlayer(happy, "I've come to rescue you. Put this disguise on.")
        for (item in listOf(KEY, WIG, SKIN_PASTE, PINK_SKIRT)) access.invDel(inv, item)
        chatNpc(happy, "Thank you, my friend. I shall return home in this disguise. My father will be most grateful.")
        quest.advanceQuestStage(access, FREED - KEY_MADE)
    }

    private suspend fun Dialogue.nedWig() {
        if (quest.getQuestStage(player) < OSMAN || access.inv.count(WOOL) < WOOL_FOR_WIG) {
            chatNpc(neutral, "I could make you a wig, if you bring me three balls of wool.")
            return
        }
        chatPlayer(quiz, "Could you make me a wig out of wool?")
        chatNpc(happy, "Certainly, three balls of wool will do.")
        access.invDel(access.inv, WOOL, WOOL_FOR_WIG)
        access.invAdd(access.inv, PLAIN_WIG, 1)
        mesbox("Ned weaves the wool into a wig. Dye it yellow to make it blonde.")
    }

    private suspend fun Dialogue.aggiePaste() {
        val inv = access.inv
        if (quest.getQuestStage(player) < OSMAN) {
            chatNpc(neutral, "I don't know what you're asking me.")
            return
        }
        if (listOf(ASHES, FLOUR, WATER, REDBERRIES).any { inv.count(it) == 0 }) {
            chatNpc(neutral, "For skin paste I need ashes, flour, water and redberries. Bring all four.")
            return
        }
        for (item in listOf(ASHES, FLOUR, WATER, REDBERRIES)) access.invDel(inv, item)
        access.invAdd(inv, SKIN_PASTE, 1)
        mesbox("Aggie mixes the ingredients in her cauldron and hands you a skin paste.")
    }

    private suspend fun ProtectedAccess.dyeWig() {
        if (invDel(inv, YELLOW_DYE, 1).failure) return
        invDel(inv, PLAIN_WIG, 1)
        invAdd(inv, WIG, 1)
        mes("You dye the wig yellow.")
    }

    private suspend fun ProtectedAccess.passDoor(door: BoundLocInfo) {
        val doorCoords = door.coords
        val inside = doorCoords
        val outside = when (door.angle.id) {
            0 -> CoordGrid(doorCoords.x - 1, doorCoords.z, doorCoords.level)
            1 -> CoordGrid(doorCoords.x, doorCoords.z + 1, doorCoords.level)
            2 -> CoordGrid(doorCoords.x + 1, doorCoords.z, doorCoords.level)
            else -> CoordGrid(doorCoords.x, doorCoords.z - 1, doorCoords.level)
        }
        if (coords == inside) {
            teleport(outside)
            return
        }
        when {
            player.beers < BEERS_NEEDED -> mes("The guard stops you from opening the door.")
            inv.count(KEY) == 0 -> mes("The door is locked.")
            else -> {
                mes("You unlock the door with the bronze key.")
                teleport(inside)
            }
        }
    }

    private fun npcType(name: String) = checkNotNull(ServerCacheManager.getNpc(name.asRSCM()))

    private fun itemType(name: String) = checkNotNull(ServerCacheManager.getItem(name.asRSCM()))

    companion object {
        const val QUEST = "quest_princealirescue"
        const val KEY = "obj.princeskey"
        const val COINS = 700
        const val BEERS_NEEDED = 3
        const val WOOL_FOR_WIG = 3
        const val BEER = "obj.beer"
        const val ROPE = "obj.rope"
        const val WOOL = "obj.ball_of_wool"
        const val ASHES = "obj.ashes"
        const val FLOUR = "obj.pot_flour"
        const val WATER = "obj.bucket_water"
        const val REDBERRIES = "obj.redberries"
        const val SOFT_CLAY = "obj.softclay"
        const val KEY_PRINT = "obj.keyprint"
        const val BRONZE_BAR = "obj.bronze_bar"
        const val PLAIN_WIG = "obj.plainwig"
        const val WIG = "obj.blondwig"
        const val YELLOW_DYE = "obj.yellowdye"
        const val SKIN_PASTE = "obj.skinpaste"
        const val PINK_SKIRT = "obj.pink_skirt"
        const val STARTED = 10
        const val OSMAN = 20
        const val KEY_MADE = 30
        const val FREED = 40
        const val COMPLETE_STAGE = 110
    }
}
