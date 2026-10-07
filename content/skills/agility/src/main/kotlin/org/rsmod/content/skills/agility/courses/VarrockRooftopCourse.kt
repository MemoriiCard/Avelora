package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.balanceWalk
import org.rsmod.content.skills.agility.climbTo
import org.rsmod.content.skills.agility.fallTo
import org.rsmod.content.skills.agility.jumpTo
import org.rsmod.content.skills.agility.slips
import org.rsmod.content.skills.agility.stepTo
import org.rsmod.map.CoordGrid

object VarrockRooftopCourse {
    private val WALL_TOP = CoordGrid(3219, 3414, 3)
    private val LINE_START = CoordGrid(3214, 3414, 3)
    private val LINE_MIDDLE = CoordGrid(3211, 3414, 3)
    private val LINE_END = CoordGrid(3208, 3414, 3)
    private val LINE_FALL = CoordGrid(3211, 3414, 0)
    private val RUINS = CoordGrid(3197, 3416, 1)
    private val WALL_CORNER = CoordGrid(3190, 3414, 1)
    private val WALL_MIDDLE = CoordGrid(3190, 3410, 1)
    private val WALL_END = CoordGrid(3190, 3407, 1)
    private val WALL_FALL = CoordGrid(3190, 3410, 0)
    private val WALL_EXIT = CoordGrid(3192, 3406, 3)
    private val BALCONY = CoordGrid(3218, 3399, 3)
    private val LOWER_ROOF = CoordGrid(3236, 3403, 3)
    private const val SCRAMBLE_LANDING_Z = 3398
    private const val HURDLE_LANDING_Z = 3410
    private const val EDGE_LANDING_Z = 3417

    private val FAIL = FailChance(low = 190, high = 270)

    val course =
        AgilityCourse(
            id = 4,
            name = "Varrock Rooftop",
            level = 30,
            lapVarp = "varp.agility_laps_varrock",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(3219, 3412, 3),
                    CoordGrid(3195, 3404, 3),
                    CoordGrid(3218, 3395, 3),
                    CoordGrid(3238, 3406, 3),
                ),
            obstacles =
                listOf(
                    Obstacle(listOf("loc.rooftops_varrock_wallclimb"), xp = 13.5) {
                        climbTo(WALL_TOP)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_clothesline"), xp = 23.0) {
                        stepTo(LINE_START)
                        if (slips(FAIL)) {
                            balanceWalk(LINE_MIDDLE)
                            fallTo(LINE_FALL, 3..8, "You lose your footing and fall to the ground.")
                            return@Obstacle false
                        }
                        balanceWalk(LINE_END)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_leaptoruins"), xp = 19.0) {
                        climbTo(RUINS, anim = "seq.agility_shortcut_wall_jumpdown")
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_wallswing"), xp = 28.0) {
                        val sidestep = "seq.human_walk_sidestepl"
                        val ready = "seq.human_ready_sidestepl"
                        if (slips(FAIL)) {
                            balanceWalk(WALL_MIDDLE, sidestep, ready, via = listOf(WALL_CORNER))
                            fallTo(WALL_FALL, 2..5, "You lose your grip and fall to the ground.")
                            return@Obstacle false
                        }
                        balanceWalk(WALL_END, sidestep, ready, via = listOf(WALL_CORNER))
                        climbTo(WALL_EXIT)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_wallscramble"), xp = 10.0) {
                        val landing = CoordGrid(coords.x.coerceIn(3193, 3197), SCRAMBLE_LANDING_Z, 3)
                        jumpTo(landing, "seq.agility_pyramid_gap_jump", ticks = 2, constants.em_face_south)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_leaptobalcony"), xp = 24.5) {
                        jumpTo(BALCONY, "seq.agility_pyramid_gap_jump", ticks = 3, constants.em_face_east)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_leapdown"), xp = 4.5) {
                        jumpTo(LOWER_ROOF, "seq.agility_shortcut_wall_jumpdown", ticks = 2, constants.em_face_east)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_stepuproof"), xp = 3.5) {
                        val landing = CoordGrid(coords.x.coerceIn(3236, 3240), HURDLE_LANDING_Z, 3)
                        jumpTo(landing, "seq.human_jump_hurdle", ticks = 2, constants.em_face_north)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_varrock_finish"), xp = 143.7) {
                        val landing = CoordGrid(coords.x.coerceIn(3236, 3240), EDGE_LANDING_Z, 0)
                        climbTo(landing, anim = "seq.agility_shortcut_wall_jumpdown")
                        true
                    },
                ),
        )
}
