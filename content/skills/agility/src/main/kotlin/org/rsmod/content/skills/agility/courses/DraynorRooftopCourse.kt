package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.balanceWalk
import org.rsmod.content.skills.agility.climbTo
import org.rsmod.content.skills.agility.jumpTo
import org.rsmod.content.skills.agility.stepTo
import org.rsmod.map.CoordGrid

object DraynorRooftopCourse {
    private val WALL_TOP = CoordGrid(3102, 3279, 3)
    private val ROPE1_START = CoordGrid(3099, 3277, 3)
    private val ROPE1_END = CoordGrid(3090, 3277, 3)
    private val ROPE2_START = CoordGrid(3092, 3276, 3)
    private val ROPE2_END = CoordGrid(3092, 3266, 3)
    private val LEDGE_START = CoordGrid(3089, 3265, 3)
    private val LEDGE_END = CoordGrid(3088, 3261, 3)
    private val WALL_JUMP_START = CoordGrid(3088, 3257, 3)
    private val WALL_JUMP_END = CoordGrid(3088, 3255, 3)
    private val GAP_START = CoordGrid(3094, 3255, 3)
    private val GAP_END = CoordGrid(3096, 3256, 3)
    private val CRATE_BOTTOM = CoordGrid(3103, 3261, 0)

    val course =
        AgilityCourse(
            id = 2,
            name = "Draynor Village Rooftop",
            level = 1,
            lapVarp = "varp.agility_laps_draynor",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(3099, 3279, 3),
                    CoordGrid(3088, 3274, 3),
                    CoordGrid(3092, 3266, 3),
                    CoordGrid(3099, 3259, 3),
                ),
            obstacles =
                listOf(
                    Obstacle(listOf("loc.rooftops_draynor_wallclimb"), xp = 5.0) {
                        climbTo(WALL_TOP)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_draynor_tightrope_1"), xp = 8.0) {
                        stepTo(ROPE1_START)
                        balanceWalk(ROPE1_END)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_draynor_tightrope_2"), xp = 7.0) {
                        stepTo(ROPE2_START)
                        balanceWalk(ROPE2_END)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_draynor_wallcrossing"), xp = 7.0) {
                        stepTo(LEDGE_START)
                        balanceWalk(
                            LEDGE_END,
                            walk = "seq.human_walk_sidestepl",
                            ready = "seq.human_ready_sidestep",
                            via = listOf(CoordGrid(3089, 3262, 3)),
                        )
                        true
                    },
                    Obstacle(listOf("loc.rooftops_draynor_wallscramble"), xp = 10.0) {
                        stepTo(WALL_JUMP_START)
                        jumpTo(WALL_JUMP_END, "seq.agility_shortcut_wall_jump2", ticks = 2, constants.em_face_south)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_draynor_leapdown"), xp = 4.0) {
                        stepTo(GAP_START)
                        jumpTo(GAP_END, "seq.agility_shortcut_wall_jumpdown", ticks = 2, constants.em_face_east)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_draynor_crate"), xp = 79.0) {
                        climbTo(CRATE_BOTTOM, anim = "seq.agility_shortcut_wall_jumpdown")
                        true
                    },
                ),
        )
}
