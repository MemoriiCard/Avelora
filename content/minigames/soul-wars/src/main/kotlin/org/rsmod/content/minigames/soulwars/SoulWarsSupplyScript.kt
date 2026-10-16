package org.rsmod.content.minigames.soulwars

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SoulWarsSupplyScript
@Inject
constructor(
    private val service: SoulWarsService,
    private val objs: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((counter, item) in COUNTERS) {
            onOpLoc1(counter) { take(item, countDialog("How many would you like to take?")) }
            onOpLoc2(counter) { take(item, 1) }
            onOpLoc3(counter) { take(item, 5) }
            onOpLoc4(counter) { take(item, 10) }
        }
        onOpHeld1(SoulWarsItems.BONES) { if (service.buryBones(player)) invDel(inv, SoulWarsItems.BONES) }
        onOpHeld1(SoulWarsItems.BANDAGES) { bandage(it.slot) }
        for (dose in 1..SoulWarsSupplies.POTION_DOSES) {
            onOpHeld1(SoulWarsSupplies.potion(dose)) { drink(it.slot, dose) }
        }
        onOpHeld3("obj.soul_cape_blue") { toggleCape("obj.soul_cape_blue", "obj.soul_cape_red") }
        onOpHeld3("obj.soul_cape_red") { toggleCape("obj.soul_cape_red", "obj.soul_cape_blue") }
    }

    private fun ProtectedAccess.take(item: String, requested: Int) {
        if (!service.isPlaying(player)) {
            mes("You can only take supplies during a game.")
            return
        }
        val allowed = (SoulWarsSupplies.CARRY_LIMIT - inv.count(item)).coerceAtMost(requested)
        if (allowed <= 0) {
            mes("You can't carry any more of those.")
            return
        }
        player.invAddOrDrop(objs, item, allowed)
    }

    private fun ProtectedAccess.bandage(slot: Int) {
        if (!service.isPlaying(player)) {
            mes("You can only use bandages during a game.")
            return
        }
        if (invDel(inv, SoulWarsItems.BANDAGES, 1, slot = slot).failure) return
        player.statHeal("stat.hitpoints", SoulWarsSupplies.BANDAGE_HEAL, SoulWarsSupplies.BANDAGE_PERCENT)
        mes("You bandage your wounds.")
    }

    private fun ProtectedAccess.drink(slot: Int, dose: Int) {
        if (!service.isPlaying(player)) {
            mes("You can only drink that during a game.")
            return
        }
        if (invDel(inv, SoulWarsSupplies.potion(dose), 1, slot = slot).failure) return
        if (dose > 1) invAdd(inv, SoulWarsSupplies.potion(dose - 1))
        for (stat in SoulWarsSupplies.BOOSTED) {
            player.statBoost(stat, SoulWarsSupplies.BOOST_CONSTANT, SoulWarsSupplies.BOOST_PERCENT)
        }
        mes("You drink some of the potion of power.")
    }

    private fun ProtectedAccess.toggleCape(from: String, to: String) {
        if (invDel(inv, from).failure) return
        invAdd(inv, to)
    }

    private companion object {
        val COUNTERS =
            buildList {
                for (side in listOf("blue", "red", "neutral")) {
                    add("loc.soul_wars_counter_bandages_$side" to SoulWarsItems.BANDAGES)
                    add("loc.soul_wars_counter_buff_potions_$side" to SoulWarsItems.POWER_POTION)
                }
                for (side in listOf("blue", "red")) {
                    add("loc.soul_wars_counter_potions_$side" to SoulWarsItems.EXPLOSIVE_POTION)
                }
                add("loc.soul_wars_counter_barricades_blue" to SoulWarsItems.barricade(SwTeam.Blue))
                add("loc.soul_wars_counter_barricades_red" to SoulWarsItems.barricade(SwTeam.Red))
            }
    }
}

internal object SoulWarsSupplies {
    const val CARRY_LIMIT = 10
    const val POTION_DOSES = 4
    const val BANDAGE_HEAL = 10
    const val BANDAGE_PERCENT = 10
    const val BOOST_CONSTANT = 5
    const val BOOST_PERCENT = 15
    val BOOSTED =
        listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")

    fun potion(dose: Int): String = "obj.soul_wars_buff_potion_$dose"
}
