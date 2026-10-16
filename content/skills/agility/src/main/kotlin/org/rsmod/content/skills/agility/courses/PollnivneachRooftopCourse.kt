package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.Fall
import org.rsmod.content.skills.agility.Rooftop
import org.rsmod.map.CoordGrid

object PollnivneachRooftopCourse {
    private val STALL =
        Fall(FailChance(low = 190, high = 265), CoordGrid(3349, 2970, 0), 1..5, "You slip and fall onto the ground.")

    val course =
        AgilityCourse(
            id = 10,
            name = "Pollnivneach Rooftop",
            level = 70,
            lapVarp = "varp.agility_laps_pollnivneach",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(3348, 2968, 1),
                    CoordGrid(3354, 2976, 1),
                    CoordGrid(3361, 2977, 1),
                    CoordGrid(3368, 2976, 1),
                    CoordGrid(3358, 2993, 2),
                ),
            obstacles =
                listOf(
                    Rooftop.climb("loc.rooftops_pollnivneach_basket", 10.0, CoordGrid(3348, 2968, 1)),
                    Rooftop.leap(listOf("loc.rooftops_pollnivneach_marketstall"), 45.0, CoordGrid(3354, 2976, 1), STALL),
                    Rooftop.leap(listOf("loc.rooftops_pollnivneach_hangingbanner"), 65.0, CoordGrid(3361, 2977, 1)),
                    Rooftop.leap(listOf("loc.rooftops_pollnivneach_gap"), 35.0, CoordGrid(3368, 2976, 1)),
                    Rooftop.leap(listOf("loc.rooftops_pollnivneach_tree"), 75.0, CoordGrid(3367, 2982, 1)),
                    Rooftop.climb("loc.rooftops_pollnivneach_wallclimb", 5.0, CoordGrid(3358, 2984, 2)),
                    Rooftop.rope(
                        listOf("loc.rooftops_pollnivneach_monkeybars_start"),
                        55.0,
                        CoordGrid(3358, 2985, 2),
                        CoordGrid(3358, 2993, 2),
                        walk = "seq.human_ledge_walk_left",
                        ready = "seq.human_ledge_on_left",
                    ),
                    Rooftop.leap(listOf("loc.rooftops_pollnivneach_treetop"), 60.0, CoordGrid(3359, 3001, 2)),
                    Rooftop.leap(listOf("loc.rooftops_pollnivneach_line"), 540.0, CoordGrid(3362, 3002, 0)),
                ),
        )
}
