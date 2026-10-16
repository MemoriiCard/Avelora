package org.rsmod.content.skills.fletching

object ExtraRecipes {
    private const val BROAD_ARROWS_PER_SET = 15
    private const val BROAD_BOLTS_PER_SET = 10
    private const val CUT_TICKS = 5
    private const val FLIGHT_ANIM = "seq.human_fletching_add_arrow_tips"

    val broad: List<FletchRecipe> =
        listOf(
            FletchRecipe(
                output = "obj.slayer_broad_arrows",
                level = 52,
                xp = 10.0,
                inputs =
                    listOf(
                        FletchInput(FletchingRecipes.HEADLESS_ARROW),
                        FletchInput("obj.slayer_broad_arrowhead"),
                    ),
                perSet = BROAD_ARROWS_PER_SET,
                ticks = 2,
                anim = FLIGHT_ANIM,
                message = "You attach broad arrowheads to {count} arrow shafts.",
                requiresBroader = true,
            ),
            FletchRecipe(
                output = "obj.slayer_broad_bolt",
                level = 55,
                xp = 3.0,
                inputs =
                    listOf(
                        FletchInput("obj.slayer_broad_bolt_unfinished"),
                        FletchInput(FletchingRecipes.FEATHER),
                    ),
                perSet = BROAD_BOLTS_PER_SET,
                ticks = 0,
                anim = "seq.human_fletching_add_bolt_feathers_iron",
                message = "You fletch {count} broad bolts.",
                requiresBroader = true,
            ),
            FletchRecipe(
                output = "obj.slayer_broad_bolt_amethyst",
                level = 76,
                xp = 10.6,
                inputs =
                    listOf(
                        FletchInput("obj.slayer_broad_bolt"),
                        FletchInput("obj.xbows_bolt_tips_amethyst"),
                    ),
                perSet = BROAD_BOLTS_PER_SET,
                ticks = 2,
                anim = "seq.human_fletching_add_bolt_tips_rune",
                message = "You fletch {count} amethyst broad bolts.",
                requiresBroader = true,
            ),
        )

    private fun amethyst(output: String, count: Int, level: Int, xp: Double) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput("obj.amethyst")),
            outputCount = count,
            ticks = CUT_TICKS,
            anim = "seq.human_emeraldcutting",
            tool = AmmoRecipes.CHISEL,
            message = "You cut the amethyst into $count pieces.",
        )

    val amethystCutting: List<FletchRecipe> =
        listOf(
            amethyst("obj.amethyst_arrowheads", 15, 82, 4.0),
            amethyst("obj.xbows_bolt_tips_amethyst", 15, 83, 4.0),
            amethyst("obj.amethyst_javelin_head", 5, 84, 6.0),
            amethyst("obj.amethyst_dart_tip", 8, 85, 7.5),
        )

    private fun ballista(inputA: String, inputB: String, output: String, level: Int, xp: Double) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(inputA), FletchInput(inputB)),
            ticks = 2,
            anim = "seq.human_fletching",
            tool = CrossbowRecipes.HAMMER,
            message = "You assemble the ballista.",
        )

    val ballistae: List<FletchRecipe> =
        listOf(
            ballista("obj.ballista_frame_light", "obj.ballista_limbs", "obj.ballista_incomplete_light", 72, 0.0),
            ballista("obj.ballista_incomplete_light", "obj.ballista_spring", "obj.ballista_unstrung_light", 72, 0.0),
            ballista("obj.ballista_unstrung_light", "obj.ballista_rope", "obj.light_ballista", 72, 110.0),
            ballista("obj.ballista_frame_heavy", "obj.ballista_limbs", "obj.ballista_incomplete_heavy", 78, 0.0),
            ballista("obj.ballista_incomplete_heavy", "obj.ballista_spring", "obj.ballista_unstrung_heavy", 78, 0.0),
            ballista("obj.ballista_unstrung_heavy", "obj.ballista_rope", "obj.heavy_ballista", 78, 150.0),
        )

    val all: List<FletchRecipe>
        get() = broad + amethystCutting + ballistae
}
