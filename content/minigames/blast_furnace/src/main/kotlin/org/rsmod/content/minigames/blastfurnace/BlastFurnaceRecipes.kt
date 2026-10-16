package org.rsmod.content.minigames.blastfurnace

internal enum class Bar(
    val obj: String,
    val barVarbit: String,
    val level: Int,
    val xp: Double,
    val ores: List<Ore>,
    val coal: Int = 0,
    val goldsmithXp: Double? = null,
) {
    Bronze("obj.bronze_bar", "varbit.blast_furnace_bronze_bars", 1, 6.2, listOf(Ore.Copper, Ore.Tin)),
    Iron("obj.iron_bar", "varbit.blast_furnace_iron_bars", 15, 12.5, listOf(Ore.Iron)),
    Silver("obj.silver_bar", "varbit.blast_furnace_silver_bars", 20, 13.7, listOf(Ore.Silver)),
    Steel("obj.steel_bar", "varbit.blast_furnace_steel_bars", 30, 17.5, listOf(Ore.Iron), coal = 1),
    Gold("obj.gold_bar", "varbit.blast_furnace_gold_bars", 40, 22.5, listOf(Ore.Gold), goldsmithXp = 56.2),
    Mithril("obj.mithril_bar", "varbit.blast_furnace_mithril_bars", 50, 30.0, listOf(Ore.Mithril), coal = 2),
    Adamant("obj.adamantite_bar", "varbit.blast_furnace_adamantite_bars", 70, 37.5, listOf(Ore.Adamant), coal = 3),
    Runite("obj.runite_bar", "varbit.blast_furnace_runite_bars", 85, 50.0, listOf(Ore.Runite), coal = 4),
    ;

    companion object {
        val smeltOrder = listOf(Steel, Iron, Bronze, Silver, Gold, Mithril, Adamant, Runite)
    }
}

internal enum class Ore(val obj: String, val varbit: String) {
    Copper("obj.copper_ore", "varbit.blast_furnace_copper_ore"),
    Tin("obj.tin_ore", "varbit.blast_furnace_tin_ore"),
    Iron("obj.iron_ore", "varbit.blast_furnace_iron_ore"),
    Silver("obj.silver_ore", "varbit.blast_furnace_silver_ore"),
    Gold("obj.gold_ore", "varbit.blast_furnace_gold_ore"),
    Mithril("obj.mithril_ore", "varbit.blast_furnace_mithril_ore"),
    Adamant("obj.adamantite_ore", "varbit.blast_furnace_adamantite_ore"),
    Runite("obj.runite_ore", "varbit.blast_furnace_runite_ore"),
}

internal object BlastFurnaceRules {
    const val MAX_PRIMARY_ORES = 28
    const val MAX_COAL = 254
    const val MAX_BARS = 28
    const val FREE_ENTRY_SMITHING = 60
    const val FEE_COINS = 2500
    const val FEE_TICKS = 1000
    const val MANUAL_LEVEL = 30
    const val SMELT_INTERVAL = 2
    const val MIN_TEMPERATURE = 10
    const val DECAY_EVERY = 10
    const val COKE_HEAT = 10
    const val PUMP_HEAT = 1
    const val PUMP_TICKS = 5
    const val PEDAL_TICKS = 5
    const val OVERHEAT_AT = 100
    const val OVERHEAT_RESET = 60
    const val OVERHEAT_DISABLED_TICKS = 100
    const val COKE_FIREMAKING_XP = 5.0
    const val PUMP_STRENGTH_XP = 2.0
    const val PEDAL_AGILITY_XP = 1.0
    const val LOW_GAUGE = 33
    const val HIGH_GAUGE = 66

    const val DISPENSER_EMPTY = 0
    const val DISPENSER_HOT = 2
    const val DISPENSER_COOLED = 3

    fun gauge(temperature: Int): String =
        when {
            temperature < LOW_GAUGE -> "low"
            temperature < HIGH_GAUGE -> "medium"
            else -> "high"
        }

    fun canSmelt(bar: Bar, ore: (Ore) -> Int, coal: Int): Boolean =
        bar.ores.all { ore(it) > 0 } && coal >= bar.coal
}
