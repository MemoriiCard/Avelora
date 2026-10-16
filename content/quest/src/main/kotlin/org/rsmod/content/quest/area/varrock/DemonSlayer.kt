package org.rsmod.content.quest.area.varrock

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class DemonSlayer
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val playerList: PlayerList,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : QuestScript(
    QUEST,
    "varp.demonstart",
    rewards { item(SILVERLIGHT, 1) },
    ItemRewardDisplay(SILVERLIGHT, zoom = 400),
    questVarbit = "varbit.demon_slayer_progress",
) {
    var Player.bonesGiven by intVarBit("varbit.demon_slayer_bones")
    var Player.drainWet by intVarBit("varbit.demon_slayer_drain_wet")
    var Player.incantation by intVarBit("varbit.demon_slayer_incantation")
    var Player.weakened by intVarBit("varbit.demon_slayer_delrith_weakened")
    var Player.hasSilverlight by intVarBit("varbit.demon_slayer_silverlight")

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Demon Slayer end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        val traiborn = checkNotNull(ServerCacheManager.getNpc(TRAIBORN.asRSCM()))
        val bones = checkNotNull(ServerCacheManager.getItem(BONES.asRSCM()))
        val delrith = checkNotNull(ServerCacheManager.getNpc(DELRITH.asRSCM()))
        onOpNpc1(ARIS) { startDialogue(it.npc) { aris() } }
        onOpNpc1(PRYSIN_NPC) { startDialogue(it.npc) { prysin() } }
        onOpNpc1(ROVIN_NPC) { startDialogue(it.npc) { rovin() } }
        onOpNpcU(traiborn, bones) { startDialogue(it.npc) { with(DemonSlayerTraiborn) { talk() } } }
        onOpLocU(DRAIN, "obj.bucket_water") { pourWater() }
        onOpLoc1(KEY_PIPE) { searchPipe() }
        onPlayerCoordsChanged { enterCircle(player) }
        onModifyNpcHit(delrith) { protectDelrith() }
        onOpNpc1(DELRITH_WEAKENED) { startDialogue(it.npc) { banish(it.npc) } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Aris</col> in the tent in <col=800000>Varrock Square</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("The demon Delrith threatens Varrock and only the sword Silverlight can stop him.")
        val stage = quest.getQuestStage(player.player)
        when {
            stage == STARTED -> objective("I should speak to <red>Sir Prysin</red> in <red>Varrock Palace</red> about the sword <red>Silverlight</red>.") {}
            player.player.hasSilverlight == 0 -> objective("Sir Prysin needs the three <red>Silverlight keys</red>: his own from the <red>drain</red>, <red>Captain Rovin's</red>, and <red>Wizard Traiborn's</red> for 25 bones.") {
                hasItem("silverlight_key_1", "I have Sir Prysin's key.")
                hasItem("silverlight_key_2", "I have Captain Rovin's key.")
                hasItem("silverlight_key_3", "I have Wizard Traiborn's key.")
            }
            else -> objective("I have <red>Silverlight</red>. I should defeat <red>Delrith</red> at the <red>stone circle</red> south of Varrock, then banish him with my incantation.") {
                hasItem("silverlight", "I have Silverlight.")
            }
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I recovered Silverlight and banished the demon Delrith from Varrock.")
    }

    private suspend fun Dialogue.aris() {
        when (quest.questState(player)) {
            QuestProgressState.NOT_STARTED -> {
                chatNpc(neutral, "Greetings, adventurer. Shall I read your fortune? It will cost you one coin.")
                if (!choice2("Ok, here you go.", true, "No, I don't believe in that stuff.", false)) {
                    chatPlayer(neutral, "No, I don't believe in that stuff.")
                    return
                }
                if (access.invDel(access.inv, "obj.coins", 1).failure) {
                    chatPlayer(sad, "I don't have a coin on me.")
                    return
                }
                player.incantation = (Math.floorMod(player.uuid ?: 0L, INCANTATION_WORDS.size.toLong())).toInt() + 1
                chatNpc(worried, "Ooh, I see a terrible demon, Delrith, who will be summoned south of Varrock. Only the sword Silverlight can defeat him.")
                chatNpc(neutral, "To send him back you must speak these words: ${incantation(player)}.")
                quest.advanceQuestStage(access, STARTED)
            }
            QuestProgressState.IN_PROGRESS -> {
                chatNpc(neutral, "Remember the words: ${incantation(player)}. Find Sir Prysin in the palace for Silverlight.")
            }
            QuestProgressState.FINISHED -> chatNpc(happy, "Thank you for saving Varrock.")
        }
    }

    private suspend fun Dialogue.prysin() {
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) {
            chatNpc(neutral, "Hello. How can I help you?")
            return
        }
        when (quest.getQuestStage(player)) {
            STARTED -> {
                chatPlayer(neutral, "Aris says I need Silverlight to defeat Delrith.")
                chatNpc(worried, "The sword is locked in a case that needs three keys. Mine fell down the drain, Captain Rovin and Wizard Traiborn hold the others.")
                quest.advanceQuestStage(access, PRYSIN - STARTED)
            }
            else -> if (player.hasSilverlight == 0) {
                val keys = KEYS.all { access.inv.count(it) > 0 }
                if (!keys) {
                    chatNpc(neutral, "Bring me all three keys and the sword is yours.")
                    return
                }
                for (key in KEYS) access.invDel(access.inv, key, 1)
                chatPlayer(happy, "I have all three keys.")
                chatNpc(happy, "Excellent! Take Silverlight and be careful with it.")
                access.invAdd(access.inv, SILVERLIGHT, 1)
                player.hasSilverlight = 1
            } else {
                if (access.inv.count(SILVERLIGHT) + access.worn.count(SILVERLIGHT) > 0) {
                    chatNpc(neutral, "Go and use Silverlight on Delrith at the stone circle.")
                    return
                }
                chatPlayer(sad, "I've lost Silverlight.")
                chatNpc(neutral, "I can make you another for 500 coins.")
                if (access.invDel(access.inv, "obj.coins", 500).failure) return
                access.invAdd(access.inv, SILVERLIGHT, 1)
            }
        }
    }

    private suspend fun Dialogue.rovin() {
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS || quest.getQuestStage(player) < PRYSIN) {
            chatNpc(neutral, "Hello there. I guard the palace.")
            return
        }
        if (access.inv.count(KEYS[1]) > 0) {
            chatNpc(neutral, "You already have my key.")
            return
        }
        chatPlayer(neutral, "Sir Prysin asked me to collect your key to Silverlight.")
        chatNpc(neutral, "Here, take it, but keep it from Sir Prysin if he is in a murderous mood.")
        access.invAdd(access.inv, KEYS[1], 1)
    }

    private fun ProtectedAccess.pourWater() {
        if (quest.getQuestStage(player) < PRYSIN) {
            mes("Nothing interesting happens.")
            return
        }
        invDel(inv, "obj.bucket_water", 1)
        invAdd(inv, "obj.bucket_empty", 1)
        player.drainWet = 1
        mes("You pour the water down the drain.")
    }

    private fun ProtectedAccess.searchPipe() {
        if (quest.getQuestStage(player) < PRYSIN || player.drainWet == 0) {
            mes("You search the pipe but find nothing.")
            return
        }
        if (inv.count(KEYS[0]) > 0) {
            mes("You have already taken the key.")
            return
        }
        invAdd(inv, KEYS[0], 1)
        mes("You find a small rusty key.")
    }

    private fun enterCircle(player: Player) {
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) return
        if (quest.getQuestStage(player) != PRYSIN || player.hasSilverlight == 0) return
        if (!inCircle(player.coords)) return
        if (npcRepo.findAll(CENTRE).any { it.id == DELRITH.asRSCM() }) return
        spawn(DELRITH, CENTRE, player)
        for (wizard in WIZARDS) spawn(wizard.first, wizard.second, player)
        player.mes("The stone circle erupts as Delrith is summoned!")
    }

    private fun inCircle(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 3223..3233 && coords.z in 3363..3374

    private fun spawn(type: String, tile: CoordGrid, target: Player) {
        val npc = Npc(type, tile)
        npc.respawns = false
        npcRepo.add(npc, SPAWN_LIFETIME)
        npc.opPlayer2(target, aiPlayerInteractions)
    }

    private fun org.rsmod.api.npc.events.NpcHitEvents.Modify.protectDelrith() {
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(playerList) } ?: return
        if (attacker.worn.count(SILVERLIGHT) == 0) {
            hit.damage = 0
            return
        }
        if (hit.damage < npc.hitpoints) return
        hit.damage = 0
        attacker.weakened = 1
        val weakened = checkNotNull(ServerCacheManager.getNpc(DELRITH_WEAKENED.asRSCM()))
        npc.transmog(weakened, SPAWN_LIFETIME)
        attacker.mes("Silverlight pierces Delrith and he collapses, weakened.")
    }

    private suspend fun Dialogue.banish(npc: Npc) {
        if (quest.getQuestStage(player) != PRYSIN || player.weakened == 0) {
            chatNpc(angry, "Aaargh!")
            return
        }
        val right = player.incantation
        val picked = menu(
            *INCANTATION_WORDS.indices.map { incantation(it + 1) to (it + 1) }.toTypedArray(),
        )
        if (picked != right) {
            chatPlayer(worried, "Hm, that didn't work...")
            player.weakened = 0
            val delrith = checkNotNull(ServerCacheManager.getNpc(DELRITH.asRSCM()))
            npc.transmog(delrith, SPAWN_LIFETIME)
            npc.hitpoints = delrith.hitpoints
            return
        }
        npcRepo.del(npc, 1)
        chatNpc(angry, "Aaaaaaargh! Back to the abyss...")
        player.weakened = 0
        player.hasSilverlight = 0
        player.bonesGiven = 0
        player.drainWet = 0
        player.incantation = 0
        quest.advanceQuestStage(access, COMPLETE_STAGE - PRYSIN)
    }

    private fun incantation(player: Player): String = incantation(player.incantation)

    private fun incantation(variant: Int): String {
        val start = (variant - 1).coerceAtLeast(0)
        return INCANTATION_WORDS.indices.joinToString(" ") { INCANTATION_WORDS[(start + it) % INCANTATION_WORDS.size] }
    }

    companion object {
        const val QUEST = "quest_demonslayer"
        const val ARIS = "npc.aris"
        const val PRYSIN_NPC = "npc.sir_prysin"
        const val ROVIN_NPC = "npc.captain_rovin"
        const val TRAIBORN = "npc.traiborn"
        const val DELRITH = "npc.delrith"
        const val DELRITH_WEAKENED = "npc.delrith_weakened"
        const val DRAIN = "loc.questdrain"
        const val KEY_PIPE = "loc.qip_ds_keypipe"
        const val BONES = "obj.bones"
        const val SILVERLIGHT = "obj.silverlight"
        val KEYS = listOf("obj.silverlight_key_1", "obj.silverlight_key_2", "obj.silverlight_key_3")
        val INCANTATION_WORDS = listOf("Carlem", "Gabindo", "Purchai", "Zaree", "Camerinthum")
        val CENTRE = CoordGrid(3227, 3368, 0)
        val WIZARDS = listOf(
            "npc.qip_ds_young_dark_wizard1" to CoordGrid(3225, 3366, 0),
            "npc.qip_ds_young_dark_wizard2" to CoordGrid(3229, 3366, 0),
            "npc.qip_ds_young_dark_wizard3" to CoordGrid(3225, 3370, 0),
            "npc.qip_ds_young_dark_wizard4" to CoordGrid(3229, 3370, 0),
        )
        const val SPAWN_LIFETIME = 600
        const val STARTED = 1
        const val PRYSIN = 2
        const val COMPLETE_STAGE = 3
    }
}
