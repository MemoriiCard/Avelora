package org.rsmod.content.quest.area.varrock

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.quest.area.varrock.DemonSlayer.Companion.BONES
import org.rsmod.content.quest.area.varrock.DemonSlayer.Companion.KEYS
import org.rsmod.content.quest.area.varrock.DemonSlayer.Companion.PRYSIN
import org.rsmod.content.quest.area.varrock.DemonSlayer.Companion.QUEST
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.game.entity.Player

object DemonSlayerTraiborn {
    private const val BONES_NEEDED = 25

    private var Player.bonesGiven by intVarBit("varbit.demon_slayer_bones")
    private var Player.hasSilverlight by intVarBit("varbit.demon_slayer_silverlight")

    suspend fun Dialogue.talk(): Boolean {
        val quest = Quest.get(QUEST) ?: return false
        if (quest.questState(player) != QuestProgressState.IN_PROGRESS) return false
        if (quest.getQuestStage(player) != PRYSIN || player.hasSilverlight != 0) return false
        val held = access.inv.count(BONES)
        if (player.bonesGiven < BONES_NEEDED && held > 0) {
            val give = minOf(held, BONES_NEEDED - player.bonesGiven)
            access.invDel(access.inv, BONES, give)
            player.bonesGiven += give
            val left = BONES_NEEDED - player.bonesGiven
            chatNpc(happy, if (left == 0) "Thank you, thingummywut! That is all I need." else "Thank you, thingummywut! $left more bones to go.")
        } else if (player.bonesGiven < BONES_NEEDED) {
            chatPlayer(quiz, "Do you have a key to the Silverlight case?")
            chatNpc(confused, "Oh the key! I'll give it to you for $BONES_NEEDED bones. Bring them and use them on me.")
            return true
        }
        if (player.bonesGiven >= BONES_NEEDED && access.inv.count(KEYS[2]) == 0) {
            chatNpc(happy, "Here is the key, thingummywut. Don't lose it.")
            access.invAdd(access.inv, KEYS[2], 1)
        }
        return true
    }
}
