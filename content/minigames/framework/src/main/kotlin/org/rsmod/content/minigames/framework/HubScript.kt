package org.rsmod.content.minigames.framework

import jakarta.inject.Inject
import java.time.LocalDate
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HubScript
@Inject
constructor(
    private val launcher: ProtectedAccessLauncher,
    private val objs: ObjRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("minigames") {
            desc = "Open the minigame board"
            cheat { launcher.launch(player) { board() } }
        }
        onCommand("minigamestore") {
            desc = "Spend Minigame Tokens"
            cheat { launcher.launch(player) { store() } }
        }
        onOpHeld1(HubCrates.CRATE) { openCrate() }
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private suspend fun ProtectedAccess.board() {
        val day = today()
        val bonus = BonusOfTheDay.gameFor(day)
        mes("Today's bonus minigame is ${MinigameBoard.ENTRIES.first { it.game == bonus }.label}.")
        val entries = MinigameBoard.ENTRIES
        val entry = pick(entries.map { MinigameBoard.label(it, day) }, entries, 0) ?: return
        telejump(entry.destination)
    }

    private suspend fun ProtectedAccess.store() {
        mes("You have ${MinigameProfile.tokens(player)} Minigame Tokens.")
        val choice = choice2("Supplies and crates", HubStore.SUPPLIES, "Cosmetics", HubStore.COSMETICS)
        val stock = pick(choice.map { "${it.label} (${it.cost} tokens)" }, choice, 0) ?: return
        if (!MinigameProfile.spendTokens(player, stock.cost)) {
            mes("You don't have enough Minigame Tokens for that.")
            return
        }
        player.invAddOrDrop(objs, stock.obj, stock.quantity)
        mes("You spend ${stock.cost} tokens.")
    }

    private suspend fun ProtectedAccess.openCrate() {
        if (invDel(inv, HubCrates.CRATE).failure) return
        val reward = HubCrates.roll { random.of(it) }
        if (reward.obj == null) {
            MinigameProfile.addTokens(player, reward.amount)
            mes("You find ${reward.amount} Minigame Tokens inside.")
        } else {
            player.invAddOrDrop(objs, reward.obj, reward.amount)
            mes("You find ${reward.amount} x ${reward.obj.substringAfter('.')} inside.")
        }
    }

    private suspend fun <T> ProtectedAccess.pick(labels: List<String>, items: List<T>, from: Int): T? {
        val remaining = items.size - from
        val shown = remaining.coerceAtMost(PAGE)
        val more = remaining > PAGE
        val page = (0 until shown).map { labels[from + it] to items[from + it] }
        val extra = if (more) "More..." to null else null
        val options = if (extra != null) page + extra else page
        val picked =
            when (options.size) {
                1 -> options[0].second
                2 -> choice2(options[0].first, options[0].second, options[1].first, options[1].second)
                3 ->
                    choice3(
                        options[0].first,
                        options[0].second,
                        options[1].first,
                        options[1].second,
                        options[2].first,
                        options[2].second,
                    )
                4 ->
                    choice4(
                        options[0].first,
                        options[0].second,
                        options[1].first,
                        options[1].second,
                        options[2].first,
                        options[2].second,
                        options[3].first,
                        options[3].second,
                    )
                else ->
                    choice5(
                        options[0].first,
                        options[0].second,
                        options[1].first,
                        options[1].second,
                        options[2].first,
                        options[2].second,
                        options[3].first,
                        options[3].second,
                        options[4].first,
                        options[4].second,
                    )
            }
        return picked ?: if (more) pick(labels, items, from + PAGE) else null
    }

    private companion object {
        const val PAGE = 4
    }
}
