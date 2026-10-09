package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.Fall
import org.rsmod.content.skills.agility.Rooftop
import org.rsmod.map.CoordGrid

object SeersRooftopCourse {
    private val FAIL = FailChance(low = 190, high = 270)
    private val GAP_1 = Fall(FAIL, CoordGrid(2720, 3492, 0), 1..5, "You slip and fall to the ground.")
    private val ROPE = Fall(FAIL, CoordGrid(2710, 3485, 0), 1..5, "You lose your footing and fall to the ground.")

    val course =
        AgilityCourse(
            id = 9,
            name = "Seers' Village Rooftop",
            level = 60,
            lapVarp = "varp.agility_laps_seers",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(2727, 3493, 3),
                    CoordGrid(2713, 3493, 2),
                    CoordGrid(2710, 3481, 2),
                    CoordGrid(2711, 3471, 3),
                    CoordGrid(2701, 3464, 2),
                ),
            obstacles =
                listOf(
                    Rooftop.climb("loc.rooftops_seers_wallclimb", 45.0, CoordGrid(2727, 3493, 3)),
                    Rooftop.leap(listOf("loc.rooftops_seers_jump"), 20.0, CoordGrid(2713, 3493, 2), GAP_1),
                    Rooftop.rope(
                        listOf("loc.rooftops_seers_tightrope"),
                        20.0,
                        CoordGrid(2710, 3489, 2),
                        CoordGrid(2710, 3481, 2),
                        ROPE,
                    ),
                    Rooftop.leap(listOf("loc.rooftops_seers_jump_1"), 35.0, CoordGrid(2711, 3471, 3)),
                    Rooftop.leap(listOf("loc.rooftops_seers_jump_2"), 15.0, CoordGrid(2701, 3464, 2)),
                    Rooftop.leap(listOf("loc.rooftops_seers_leapdown"), 435.0, CoordGrid(2704, 3460, 0)),
                ),
        )
}
