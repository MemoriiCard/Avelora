package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.balanceWalk
import org.rsmod.content.skills.agility.climbTo
import org.rsmod.content.skills.agility.jumpTo
import org.rsmod.content.skills.agility.stepTo
import org.rsmod.map.CoordGrid

object BarbarianOutpostCourse {
    val ROPE_START = CoordGrid(2551, 3554, 0)
    val ROPE_END = CoordGrid(2551, 3549, 0)
    val LOG_START = CoordGrid(2551, 3546, 0)
    val LOG_END = CoordGrid(2541, 3546, 0)
    val NET_START = CoordGrid(2539, 3546, 0)
    val NET_TOP = CoordGrid(2538, 3547, 1)
    val LEDGE_END = CoordGrid(2532, 3547, 1)
    val LADDER_EXIT = CoordGrid(2532, 3546, 0)
    val WALL_Z = 3553
    val WALLS = listOf(2536, 2539, 2542)
    val PIPE_NORTH = CoordGrid(2552, 3560, 0)
    val PIPE_SOUTH = CoordGrid(2552, 3557, 0)

    const val PIPE_LEVEL = 35
    const val LAP_BONUS = 46.3
    const val LAP_STRENGTH = 41.3

    val course =
        AgilityCourse(
            id = 6,
            name = "Barbarian Outpost",
            level = 1,
            lapVarp = "varp.agility_laps_barbarian",
            bonusXp = LAP_BONUS,
            markTiles = emptyList(),
            onLap = { statAdvance("stat.strength", LAP_STRENGTH) },
            obstacles =
                listOf(
                    Obstacle(listOf("loc.obstical_ropeswing1"), xp = 22.0) {
                        if (coords.z < ROPE_END.z + 2) return@Obstacle false
                        stepTo(ROPE_START)
                        jumpTo(ROPE_END, "seq.human_ropeswing_long", ticks = 4, constants.em_face_south)
                        true
                    },
                    Obstacle(listOf("loc.barbarian_log_balance1"), xp = 13.7) {
                        stepTo(LOG_START)
                        balanceWalk(LOG_END)
                        true
                    },
                    Obstacle(listOf("loc.agility_obstical_net_barbarian"), xp = 8.2) {
                        stepTo(NET_START)
                        climbTo(NET_TOP, anim = "seq.human_largenet", ticks = 3)
                        true
                    },
                    Obstacle(listOf("loc.balancing_ledge1"), xp = 22.0) {
                        balanceWalk(LEDGE_END, "seq.human_ledge_walk_left", "seq.human_ledge_on_left")
                        true
                    },
                    Obstacle(listOf("loc.barbarian_laddertop_norim"), xp = 0.0) {
                        climbTo(LADDER_EXIT, anim = "seq.human_reachforladdertop", ticks = 2)
                        true
                    },
                ) +
                    WALLS.map { x ->
                        Obstacle(listOf("loc.castlecrumbly1"), xp = 13.7, at = CoordGrid(x, WALL_Z, 0)) {
                            if (coords.x > x) return@Obstacle false
                            climbTo(CoordGrid(x + 1, WALL_Z, 0), anim = "seq.human_walk_crumbledwall", ticks = 2)
                            true
                        }
                    },
        )
}
