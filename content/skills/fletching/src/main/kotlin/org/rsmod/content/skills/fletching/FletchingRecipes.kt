package org.rsmod.content.skills.fletching

import org.rsmod.content.skills.SkillingActionType

data class FletchInput(val obj: String, val count: Int = 1)

/**
 * One Fletching product. A single "action" consumes [inputs] and yields [outputCount] of [output].
 *
 * Set-based recipes ([perSet] > 0, e.g. arrows) instead consume up to [perSet] of each input per
 * action, yield the same number of outputs, and pay [xp] for every item made. Everything else pays
 * [xp] once per action.
 *
 * A [ticks] of 0 marks a one-click recipe (bolts, darts, javelins): no Make-X menu, each click
 * makes one set straight away, as in OSRS.
 */
data class FletchRecipe(
    val output: String,
    val level: Int,
    val xp: Double,
    val inputs: List<FletchInput>,
    val outputCount: Int = 1,
    val perSet: Int = 0,
    val ticks: Int,
    val firstTicks: Int = ticks,
    val anim: String,
    val sound: String? = null,
    val tool: String? = null,
    val menu: SkillingActionType = if (perSet > 0) SkillingActionType.MAKE_SETS else SkillingActionType.MAKE,
    val message: String,
) {
    val isSet: Boolean
        get() = perSet > 0

    val isInstant: Boolean
        get() = ticks == 0

    /** The two objs used on each other to start this recipe: the tool and sole input, or both inputs. */
    val trigger: Pair<String, String>
        get() =
            if (inputs.size == 1) {
                checkNotNull(tool) { "Single-input recipe $output needs a tool" } to inputs.single().obj
            } else {
                inputs[0].obj to inputs[1].obj
            }
}

object FletchingRecipes {
    const val KNIFE = "obj.knife"
    const val BOW_STRING = "obj.bow_string"
    const val FEATHER = "obj.feather"
    const val ARROW_SHAFT = "obj.arrow_shaft"
    const val HEADLESS_ARROW = "obj.headless_arrow"

    private const val KNIFE_ANIM = "seq.human_fletching"
    private const val KNIFE_TICKS = 3
    private const val KNIFE_FIRST_TICKS = 2
    private const val SHIELD_TICKS = 7
    private const val ARROWS_PER_SET = 15

