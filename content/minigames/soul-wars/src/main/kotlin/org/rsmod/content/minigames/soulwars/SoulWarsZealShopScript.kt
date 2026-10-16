package org.rsmod.content.minigames.soulwars

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SoulWarsZealShopScript
@Inject
constructor(
    private val service: SoulWarsService,
    private val objs: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (nomad in listOf("npc.soul_wars_nomad_1op", "npc.soul_wars_nomad_2op", "npc.soul_wars_nomad")) {
            onOpNpc1(nomad) { talk(it.npc) }
        }
        onOpNpc2("npc.soul_wars_nomad_2op") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "You have ${service.zeal(player)} Zeal. What would you like to spend it on?")
            when (
                choice4(
                    "Combat experience",
                    Menu.Experience,
                    "Food and potions",
                    Menu.Supplies,
                    "Spell sacks",
                    Menu.Sacks,
                    "Special items",
                    Menu.Special,
                )
            ) {
                Menu.Experience -> experience()
                Menu.Supplies -> stock(ZealShop.SUPPLIES)
                Menu.Sacks -> stock(ZealShop.SACKS)
                Menu.Special -> stock(ZealShop.SPECIAL)
            }
        }
    }

    private suspend fun Dialogue.experience() {
        val skill =
            choice5(
                "Attack",
                ZealSkill.Attack,
                "Strength",
                ZealSkill.Strength,
                "Defence",
                ZealSkill.Defence,
                "Hitpoints",
                ZealSkill.Hitpoints,
                "Ranged, Magic or Prayer",
                null,
            ) ?: choice3("Ranged", ZealSkill.Ranged, "Magic", ZealSkill.Magic, "Prayer", ZealSkill.Prayer)
        val level = player.statBase(skill.stat)
        val perZeal = ZealShop.xpPerZeal(skill, level)
        if (perZeal == 0) {
            chatNpc(neutral, "You need level ${ZealShop.MIN_LEVEL} ${skill.label} first.")
            return
        }
        val amount = access.countDialog("Zeal to spend ($perZeal ${skill.label} XP each)")
        if (amount <= 0 || !service.spendZeal(player, amount)) {
            chatNpc(neutral, "You don't have that much Zeal.")
            return
        }
        access.statAdvance(skill.stat, (perZeal.toLong() * amount).toDouble(), rate = 1.0, globalRate = 1.0)
        player.mes("You spend $amount Zeal for ${perZeal.toLong() * amount} ${skill.label} experience.")
    }

    private suspend fun Dialogue.stock(list: List<ZealStock>) {
        val picked =
            when (list.size) {
                3 -> choice3(list[0].label, list[0], list[1].label, list[1], list[2].label, list[2])
                else ->
                    choice4(
                        list[0].label,
                        list[0],
                        list[1].label,
                        list[1],
                        list[2].label,
                        list[2],
                        list[3].label,
                        list[3],
                    )
            }
        val lots = if (picked.quantity > 1) access.countDialog("How many lots of ${picked.quantity}?") else 1
        val cost = picked.cost.toLong() * lots
        if (lots <= 0 || cost > Int.MAX_VALUE || !service.spendZeal(player, cost.toInt())) {
            chatNpc(neutral, "You don't have enough Zeal for that.")
            return
        }
        player.invAddOrDrop(objs, picked.obj, picked.quantity * lots)
        player.mes("You spend $cost Zeal.")
    }

    private enum class Menu {
        Experience,
        Supplies,
        Sacks,
        Special,
    }
}
