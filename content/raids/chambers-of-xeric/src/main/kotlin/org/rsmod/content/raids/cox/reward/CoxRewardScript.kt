package org.rsmod.content.raids.cox.reward

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.raids.cox.party.coxBestTime
import org.rsmod.content.raids.cox.party.coxCmBestTime
import org.rsmod.content.raids.cox.party.coxCmKc
import org.rsmod.content.raids.cox.party.coxKc
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.rigourUnlocked by boolVarBit("varbit.prayer_rigour_unlocked")
private var Player.auguryUnlocked by boolVarBit("varbit.prayer_augury_unlocked")
private var Player.preserveUnlocked by boolVarBit("varbit.prayer_preserve_unlocked")

class CoxRewardScript
@Inject
constructor(
    private val raids: CoxRaids,
    private val delivery: CoxRewardDelivery,
    private val pets: PetRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CoxChest.CHEST) { openChest() }
        onOpHeld1("obj.raids_prayerscroll") { readScroll("Rigour") }
        onOpHeld1("obj.raids_prayerscroll_augury") { readScroll("Augury") }
        onOpHeld1("obj.raids_prayerscroll_preserve") { readScroll("Preserve") }
        onOpHeld1(CoxLoot.JOURNAL) { readJournal() }
    }

    private fun ProtectedAccess.openChest() {
        val raid = raids.containing(player)
        if (raid == null || raid.completedAt < 0) {
            mes("The chest is sealed.")
            return
        }
        val loot = raid.rewards.remove(player)
        if (loot == null) {
            mes("There is nothing left in the chest for you.")
            return
        }
        for (item in loot) {
            if (item.obj == CoxLoot.OLMLET) pets.give(player, item.obj) else delivery.give(player, item)
            val name = delivery.displayName(item)
            val amount = if (item.count > 1) "${item.count} x " else ""
            mes("You find: $amount$name.")
        }
    }

    private suspend fun ProtectedAccess.readScroll(prayer: String) {
        val known =
            when (prayer) {
                "Rigour" -> player.rigourUnlocked
                "Augury" -> player.auguryUnlocked
                else -> player.preserveUnlocked
            }
        if (known) {
            mes("You have already learnt this prayer.")
            return
        }
        if (menu("Learn $prayer? The scroll will be used up.", "Yes", "No") != 0) return
        val scroll =
            when (prayer) {
                "Rigour" -> "obj.raids_prayerscroll"
                "Augury" -> "obj.raids_prayerscroll_augury"
                else -> "obj.raids_prayerscroll_preserve"
            }
        if (invDel(inv, scroll).failure) return
        when (prayer) {
            "Rigour" -> player.rigourUnlocked = true
            "Augury" -> player.auguryUnlocked = true
            else -> player.preserveUnlocked = true
        }
        mes("You can now use the $prayer prayer.")
    }

    private fun ProtectedAccess.readJournal() {
        mes("Chambers of Xeric completions: <col=ef1020>${player.coxKc}</col>.")
        mes("Challenge Mode completions: <col=ef1020>${player.coxCmKc}</col>.")
        player.coxBestTime.takeIf { it > 0 }?.let { mes("Fastest raid: ${clock(it)}.") }
        player.coxCmBestTime.takeIf { it > 0 }?.let { mes("Fastest Challenge Mode raid: ${clock(it)}.") }
    }

    private fun clock(ticks: Int): String {
        val seconds = ticks * 6 / 10
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