    private fun shafts(log: String, level: Int, xp: Double, count: Int) =
        FletchRecipe(
            output = ARROW_SHAFT,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(log)),
            outputCount = count,
            ticks = KNIFE_TICKS,
            tool = KNIFE,
            firstTicks = KNIFE_FIRST_TICKS,
            anim = KNIFE_ANIM,
            message = "You carefully cut the wood into $count arrow shafts.",
        )

    private fun carve(log: String, output: String, level: Int, xp: Double, what: String) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(log)),
            ticks = KNIFE_TICKS,
            tool = KNIFE,
            firstTicks = KNIFE_FIRST_TICKS,
            anim = KNIFE_ANIM,
            message = "You carefully cut the wood into $what.",
        )

    private fun shield(log: String, output: String, level: Int, xp: Double) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(log, 2)),
            ticks = SHIELD_TICKS,
            tool = KNIFE,
            anim = KNIFE_ANIM,
            message = "You carefully cut the wood into a shield.",
        )

    val logCutting: Map<String, List<FletchRecipe>> =
        linkedMapOf(
            "obj.logs" to
                listOf(
                    shafts("obj.logs", 1, 5.0, 15),
                    FletchRecipe(
                        output = "obj.javelin_shaft",
                        level = 3,
                        xp = 5.0,
                        inputs = listOf(FletchInput("obj.logs")),
                        outputCount = 15,
                        ticks = KNIFE_TICKS,
                        tool = KNIFE,
                        firstTicks = KNIFE_FIRST_TICKS,
                        anim = KNIFE_ANIM,
                        message = "You carefully cut the wood into 15 javelin shafts.",
                    ),
                    carve("obj.logs", "obj.unstrung_shortbow", 5, 5.0, "a shortbow"),
                    carve("obj.logs", "obj.unstrung_longbow", 10, 10.0, "a longbow"),
                    carve("obj.logs", "obj.xbows_crossbow_stock_wood", 9, 6.0, "a crossbow stock"),
                ),
            "obj.oak_logs" to
                listOf(
                    shafts("obj.oak_logs", 15, 10.0, 30),
                    carve("obj.oak_logs", "obj.unstrung_oak_shortbow", 20, 16.5, "a shortbow"),
                    carve("obj.oak_logs", "obj.unstrung_oak_longbow", 25, 25.0, "a longbow"),
                    carve("obj.oak_logs", "obj.xbows_crossbow_stock_oak", 24, 16.0, "a crossbow stock"),
                    shield("obj.oak_logs", "obj.oak_shield", 27, 50.0),
                ),
            "obj.willow_logs" to
                listOf(
                    shafts("obj.willow_logs", 30, 15.0, 45),
                    carve("obj.willow_logs", "obj.unstrung_willow_shortbow", 35, 33.3, "a shortbow"),
                    carve("obj.willow_logs", "obj.unstrung_willow_longbow", 40, 41.5, "a longbow"),
                    carve("obj.willow_logs", "obj.xbows_crossbow_stock_willow", 39, 22.0, "a crossbow stock"),
                    shield("obj.willow_logs", "obj.willow_shield", 42, 83.0),
                ),
            "obj.teak_logs" to
                listOf(
                    carve("obj.teak_logs", "obj.xbows_crossbow_stock_teak", 46, 27.0, "a crossbow stock"),
                ),
            "obj.maple_logs" to
                listOf(
                    shafts("obj.maple_logs", 45, 20.0, 60),
                    carve("obj.maple_logs", "obj.unstrung_maple_shortbow", 50, 50.0, "a shortbow"),
                    carve("obj.maple_logs", "obj.unstrung_maple_longbow", 55, 58.3, "a longbow"),
                    carve("obj.maple_logs", "obj.xbows_crossbow_stock_maple", 54, 32.0, "a crossbow stock"),
                    shield("obj.maple_logs", "obj.maple_shield", 57, 116.5),
                ),
            "obj.mahogany_logs" to
                listOf(
                    carve("obj.mahogany_logs", "obj.xbows_crossbow_stock_mahogany", 61, 41.0, "a crossbow stock"),
                ),
            "obj.yew_logs" to
                listOf(
                    shafts("obj.yew_logs", 60, 25.0, 75),
                    carve("obj.yew_logs", "obj.unstrung_yew_shortbow", 65, 67.5, "a shortbow"),
                    carve("obj.yew_logs", "obj.unstrung_yew_longbow", 70, 75.0, "a longbow"),
                    carve("obj.yew_logs", "obj.xbows_crossbow_stock_yew", 69, 50.0, "a crossbow stock"),
                    shield("obj.yew_logs", "obj.yew_shield", 72, 150.0),
                ),
            "obj.magic_logs" to
                listOf(
                    shafts("obj.magic_logs", 75, 30.0, 90),
                    carve("obj.magic_logs", "obj.unstrung_magic_shortbow", 80, 83.3, "a shortbow"),
                    carve("obj.magic_logs", "obj.unstrung_magic_longbow", 85, 91.5, "a longbow"),
                    carve("obj.magic_logs", "obj.xbows_crossbow_stock_magic", 78, 70.0, "a crossbow stock"),
                    shield("obj.magic_logs", "obj.magic_shield", 87, 183.0),
                ),
            "obj.redwood_logs" to
                listOf(
                    shafts("obj.redwood_logs", 90, 35.0, 105),
                    shield("obj.redwood_logs", "obj.redwood_shield", 92, 216.0),
                ),
        )

    private fun string(unstrung: String, strung: String, level: Int, xp: Double, anim: String) =
        FletchRecipe(
            output = strung,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(unstrung), FletchInput(BOW_STRING)),
            ticks = 2,
            anim = anim,
            sound = "synth.stringing",
            menu = SkillingActionType.STRING,
            message = "You add a string to the bow.",
        )

    val bowStringing: List<FletchRecipe> =
        listOf(
            string("obj.unstrung_shortbow", "obj.shortbow", 5, 5.0, "seq.stringing_shortbow"),
            string("obj.unstrung_longbow", "obj.longbow", 10, 10.0, "seq.stringing_longbow"),
            string("obj.unstrung_oak_shortbow", "obj.oak_shortbow", 20, 16.5, "seq.stringing_oak_shortbow"),
            string("obj.unstrung_oak_longbow", "obj.oak_longbow", 25, 25.0, "seq.stringing_oak_longbow"),
            string("obj.unstrung_willow_shortbow", "obj.willow_shortbow", 35, 33.2, "seq.stringing_willow_shortbow"),
            string("obj.unstrung_willow_longbow", "obj.willow_longbow", 40, 41.5, "seq.stringing_willow_longbow"),
            string("obj.unstrung_maple_shortbow", "obj.maple_shortbow", 50, 50.0, "seq.stringing_maple_shortbow"),
            string("obj.unstrung_maple_longbow", "obj.maple_longbow", 55, 58.2, "seq.stringing_maple_longbow"),
            string("obj.unstrung_yew_shortbow", "obj.yew_shortbow", 65, 67.5, "seq.stringing_yew_shortbow"),
            string("obj.unstrung_yew_longbow", "obj.yew_longbow", 70, 75.0, "seq.stringing_yew_longbow"),
            string("obj.unstrung_magic_shortbow", "obj.magic_shortbow", 80, 83.2, "seq.stringing_magic_shortbow"),
            string("obj.unstrung_magic_longbow", "obj.magic_longbow", 85, 91.5, "seq.stringing_magic_longbow"),
        )

    val headlessArrows: FletchRecipe =
        FletchRecipe(
            output = HEADLESS_ARROW,
            level = 1,
            xp = 1.0,
            inputs = listOf(FletchInput(ARROW_SHAFT), FletchInput(FEATHER)),
            perSet = ARROWS_PER_SET,
            ticks = 2,
            anim = "seq.human_fletching_add_feather",
            message = "You attach feathers to {count} arrow shafts.",
        )

    private fun arrow(tips: String, output: String, level: Int, xp: Double) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(HEADLESS_ARROW), FletchInput(tips)),
            perSet = ARROWS_PER_SET,
            ticks = 2,
            anim = "seq.human_fletching_add_arrow_tips",
            message = "You attach arrow heads to {count} arrow shafts.",
        )

    val arrows: List<FletchRecipe> =
        listOf(
            arrow("obj.bronze_arrowheads", "obj.bronze_arrow", 1, 1.3),
            arrow("obj.iron_arrowheads", "obj.iron_arrow", 15, 2.5),
            arrow("obj.steel_arrowheads", "obj.steel_arrow", 30, 5.0),
            arrow("obj.mithril_arrowheads", "obj.mithril_arrow", 45, 7.5),
            arrow("obj.adamant_arrowheads", "obj.adamant_arrow", 60, 10.0),
            arrow("obj.rune_arrowheads", "obj.rune_arrow", 75, 12.5),
            arrow("obj.amethyst_arrowheads", "obj.amethyst_arrow", 82, 13.5),
            arrow("obj.dragon_arrowheads", "obj.dragon_arrow", 90, 15.0),
        )

    val all: List<FletchRecipe>
        get() =
            logCutting.values.flatten() + bowStringing + headlessArrows + arrows +
                AmmoRecipes.all + CrossbowRecipes.all

    /** How many actions the inventory can afford, given a per-input [count] function. */
    fun FletchRecipe.maxActions(count: (String) -> Int): Int =
        if (isSet) {
            val items = inputs.minOf { count(it.obj) / it.count }
            (items + perSet - 1) / perSet
        } else {
            inputs.minOf { count(it.obj) / it.count }
        }

    /** Items a set-based recipe makes this action: a full set, or whatever is left. */
    fun FletchRecipe.setSize(count: (String) -> Int): Int =
        minOf(perSet, inputs.minOf { count(it.obj) / it.count })
}
