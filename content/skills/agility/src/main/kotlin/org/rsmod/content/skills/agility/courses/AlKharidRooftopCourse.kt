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

object AlKharidRooftopCourse {
    private val WALL_TOP = CoordGrid(3273, 3192, 3)
    private val ROPE1_START = CoordGrid(3272, 3182, 3)
    private val ROPE1_MIDDLE = CoordGrid(3272, 3177, 3)
    private val ROPE1_END = CoordGrid(3272, 3172, 3)
    private val ROPE1_FALL = CoordGrid(3272, 3177, 0)
    private val CABLE_END = CoordGrid(3284, 3166, 3)
    private val ZIP_START = CoordGrid(3301, 3163, 3)
    private val ZIP_END = CoordGrid(3315, 3163, 1)
    private val ZIP_FALL = CoordGrid(3307, 3163, 0)
    private val TREE_END = CoordGrid(3317, 3174, 2)
    private val BEAMS_TOP = CoordGrid(3316, 3180, 3)
    private val ROPE2_START = CoordGrid(3313, 3186, 3)
    private val ROPE2_END = CoordGrid(3302, 3186, 3)
    private val GAP_BOTTOM = CoordGrid(3299, 3194, 0)

    private val FAIL = FailChance(low = 200, high = 280)
    private val FALL_DAMAGE = 1..5

    val course =
        AgilityCourse(
            id = 3,
            name = "Al Kharid Rooftop",
            level = 20,
            lapVarp = "varp.agility_laps_alkharid",
            bonusXp = 0.0,
            markTiles =
                listOf(
                    CoordGrid(3274, 3190, 3),
                    CoordGrid(3269, 3171, 3),
                    CoordGrid(3285, 3163, 3),
                    CoordGrid(3316, 3183, 3),
                    CoordGrid(3302, 3188, 3),
                ),
            obstacles =
                listOf(
                    Obstacle(listOf("loc.rooftops_kharid_wallclimb"), xp = 12.0) {
                        climbTo(WALL_TOP)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_tightrope_1"), xp = 36.0) {
                        stepTo(ROPE1_START)
                        if (slips(FAIL)) {
                            balanceWalk(ROPE1_MIDDLE)
                            fallTo(ROPE1_FALL, FALL_DAMAGE, "You lose your footing and fall to the ground.")
                            return@Obstacle false
                        }
                        balanceWalk(ROPE1_END)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_rope_swing"), xp = 48.0) {
                        jumpTo(CABLE_END, "seq.human_ropeswing_long", ticks = 3, constants.em_face_east)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_slide_side"), xp = 48.0) {
                        stepTo(ZIP_START)
                        anim("seq.zipline_bite")
                        delay(1)
                        if (slips(FAIL)) {
                            fallTo(ZIP_FALL, FALL_DAMAGE, "You lose your grip and fall to the ground.")
                            return@Obstacle false
                        }
                        climbTo(ZIP_END, anim = "seq.zipline_slide", ticks = 3)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_bamboo_tree_top"), xp = 12.0) {
                        climbTo(TREE_END, anim = "seq.human_ropeswing")
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_wallclimb_2"), xp = 6.0) {
                        climbTo(BEAMS_TOP)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_tightrope_4"), xp = 18.0) {
                        stepTo(ROPE2_START)
                        balanceWalk(ROPE2_END)
                        true
                    },
                    Obstacle(listOf("loc.rooftops_kharid_leapdown"), xp = 36.0) {
                        climbTo(GAP_BOTTOM, anim = "seq.agility_shortcut_wall_jumpdown")
                        true
                    },
                ),
        )
}
