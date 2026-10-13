package org.rsmod.content.minigames.pestcontrol

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PcShopScript
@Inject
constructor(
    private val service: PcService,
    private val objs: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (knight in KNIGHTS) {
            onOpNpc1(knight) { talk(it.npc) }
            onOpNpc3(knight) { talk(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "You have ${service.points(player)} commendation points.")
            when (choice3("Void knight equipment", Menu.Void, "Elite void upgrade", Menu.Elite, "Experience", Menu.Xp)) {
                Menu.Void -> buyVoid()
                Menu.Elite -> upgrade()
                Menu.Xp -> exchange()
            }
        }
    }

    private suspend fun Dialogue.buyVoid() {
        if (!meetsRequirements(player)) {
            chatNpc(neutral, "You need 42 in Attack, Strength, Defence, Hitpoints, Ranged and Magic, and 22 Prayer.")
            return
        }
        val items = PcShop.VOID
        val stock =
            choice5(
                items[0].label,
                items[0],
                items[1].label,
                items[1],
                items[2].label,
                items[2],
                "Helms",
                null,
                "Void knight mace",
                items[6],
            ) ?: choice3(items[3].label, items[3], items[4].label, items[4], items[5].label, items[5])
        if (!service.spendPoints(player, stock.cost)) {
            chatNpc(neutral, "You don't have enough points for that.")
            return
        }
        player.invAddOrDrop(objs, stock.obj, 1)
        player.mes("You spend ${stock.cost} points.")
    }

    private suspend fun Dialogue.upgrade() {
        if (!meetsRequirements(player)) {
            chatNpc(neutral, "You need 42 in Attack, Strength, Defence, Hitpoints, Ranged and Magic, and 22 Prayer.")
            return
        }
        val choice = choice2(PcShop.ELITE[0].label, PcShop.ELITE[0], PcShop.ELITE[1].label, PcShop.ELITE[1])
        if (!player.inv.contains(choice.from)) {
            chatNpc(neutral, "Bring me the matching void piece first; I will upgrade it.")
            return
        }
        if (!service.spendPoints(player, choice.cost)) {
            chatNpc(neutral, "You don't have enough points for that.")
            return
        }
        access.invDel(player.inv, choice.from)
        player.invAddOrDrop(objs, choice.to, 1)
        player.mes("You spend ${choice.cost} points.")
    }

    private suspend fun Dialogue.exchange() {
        val skill =
            choice5(
                "Attack",
                PcSkill.Attack,
                "Strength",
                PcSkill.Strength,
                "Defence",
                PcSkill.Defence,
                "Hitpoints",
                PcSkill.Hitpoints,
                "Ranged, Magic or Prayer",
                null,
            ) ?: choice3("Ranged", PcSkill.Ranged, "Magic", PcSkill.Magic, "Prayer", PcSkill.Prayer)
        val level = player.statBase(skill.stat)
        if (PcShop.xpPerPoint(skill, level) == 0) {
            chatNpc(neutral, "You need level ${PcShop.MIN_EXCHANGE_LEVEL} ${skill.label} first.")
            return
        }
        val points = access.countDialog("Points to exchange")
        if (points <= 0 || !service.spendPoints(player, points)) {
            chatNpc(neutral, "You don't have that many points.")
            return
        }
        val xp = PcShop.experience(skill, level, points)
        access.statAdvance(skill.stat, xp.toDouble(), rate = 1.0, globalRate = 1.0)
        player.mes("You exchange $points points for $xp ${skill.label} experience.")
    }

    private fun meetsRequirements(player: Player): Boolean =
        listOf("stat.attack", "stat.strength", "stat.defence", "stat.hitpoints", "stat.ranged", "stat.magic")
            .all { player.statBase(it) >= PcShop.MIN_COMBAT_STAT } &&
            player.statBase("stat.prayer") >= PcShop.MIN_PRAYER

    private enum class Menu {
        Void,
        Elite,
        Xp,
    }

    companion object {
        val KNIGHTS =
            listOf(
                "npc.pest_voidknight_1",
                "npc.pest_voidknight_2",
                "npc.pest_voidknight_3",
                "npc.pest_voidknight_4",
            )
    }
}
