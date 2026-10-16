package org.rsmod.content.skills.fletching

import org.rsmod.content.skills.SkillingActionType

object CrossbowRecipes {
    const val HAMMER = "obj.hammer"
    const val CROSSBOW_STRING = "obj.xbows_crossbow_string"

    private data class Crossbow(
        val metal: String,
        val stock: String,
        val level: Int,
        val limbsXp: Double,
        val stringXp: Double,
        val limbsAnim: String,
    )

    private val crossbows =
        listOf(
            Crossbow("bronze", "wood", 9, 12.0, 6.0, "wood_bronze"),
            Crossbow("blurite", "oak", 24, 32.0, 16.0, "oak_blurite"),
            Crossbow("iron", "willow", 39, 44.0, 22.0, "willow_iron"),
            Crossbow("steel", "teak", 46, 54.0, 27.0, "teak_steel"),
            Crossbow("mithril", "maple", 54, 64.0, 32.0, "maple_mithril"),
            Crossbow("adamantite", "mahogany", 61, 82.0, 41.0, "mahogany_adamantite"),
            Crossbow("runite", "yew", 69, 100.0, 50.0, "yew_runite"),
            Crossbow("dragon", "magic", 78, 135.0, 70.0, "yew_dragon"),
        )

    val limbs: List<FletchRecipe> =
        crossbows.map {
            FletchRecipe(
                output = "obj.xbows_crossbow_unstrung_${it.metal}",
                level = it.level,
                xp = it.limbsXp,
                inputs =
                    listOf(
                        FletchInput("obj.xbows_crossbow_stock_${it.stock}"),
                        FletchInput("obj.xbows_crossbow_limbs_${it.metal}"),
                    ),
                ticks = 2,
                anim = "seq.xbows_fletching_${it.limbsAnim}",
                tool = HAMMER,
                message = "You attach the metal limbs to the stock.",
            )
        }

    val stringing: List<FletchRecipe> =
        crossbows.map {
            FletchRecipe(
                output = "obj.xbows_crossbow_${it.metal}",
                level = it.level,
                xp = it.stringXp,
                inputs =
                    listOf(
                        FletchInput("obj.xbows_crossbow_unstrung_${it.metal}"),
                        FletchInput(CROSSBOW_STRING),
                    ),
                ticks = 2,
                firstTicks = 1,
                anim = "seq.xbows_stringing_crossbow_${it.metal}",
                sound = "synth.stringing",
                menu = SkillingActionType.STRING,
                message = "You add a string to the crossbow.",
            )
        }

    val all: List<FletchRecipe>
        get() = limbs + stringing
}
