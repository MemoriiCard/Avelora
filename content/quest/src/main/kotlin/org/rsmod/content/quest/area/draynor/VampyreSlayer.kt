package org.rsmod.content.quest.area.draynor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.area.draynor.VampyreSlayerMorgan.countSlain
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class VampyreSlayer
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val playerList: PlayerList,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : QuestScript(
    QUEST,
    "varp.vampire",
    rewards { xp("stat.attack", ATTACK_XP) },
    ItemRewardDisplay(STAKE, zoom = 400),
    questVarbit = "varbit.vampyre_slayer_progress",
) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Vampyre Slayer end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the quest completes at $COMPLETE_STAGE."
        }
        val count = checkNotNull(ServerCacheManager.getNpc(COUNT.asRSCM()))
        val harlowType = checkNotNull(ServerCacheManager.getNpc(HARLOW.asRSCM()))
        val beer = checkNotNull(ServerCacheManager.getItem(BEER.asRSCM()))
        onOpNpc1("npc.dr_harlow") { startDialogue(it.npc) { harlow() } }
        onOpNpcU(harlowType, beer) { startDialogue(it.npc) { harlow() } }
        onOpLoc1("loc.garliccupboardshut") { searchCupboard() }
        onOpLoc1("loc.garliccupboardopen") { searchCupboard() }
        onOpLoc1("loc.vampcoffin") { openCoffin() }
        onModifyNpcHit(count) { protectCount() }
        onNpcHit(count) { recordKill() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Morgan</col> in his house in <col=800000>Draynor Village</col>."

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("Count Draynor terrorises Draynor Village from his manor, and Morgan wants him stopped.")
        when (quest.getQuestStage(player.player)) {
            STARTED -> objective("I should ask <red>Dr Harlow</red> in the <red>Blue Moon Inn</red> in Varrock for help. He will want a <red>beer</red>.") {
                hasItem("beer", "I have a beer.")
            }
            else -> objective("I have a <red>stake</red>. I should weaken the <red>Count</red> in the <red>basement of Draynor Manor</red>, then drive it in with a <red>hammer</red>. <red>Garlic</red> from Morgan's cupboard helps.") {
                hasItem("stake", "I have the stake.")
                hasItem("hammer", "I have a hammer.")
                hasItem("garlic", "I have garlic.")
            }
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I drove a stake through Count Draynor's heart and freed Draynor Village.")
    }

    private suspend fun Dialogue.harlow() {
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) {
            chatNpc(drunk, "Buy me a beer, will you?")
            return
        }
        when (quest.getQuestStage(player)) {
            STARTED -> {
                chatNpc(drunk, "Buy me a beer... then I'll help you with your vampyre problem.")
                if (access.invDel(access.inv, BEER, 1).failure) {
                    chatPlayer(neutral, "I'll see if I can find one.")
                    return
                }
                chatPlayer(happy, "Here's a beer.")
                chatNpc(drunk, "Now, buy ush a beer... Ahh, that's better. Takesh that stake to Draynor Manor and bring garlic. When hesh weak, use a hammer to drive the stake in!")
                if (access.invAdd(access.inv, STAKE, 1).failure) {
                    chatNpc(drunk, "Hmm, you've no room. Come back when you can carry it.")
                    return
                }
                quest.advanceQuestStage(access, STAKE_GIVEN - STARTED)
            }
            else -> {
                if (access.inv.count(STAKE) > 0) {
                    chatNpc(drunk, "Takesh that stake to Draynor Manor.")
                    return
                }
                chatPlayer(sad, "I've lost the stake.")
                chatNpc(drunk, "Hic. Here, I've a shpare.")
                access.invAdd(access.inv, STAKE, 1)
            }
        }
    }

    private fun ProtectedAccess.searchCupboard() {
        if (quest.getQuestStage(player) < STARTED || inv.count(GARLIC) > 0) {
            mes("You search the cupboard but find nothing interesting.")
            return
        }
        invAdd(inv, GARLIC, 1)
        mes("You find a clove of garlic in the cupboard and take it.")
    }

    private fun ProtectedAccess.openCoffin() {
        if (quest.getQuestStage(player) < STAKE_GIVEN) {
            mes("You open the coffin and find it empty.")
            return
        }
        val tile = CoordGrid(COFFIN.x, COFFIN.z - 1, COFFIN.level)
        val count = Npc(COUNT, tile)
        count.respawns = false
        npcRepo.add(count, COUNT_LIFETIME)
        count.opPlayer2(player, aiPlayerInteractions)
        mes("You open the coffin and Count Draynor rises from it!")
    }

    private fun org.rsmod.api.npc.events.NpcHitEvents.Modify.protectCount() {
        if (!hit.isFromPlayer) return
        if (npc.hitpoints - hit.damage > 0) return
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(playerList) }
        val staked = attacker != null && attacker.inv.count(STAKE) > 0 && attacker.inv.count(HAMMER) > 0
        if (!staked) hit.damage = maxOf(0, npc.hitpoints - 1)
    }

    private fun org.rsmod.api.npc.events.NpcHitEvents.Impact.recordKill() {
        if (npc.hitpoints > 0 || !hit.isFromPlayer) return
        val attacker = hit.resolvePlayerSource(playerList) ?: return
        attacker.countSlain = 1
        attacker.mes("You drive the stake into the vampyre's heart with your hammer. The Count is dead!")
    }

    companion object {
        const val QUEST = "quest_vampyreslayer"
        const val COUNT = "npc.count_draynor"
        const val HARLOW = "npc.dr_harlow"
        const val BEER = "obj.beer"
        const val STAKE = "obj.stake"
        const val GARLIC = "obj.garlic"
        const val HAMMER = "obj.hammer"
        const val ATTACK_XP = 4825.0
        const val COUNT_LIFETIME = 500
        val COFFIN = CoordGrid(3077, 9775, 0)
        const val STARTED = 1
        const val STAKE_GIVEN = 2
        const val COMPLETE_STAGE = 3
    }
}
