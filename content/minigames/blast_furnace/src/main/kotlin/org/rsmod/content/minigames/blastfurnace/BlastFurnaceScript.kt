package org.rsmod.content.minigames.blastfurnace

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.player.stat.strengthLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.smithing.coalbag.CoalBag
import org.rsmod.content.skills.smithing.util.SmithingBonuses.wearingIceSmithingGloves
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class BlastFurnaceScript
@Inject
internal constructor(
    private val service: BlastFurnaceService,
    private val xpMods: XpModifiers,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.dwarf_keldagrim_factory_stairs") { descend() }
        onOpLoc1("loc.blast_furnace_stairs_up") { ascend() }
        onOpNpc1("npc.blast_furnace_instructor") { foreman() }
        onOpLoc1("loc.blast_furnace_conveyer_belt_clickable") { putOreOnBelt() }
        for (stove in STOVES) onOpLoc1(stove) { refuel() }
        onOpLoc1("loc.blast_furnace_coke") { collectCoke() }
        onOpLoc1("loc.blast_furnace_pump") { pump() }
        onOpLoc1("loc.blast_furnace_pedals") { pedal() }
        onOpLoc1("loc.blast_furnace_gauge") { readGauge() }
        onOpLoc1("loc.blast_furnace_chimney") { checkFurnace() }
        onOpLoc1("loc.blast_furnace_sink") { fillBucket() }
        onOpLocU("loc.blast_furnace_sink", "obj.bucket_empty") { fillBucket() }
        onOpLoc1("loc.blast_furnace_ore_dispenser_empty") { checkBars() }
        onOpLoc1("loc.blast_furnace_ore_dispenser_full") { takeBars() }
        onOpLoc2("loc.blast_furnace_ore_dispenser_full") { checkBars() }
        onOpLocU("loc.blast_furnace_ore_dispenser_full", "obj.bucket_water") { coolBars() }
        onOpLoc1("loc.blast_furnace_ore_dispenser_cooled") { takeBars() }
        onOpLoc2("loc.blast_furnace_ore_dispenser_cooled") { checkBars() }
        onPlayerSoftTimer(BlastFurnaceService.TIMER) { service.tick(player) }
        onPlayerLogin { if (player.totalOres() > 0) service.startSmelting(player) }
    }

    private fun ProtectedAccess.descend() {
        PathingEntityCommon.telejump(player, collision, BASEMENT)
    }

    private fun ProtectedAccess.ascend() {
        PathingEntityCommon.telejump(player, collision, SURFACE)
    }

    private suspend fun ProtectedAccess.foreman() {
        startDialogue {
            when {
                player.baseSmithingLvl60() ->
                    chatNpc(
                        happy,
                        "You've the skill to use the furnace freely. Mind the temperature, now.",
                    )
                service.hasAccess(player) ->
                    chatNpc(
                        neutral,
                        "You're paid up for about ${service.minutesLeft(player)} more minutes.",
                    )
                else -> {
                    chatNpc(
                        neutral,
                        "Smiths under level 60 pay ${BlastFurnaceRules.FEE_COINS} coins for ten minutes.",
                    )
                    val pay =
                        choice2(
                            "Pay ${BlastFurnaceRules.FEE_COINS} coins.", true,
                            "Not now.", false,
                        )
                    if (pay) {
                        if (access.invDel(access.inv, "obj.coins", BlastFurnaceRules.FEE_COINS).success) {
                            service.pay(access.player)
                            chatNpc(happy, "Pleasure. Ten minutes, starting now.")
                        } else {
                            chatNpc(neutral, "You haven't the coins, I'm afraid.")
                        }
                    }
                }
            }
        }
    }

    private fun ProtectedAccess.canOperate(): Boolean {
        if (!service.hasAccess(player)) {
            mes("The foreman hasn't let you use the machinery. Speak to him.")
            return false
        }
        if (service.isDisabled(player)) {
            mes("The machinery is still shut down after overheating.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.putOreOnBelt() {
        if (!canOperate()) return
        var moved = false
        for (bar in Bar.entries) {
            if (player.smithingLvl < bar.level && bar.ores.any { inv.contains(it.obj) }) {
                mes("You need level ${bar.level} Smithing to smelt ${bar.name.lowercase()} bars.")
            }
        }
        val usable = Ore.entries.filter { ore -> Bar.entries.any { it.level <= player.smithingLvl && ore in it.ores } }
        for (ore in usable) {
            val room = BlastFurnaceRules.MAX_PRIMARY_ORES - player.totalOres()
            val count = minOf(inv.count(ore.obj), room)
            if (count <= 0) continue
            if (invDel(inv, ore.obj, count).failure) continue
            player.setOre(ore, player.ore(ore) + count)
            moved = true
        }
        val coalRoom = BlastFurnaceRules.MAX_COAL - player.furnaceCoal
        var coal = minOf(inv.count("obj.coal"), coalRoom)
        if (coal > 0 && invDel(inv, "obj.coal", coal).success) {
            player.furnaceCoal += coal
            moved = true
        } else {
            coal = 0
        }
        val fromBag = minOf(CoalBag.storedAmount(player), coalRoom - coal)
        if (fromBag > 0) {
            player.furnaceCoal += CoalBag.removeStored(player, fromBag)
            moved = true
        }
        if (!moved) {
            mes("You have nothing the furnace can take right now.")
            return
        }
        mes("You put the ore on the conveyor belt.")
        service.startSmelting(player)
    }

    private fun ProtectedAccess.collectCoke() {
        if (!canOperate()) return
        if (!inv.contains("obj.spade")) {
            mes("You need a spade to scoop coke.")
            return
        }
        if (player.firemakingLvl < BlastFurnaceRules.MANUAL_LEVEL) {
            mes("You need level ${BlastFurnaceRules.MANUAL_LEVEL} Firemaking to handle the coke.")
            return
        }
        if (service.hasCoke(player)) {
            mes("You already have a spade full of coke.")
            return
        }
        service.takeCoke(player)
        mes("You scoop up a spade of coke.")
    }

    private fun ProtectedAccess.refuel() {
        if (!canOperate()) return
        if (!service.spendCoke(player)) {
            mes("You need a spade full of coke to refuel the stove.")
            return
        }
        statAdvance("stat.firemaking", BlastFurnaceRules.COKE_FIREMAKING_XP)
        service.heat(player, BlastFurnaceRules.COKE_HEAT)
        service.startSmelting(player)
        mes("You refuel the stove.")
    }

    private suspend fun ProtectedAccess.pump() {
        if (!canOperate()) return
        if (player.strengthLvl < BlastFurnaceRules.MANUAL_LEVEL) {
            mes("You need level ${BlastFurnaceRules.MANUAL_LEVEL} Strength to work the pump.")
            return
        }
        repeat(BlastFurnaceRules.PUMP_TICKS) {
            delay(1)
            statAdvance("stat.strength", BlastFurnaceRules.PUMP_STRENGTH_XP)
            service.heat(player, BlastFurnaceRules.PUMP_HEAT)
        }
    }

    private suspend fun ProtectedAccess.pedal() {
        if (!canOperate()) return
        if (player.agilityLvl < BlastFurnaceRules.MANUAL_LEVEL) {
            mes("You need level ${BlastFurnaceRules.MANUAL_LEVEL} Agility to work the pedals.")
            return
        }
        repeat(BlastFurnaceRules.PEDAL_TICKS) {
            delay(1)
            statAdvance("stat.agility", BlastFurnaceRules.PEDAL_AGILITY_XP)
        }
    }

    private fun ProtectedAccess.readGauge() {
        val temperature = player.furnaceTemperature
        mes("The temperature gauge reads ${BlastFurnaceRules.gauge(temperature)} ($temperature%).")
    }

    private fun ProtectedAccess.checkFurnace() {
        mes(
            "The furnace holds ${player.totalOres()} ore, ${player.furnaceCoal} coal and " +
                "${player.totalBars()} bars are waiting."
        )
    }

    private fun ProtectedAccess.fillBucket() {
        if (invDel(inv, "obj.bucket_empty").failure) {
            mes("You need an empty bucket to fill at the sink.")
            return
        }
        invAdd(inv, "obj.bucket_water")
        mes("You fill the bucket with water.")
    }

    private fun ProtectedAccess.checkBars() {
        val held = Bar.entries.filter { player.bars(it) > 0 }
        if (held.isEmpty()) {
            mes("The dispenser is empty.")
            return
        }
        mes("Waiting: " + held.joinToString { "${player.bars(it)} ${it.name.lowercase()}" } + ".")
    }

    private fun ProtectedAccess.coolBars() {
        if (invDel(inv, "obj.bucket_water").failure) return
        invAdd(inv, "obj.bucket_empty")
        player.dispenserState = BlastFurnaceRules.DISPENSER_COOLED
        mes("You pour water over the bars to cool them.")
    }

    private fun ProtectedAccess.takeBars() {
        if (!service.hasAccess(player)) {
            mes("The foreman hasn't let you use the machinery. Speak to him.")
            return
        }
        if (player.dispenserState == BlastFurnaceRules.DISPENSER_HOT && !player.wearingIceSmithingGloves() &&
            "obj.ice_gloves" !in player.worn
        ) {
            mes("The bars are too hot to handle. Cool them with a bucket of water or wear ice gloves.")
            return
        }
        var took = false
        for (bar in Bar.entries) {
            while (player.bars(bar) > 0) {
                if (invAdd(inv, bar.obj).failure) {
                    mes("You don't have enough inventory space.")
                    finishCollect(took)
                    return
                }
                player.setBars(bar, player.bars(bar) - 1)
                statAdvance("stat.smithing", xpFor(bar))
                took = true
            }
        }
        finishCollect(took)
    }

    private fun ProtectedAccess.finishCollect(took: Boolean) {
        if (player.totalBars() == 0) player.dispenserState = BlastFurnaceRules.DISPENSER_EMPTY
        if (took) mes("You take the bars from the dispenser.")
    }

    private fun ProtectedAccess.xpFor(bar: Bar): Double {
        val goldsmith = bar.goldsmithXp != null && player.hasGoldsmithBonus()
        val base = if (goldsmith) checkNotNull(bar.goldsmithXp) else bar.xp
        return base * xpMods.get(player, "stat.smithing")
    }

    private fun org.rsmod.game.entity.Player.hasGoldsmithBonus(): Boolean =
        "obj.gauntlets_of_goldsmithing" in worn ||
            "obj.skillcape_smithing" in worn ||
            "obj.skillcape_smithing_trimmed" in worn ||
            "obj.skillcape_max" in worn

    private fun org.rsmod.game.entity.Player.baseSmithingLvl60(): Boolean =
        baseSmithingLvl >= BlastFurnaceRules.FREE_ENTRY_SMITHING

    private companion object {
        val STOVES =
            listOf(
                "loc.blast_furnace_stove_low",
                "loc.blast_furnace_stove_medium",
                "loc.blast_furnace_stove_full",
            )
        val BASEMENT = CoordGrid(1940, 4958, 0)
        val SURFACE = CoordGrid(2932, 10196, 0)
    }
}
