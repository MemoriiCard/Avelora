package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.Fall
import org.rsmod.content.skills.agility.MarkChance
import org.rsmod.content.skills.agility.Rooftop
import org.rsmod.map.CoordGrid

object CanifisRooftopCourse {
    private val FALL_GAP_3 =
        Fall(FailChance(low = 178, high = 296), CoordGrid(3485, 3499, 0), 1..5, "You slip and fall to the ground.")

    val course =
        AgilityCourse(
            id = 7,
            name = "Canifis Rooftop",
            level = 40,
            lapVarp = "varp.agility_laps_canifis",
            bonusXp = 0.0,
            markChance = MarkChance.HIGH,
            markTiles =
                listOf(
                    CoordGrid(3506, 3492, 2),
                    CoordGrid(3499, 3504, 2),
                    CoordGrid(3488, 3499, 2),
                    CoordGrid(3477, 3495, 3),
                    CoordGrid(3492, 3474, 3),
                ),
            obstacles =
                listOf(
                    Rooftop.climb("loc.rooftops_canifis_start_tree", 10.0, CoordGrid(3506, 3492, 2)),
                    Rooftop.leap(listOf("loc.rooftops_canifis_jump"), 8.0, CoordGrid(3499, 3504, 2)),
                    Rooftop.leap(listOf("loc.rooftops_canifis_jump_2"), 8.0, CoordGrid(3488, 3499, 2)),
                    Rooftop.leap(listOf("loc.rooftops_canifis_jump_5"), 10.0, CoordGrid(3477, 3495, 3), FALL_GAP_3),
                    Rooftop.leap(listOf("loc.rooftops_canifis_jump_3"), 8.0, CoordGrid(3479, 3486, 2)),
                    Rooftop.vault("loc.rooftops_canifis_polevault", 10.0, CoordGrid(3492, 3474, 3)),
                    Rooftop.leap(listOf("loc.rooftops_canifis_jump_4"), 11.0, CoordGrid(3510, 3482, 2)),
                    Rooftop.leap(listOf("loc.rooftops_canifis_leapdown"), 175.0, CoordGrid(3510, 3485, 0)),
                ),
        )
}
