package org.rsmod.content.minigames.lms

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc2
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LmsShopScript
@Inject
constructor(
    private val service: LmsService,
    private val objs: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (wizard in listOf("npc.br_wizard_casual", "npc.br_wizard_competitive", "npc.br_wizard_highstakes")) {
            onOpNpc2(wizard) { talk(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            val rating = service.rating(player)
            chatNpc(
                neutral,
                "You have ${service.points(player)} points, ${service.wins(player)} wins and a rating of " +
                    "$rating (${LmsCompetitive.title(rating)}).",
            )
            buy(
                choice3(
                    "Supplies",
                    LmsShop.SUPPLIES,
                    "Upgrades",
                    LmsShop.UPGRADES,
                    "Cosmetics",
                    LmsShop.COSMETICS,
                )
            )
        }
    }

    private suspend fun Dialogue.buy(list: List<LmsStock>) {
        val stock = pick(list, 0) ?: return
        if (!LmsShop.canBuy(stock, service.wins(player))) {
            chatNpc(neutral, "You need to win a game before you can buy that.")
            return
        }
        val lots = if (stock.quantity > 1) access.countDialog("How many lots of ${stock.quantity}?") else 1
        val cost = stock.cost.toLong() * lots
        if (lots <= 0 || cost > Int.MAX_VALUE || !service.spendPoints(player, cost.toInt())) {
            chatNpc(neutral, "You don't have enough points for that.")
            return
        }
        player.invAddOrDrop(objs, stock.obj, stock.quantity * lots)
        player.mes("You spend $cost points.")
    }

    private suspend fun Dialogue.pick(list: List<LmsStock>, from: Int): LmsStock? {
        val remaining = list.size - from
        val page = list.subList(from, from + remaining.coerceAtMost(PAGE))
        val more = remaining > PAGE
        val extra = if (more) "More..." else null
        return when (page.size + if (more) 1 else 0) {
            1 -> page[0]
            2 -> choice2(page[0].label, page[0], extra ?: page[1].label, if (more) null else page[1])
            3 -> choice3(page[0].label, page[0], page[1].label, page[1], extra ?: page[2].label, if (more) null else page[2])
            4 ->
                choice4(
                    page[0].label,
                    page[0],
                    page[1].label,
                    page[1],
                    page[2].label,
                    page[2],
                    extra ?: page[3].label,
                    if (more) null else page[3],
                )
            else ->
                choice5(
                    page[0].label,
                    page[0],
                    page[1].label,
                    page[1],
                    page[2].label,
                    page[2],
                    page[3].label,
                    page[3],
                    extra ?: page[4].label,
                    if (more) null else page[4],
                )
        } ?: pick(list, from + PAGE)
    }

    private companion object {
        const val PAGE = 4
    }
}
