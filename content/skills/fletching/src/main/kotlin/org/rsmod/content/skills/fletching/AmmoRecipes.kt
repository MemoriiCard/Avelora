package org.rsmod.content.skills.fletching

object AmmoRecipes {
    const val CHISEL = "obj.chisel"

    private const val BOLTS_PER_SET = 10
    private const val DARTS_PER_SET = 10
    private const val JAVELINS_PER_SET = 15
    private const val TIPPED_BOLT_TICKS = 2
    private const val BOLT_TIP_TICKS = 5

    private fun bolt(unfinished: String, output: String, level: Int, xp: Double, metal: String) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(unfinished), FletchInput(FletchingRecipes.FEATHER)),
            perSet = BOLTS_PER_SET,
            ticks = 0,
            anim = "seq.human_fletching_add_bolt_feathers_$metal",
            message = "You fletch {count} bolts.",
        )

    val bolts: List<FletchRecipe> =
        listOf(
            bolt("obj.xbows_crossbow_bolts_bronze_unfeathered", "obj.bolt", 9, 0.5, "bronze"),
            bolt("obj.xbows_crossbow_bolts_blurite_unfeathered", "obj.xbows_crossbow_bolts_blurite", 24, 1.0, "blurite"),
            bolt("obj.xbows_crossbow_bolts_iron_unfeathered", "obj.xbows_crossbow_bolts_iron", 39, 1.5, "iron"),
            bolt("obj.xbows_crossbow_bolts_silver_unfeathered", "obj.xbows_crossbow_bolts_silver", 43, 2.5, "silver"),
            bolt("obj.xbows_crossbow_bolts_steel_unfeathered", "obj.xbows_crossbow_bolts_steel", 46, 3.5, "steel"),
            bolt("obj.xbows_crossbow_bolts_mithril_unfeathered", "obj.xbows_crossbow_bolts_mithril", 54, 5.0, "mithril"),
            bolt("obj.xbows_crossbow_bolts_adamantite_unfeathered", "obj.xbows_crossbow_bolts_adamantite", 61, 7.0, "adamant"),
            bolt("obj.xbows_crossbow_bolts_runite_unfeathered", "obj.xbows_crossbow_bolts_runite", 69, 10.0, "rune"),
            bolt("obj.dragon_bolts_unfeathered", "obj.dragon_bolts", 84, 12.0, "dragon"),
        )

    private fun dart(metal: String, level: Int, xp: Double, anim: String = metal) =
        FletchRecipe(
            output = "obj.${metal}_dart",
            level = level,
            xp = xp,
            inputs = listOf(FletchInput("obj.${metal}_dart_tip"), FletchInput(FletchingRecipes.FEATHER)),
            perSet = DARTS_PER_SET,
            ticks = 0,
            anim = "seq.human_fletching_add_dart_feathers_$anim",
            message = "You finish making {count} darts.",
        )

    val darts: List<FletchRecipe> =
        listOf(
            dart("bronze", 10, 1.8),
            dart("iron", 22, 3.8),
            dart("steel", 37, 7.5),
            dart("mithril", 52, 11.2),
            dart("adamant", 67, 15.0),
            dart("rune", 81, 18.8),
            dart("amethyst", 90, 21.0),
            dart("dragon", 95, 25.0),
        )

    private fun javelin(metal: String, level: Int, xp: Double) =
        FletchRecipe(
            output = "obj.${metal}_javelin",
            level = level,
            xp = xp,
            inputs = listOf(FletchInput("obj.${metal}_javelin_head"), FletchInput("obj.javelin_shaft")),
            perSet = JAVELINS_PER_SET,
            ticks = 0,
            anim = "seq.human_fletching_add_arrow_tips",
            message = "You attach javelin heads to {count} javelin shafts.",
        )

    val javelins: List<FletchRecipe> =
        listOf(
            javelin("bronze", 3, 1.0),
            javelin("iron", 17, 2.0),
            javelin("steel", 32, 5.0),
            javelin("mithril", 47, 8.0),
            javelin("adamant", 62, 10.0),
            javelin("rune", 77, 12.4),
            javelin("amethyst", 84, 13.5),
            javelin("dragon", 92, 15.0),
        )

    private fun tips(gem: String, output: String, count: Int, level: Int, xp: Double, anim: String) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(gem)),
            outputCount = count,
            ticks = BOLT_TIP_TICKS,
            anim = anim,
            tool = CHISEL,
            message = "You cut the gem into $count bolt tips.",
        )

    val boltTips: List<FletchRecipe> =
        listOf(
            tips("obj.opal", "obj.opal_bolttips", 12, 11, 1.5, "seq.human_opalcutting"),
            tips("obj.jade", "obj.xbows_bolt_tips_jade", 12, 26, 2.0, "seq.human_jadecutting"),
            tips("obj.smalloysterpearls", "obj.pearl_bolttips", 6, 41, 3.2, "seq.xbows_fletching_pearl"),
            tips("obj.bigoysterpearls", "obj.pearl_bolttips", 24, 41, 3.2, "seq.xbows_fletching_pearl"),
            tips("obj.red_topaz", "obj.xbows_bolt_tips_redtopaz", 12, 48, 3.9, "seq.human_redtopazcutting"),
            tips("obj.sapphire", "obj.xbows_bolt_tips_sapphire", 12, 56, 4.0, "seq.human_sapphirecutting"),
            tips("obj.emerald", "obj.xbows_bolt_tips_emerald", 12, 58, 5.5, "seq.human_emeraldcutting"),
            tips("obj.ruby", "obj.xbows_bolt_tips_ruby", 12, 63, 6.3, "seq.human_rubycutting"),
            tips("obj.diamond", "obj.xbows_bolt_tips_diamond", 12, 65, 7.0, "seq.human_diamondcutting"),
            tips("obj.dragonstone", "obj.xbows_bolt_tips_dragonstone", 12, 71, 8.2, "seq.human_dragonstonecutting"),
            tips("obj.onyx", "obj.xbows_bolt_tips_onyx", 24, 73, 9.4, "seq.human_onyxcutting"),
        )

    private fun tipped(tips: String, bolts: String, output: String, level: Int, xp: Double, metal: String) =
        FletchRecipe(
            output = output,
            level = level,
            xp = xp,
            inputs = listOf(FletchInput(bolts), FletchInput(tips)),
            perSet = BOLTS_PER_SET,
            ticks = TIPPED_BOLT_TICKS,
            anim = "seq.human_fletching_add_bolt_tips_$metal",
            message = "You fletch {count} bolts.",
        )

    private const val JADE = "obj.xbows_bolt_tips_jade"
    private const val TOPAZ = "obj.xbows_bolt_tips_redtopaz"
    private const val SAPPHIRE = "obj.xbows_bolt_tips_sapphire"
    private const val EMERALD = "obj.xbows_bolt_tips_emerald"
    private const val RUBY = "obj.xbows_bolt_tips_ruby"
    private const val DIAMOND = "obj.xbows_bolt_tips_diamond"
    private const val DRAGONSTONE = "obj.xbows_bolt_tips_dragonstone"
    private const val ONYX = "obj.xbows_bolt_tips_onyx"

    val tippedBolts: List<FletchRecipe> =
        listOf(
            tipped("obj.opal_bolttips", "obj.bolt", "obj.opal_bolt", 11, 1.6, "bronze"),
            tipped(JADE, "obj.xbows_crossbow_bolts_blurite", "obj.xbows_crossbow_bolts_blurite_tipped_jade", 26, 2.4, "blurite"),
            tipped("obj.pearl_bolttips", "obj.xbows_crossbow_bolts_iron", "obj.pearl_bolt", 41, 3.2, "iron"),
            tipped(TOPAZ, "obj.xbows_crossbow_bolts_steel", "obj.xbows_crossbow_bolts_steel_tipped_redtopaz", 48, 3.9, "steel"),
            tipped(SAPPHIRE, "obj.xbows_crossbow_bolts_mithril", "obj.xbows_crossbow_bolts_mithril_tipped_sapphire", 56, 4.7, "mithril"),
            tipped(EMERALD, "obj.xbows_crossbow_bolts_mithril", "obj.xbows_crossbow_bolts_mithril_tipped_emerald", 58, 5.5, "mithril"),
            tipped(RUBY, "obj.xbows_crossbow_bolts_adamantite", "obj.xbows_crossbow_bolts_adamantite_tipped_ruby", 63, 6.3, "adamant"),
            tipped(DIAMOND, "obj.xbows_crossbow_bolts_adamantite", "obj.xbows_crossbow_bolts_adamantite_tipped_diamond", 65, 7.0, "adamant"),
            tipped(DRAGONSTONE, "obj.xbows_crossbow_bolts_runite", "obj.xbows_crossbow_bolts_runite_tipped_dragonstone", 71, 8.2, "rune"),
            tipped(ONYX, "obj.xbows_crossbow_bolts_runite", "obj.xbows_crossbow_bolts_runite_tipped_onyx", 73, 9.4, "rune"),
        ) +
            listOf(
                Triple("obj.opal_bolttips", "opal", 1.6),
                Triple(JADE, "jade", 2.4),
                Triple("obj.pearl_bolttips", "pearl", 3.2),
                Triple(TOPAZ, "topaz", 4.0),
                Triple(SAPPHIRE, "sapphire", 4.7),
                Triple(EMERALD, "emerald", 5.5),
                Triple(RUBY, "ruby", 6.3),
                Triple(DIAMOND, "diamond", 7.0),
                Triple(DRAGONSTONE, "dragonstone", 8.2),
                Triple(ONYX, "onyx", 9.4),
            ).map { (tips, gem, xp) ->
                tipped(tips, "obj.dragon_bolts", "obj.dragon_bolts_unenchanted_$gem", 84, xp, "dragon")
            }

    val all: List<FletchRecipe>
        get() = bolts + darts + javelins + boltTips + tippedBolts
}
