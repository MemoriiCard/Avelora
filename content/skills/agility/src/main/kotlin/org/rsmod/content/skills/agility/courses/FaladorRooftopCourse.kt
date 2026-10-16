package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.Fall
import org.rsmod.content.skills.agility.Rooftop
import org.rsmod.map.CoordGrid

object FaladorRooftopCourse {
    private val HANDHOLDS =
        Fall(FailChance(low = 180, high = 300), CoordGrid(3050, 3351, 0), 3..8, "You lose your grip and fall to the ground.")

    val course =
        AgilityCourse(
            id = 8,
            name = "Falador Rooftop",
            level = 50,
            lapVarp = "varp.agility_laps_falador",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(3047, 3343, 3),
                    CoordGrid(3050, 3356, 3),
                    CoordGrid(3027, 3355, 3),
                    CoordGrid(3016, 3348, 3),
                    CoordGrid(3015, 3333, 3),
                ),
            obstacles =
                listOf(
                    Rooftop.climb("loc.rooftops_falador_wallclimb", 11.0, CoordGrid(3036, 3342, 3)),
                    Rooftop.rope(
                        listOf("loc.rooftops_falador_tightrope_1"),
                        22.0,
                        CoordGrid(3040, 3343, 3),
                        CoordGrid(3047, 3343, 3),
                    ),
                    Rooftop.rope(
                        listOf("loc.rooftops_falador_handholds_start"),
                        61.0,
                        CoordGrid(3050, 3350, 3),
                        CoordGrid(3050, 3356, 3),
                        HANDHOLDS,
                        walk = "seq.human_walk_sidestepl",
                        ready = "seq.human_ready_sidestepl",
                    ),
                    Rooftop.leap(listOf("loc.rooftops_falador_gap_1"), 27.0, CoordGrid(3045, 3361, 3)),
                    Rooftop.leap(listOf("loc.rooftops_falador_gap_2"), 26.0, CoordGrid(3041, 3361, 3)),
                    Rooftop.rope(
                        listOf("loc.rooftops_falador_tightrope_2"),
                        61.0,
                        CoordGrid(3034, 3361, 3),
                        CoordGrid(3027, 3355, 3),
                    ),
                    Rooftop.rope(
                        listOf("loc.rooftops_falador_tightrope_3"),
                        53.0,
                        CoordGrid(3026, 3353, 3),
                        CoordGrid(3021, 3353, 3),
                    ),
                    Rooftop.leap(listOf("loc.rooftops_falador_gap_3"), 30.0, CoordGrid(3016, 3348, 3)),
                    Rooftop.leap(listOf("loc.rooftops_falador_ledge_1"), 14.0, CoordGrid(3012, 3343, 3)),
                    Rooftop.leap(listOf("loc.rooftops_falador_ledge_2"), 13.0, CoordGrid(3012, 3338, 3)),
                    Rooftop.leap(
                        listOf("loc.rooftops_falador_ledge_3a", "loc.rooftops_falador_ledge_3b"),
                        13.0,
                        CoordGrid(3015, 3333, 3),
                    ),
                    Rooftop.leap(listOf("loc.rooftops_falador_ledge_4"), 14.0, CoordGrid(3023, 3332, 3)),
                    Rooftop.leap(listOf("loc.rooftops_falador_edge"), 241.0, CoordGrid(3029, 3333, 0)),
                ),
        )
}
