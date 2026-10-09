package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.Fall
import org.rsmod.content.skills.agility.Rooftop
import org.rsmod.map.CoordGrid

object ArdougneRooftopCourse {
    private val FAIL = FailChance(low = 210, high = 255)
    private val PLANK = Fall(FAIL, CoordGrid(2660, 3320, 0), 2..6, "You slip and fall to the ground.")
    private val BALANCE = Fall(FAIL, CoordGrid(2654, 3298, 0), 2..6, "You slip and fall to the ground.")

    val course =
        AgilityCourse(
            id = 11,
            name = "Ardougne Rooftop",
            level = 90,
            lapVarp = "varp.agility_laps_ardougne",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(2671, 3304, 3),
                    CoordGrid(2663, 3318, 3),
                    CoordGrid(2655, 3318, 3),
                    CoordGrid(2653, 3314, 3),
                    CoordGrid(2653, 3305, 3),
                    CoordGrid(2654, 3299, 3),
                ),
            obstacles =
                listOf(
                    Rooftop.climb("loc.rooftops_ardy_wallclimb", 43.0, CoordGrid(2673, 3298, 3)),
                    Rooftop.leap(listOf("loc.rooftops_ardy_jump"), 15.0, CoordGrid(2667, 3311, 3)),
                    Rooftop.rope(
                        listOf("loc.rooftops_ardy_plank"),
                        56.0,
                        CoordGrid(2661, 3318, 3),
                        CoordGrid(2656, 3318, 3),
                        PLANK,
                    ),
                    Rooftop.leap(listOf("loc.rooftops_ardy_jump_2"), 15.0, CoordGrid(2653, 3314, 3)),
                    Rooftop.leap(listOf("loc.rooftops_ardy_jump_3"), 15.0, CoordGrid(2653, 3305, 3)),
                    Rooftop.rope(
                        listOf("loc.rooftops_ardy_wallcrossing"),
                        66.0,
                        CoordGrid(2654, 3300, 3),
                        CoordGrid(2654, 3297, 3),
                        BALANCE,
                    ),
                    Rooftop.leap(listOf("loc.rooftops_ardy_jump_4"), 583.0, CoordGrid(2656, 3293, 0)),
                ),
        )
}
