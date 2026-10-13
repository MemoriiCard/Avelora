package org.rsmod.content.minigames.blastfurnace

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.furnaceCoal by intVarBit("varbit.blast_furnace_coal")
internal var Player.furnaceTemperature by intVarBit("varbit.blast_furnace_temperature_reading")
internal var Player.dispenserState by intVarBit("varbit.blast_furnace_bars_hot")
internal var Player.coalRequired by intVarBit("varbit.blast_furnace_coal_required")

private var Player.copperOre by intVarBit("varbit.blast_furnace_copper_ore")
private var Player.tinOre by intVarBit("varbit.blast_furnace_tin_ore")
private var Player.ironOre by intVarBit("varbit.blast_furnace_iron_ore")
private var Player.silverOre by intVarBit("varbit.blast_furnace_silver_ore")
private var Player.goldOre by intVarBit("varbit.blast_furnace_gold_ore")
private var Player.mithrilOre by intVarBit("varbit.blast_furnace_mithril_ore")
private var Player.adamantOre by intVarBit("varbit.blast_furnace_adamantite_ore")
private var Player.runiteOre by intVarBit("varbit.blast_furnace_runite_ore")

private var Player.bronzeBars by intVarBit("varbit.blast_furnace_bronze_bars")
private var Player.ironBars by intVarBit("varbit.blast_furnace_iron_bars")
private var Player.silverBars by intVarBit("varbit.blast_furnace_silver_bars")
private var Player.steelBars by intVarBit("varbit.blast_furnace_steel_bars")
private var Player.goldBars by intVarBit("varbit.blast_furnace_gold_bars")
private var Player.mithrilBars by intVarBit("varbit.blast_furnace_mithril_bars")
private var Player.adamantBars by intVarBit("varbit.blast_furnace_adamantite_bars")
private var Player.runiteBars by intVarBit("varbit.blast_furnace_runite_bars")

internal fun Player.ore(ore: Ore): Int =
    when (ore) {
        Ore.Copper -> copperOre
        Ore.Tin -> tinOre
        Ore.Iron -> ironOre
        Ore.Silver -> silverOre
        Ore.Gold -> goldOre
        Ore.Mithril -> mithrilOre
        Ore.Adamant -> adamantOre
        Ore.Runite -> runiteOre
    }

internal fun Player.setOre(ore: Ore, value: Int) {
    when (ore) {
        Ore.Copper -> copperOre = value
        Ore.Tin -> tinOre = value
        Ore.Iron -> ironOre = value
        Ore.Silver -> silverOre = value
        Ore.Gold -> goldOre = value
        Ore.Mithril -> mithrilOre = value
        Ore.Adamant -> adamantOre = value
        Ore.Runite -> runiteOre = value
    }
}

internal fun Player.bars(bar: Bar): Int =
    when (bar) {
        Bar.Bronze -> bronzeBars
        Bar.Iron -> ironBars
        Bar.Silver -> silverBars
        Bar.Steel -> steelBars
        Bar.Gold -> goldBars
        Bar.Mithril -> mithrilBars
        Bar.Adamant -> adamantBars
        Bar.Runite -> runiteBars
    }

internal fun Player.setBars(bar: Bar, value: Int) {
    when (bar) {
        Bar.Bronze -> bronzeBars = value
        Bar.Iron -> ironBars = value
        Bar.Silver -> silverBars = value
        Bar.Steel -> steelBars = value
        Bar.Gold -> goldBars = value
        Bar.Mithril -> mithrilBars = value
        Bar.Adamant -> adamantBars = value
        Bar.Runite -> runiteBars = value
    }
}

internal fun Player.totalOres(): Int = Ore.entries.sumOf { ore(it) }

internal fun Player.totalBars(): Int = Bar.entries.sumOf { bars(it) }
