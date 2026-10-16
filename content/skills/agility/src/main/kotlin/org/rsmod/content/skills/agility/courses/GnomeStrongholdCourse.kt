package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.balanceWalk
import org.rsmod.content.skills.agility.climbTo
import org.rsmod.content.skills.agility.stepTo
import org.rsmod.map.CoordGrid

object GnomeStrongholdCourse {
    private val LOG_START = CoordGrid(2474, 3436, 0)
    private val LOG_END = CoordGrid(2474, 3429, 0)
    private val BRANCH_TOP = CoordGrid(2473, 3420, 2)
    private val ROPE_START = CoordGrid(2477, 3420, 2)
    private val ROPE_END = CoordGrid(2483, 3420, 2)
    private val TREE_BOTTOM = CoordGrid(2486, 3420, 0)
    private const val FIRST_NET_Z = 3425
    private const val SECOND_NET_Z = 3426
    private const val PIPE_ENTRY_Z = 3430
    private const val PIPE_EXIT_Z = 3437

    val course =
        AgilityCourse(
            id = 1,
            name = "Gnome Stronghold",
            level = 1,
            lapVarp = "varp.agility_laps_gnome",
            bonusXp = 50.0,
            markTiles = listOf(CoordGrid(2474, 3428, 0), CoordGrid(2486, 3428, 0)),
            obstacles =
                listOf(
                    Obstacle(listOf("loc.gnome_log_balance1"), xp = 10.0) {
                        stepTo(LOG_START)
                        mes("You walk carefully across the slippery log...")
                        balanceWalk(LOG_END)
                        mes("...You make it safely to the other side.")
                        true
                    },
                    Obstacle(listOf("loc.obstical_net2"), xp = 10.0) {
                        if (coords.z <= FIRST_NET_Z) return@Obstacle false
                        mes("You climb the netting.")
                        climbTo(CoordGrid(coords.x.coerceIn(2471, 2476), 3424, 1))
                        true
                    },
                    Obstacle(listOf("loc.climbing_branch"), xp = 6.5) {
                        mes("You climb the tree...")
                        climbTo(BRANCH_TOP)
                        mes("...To the platform above.")
                        true
                    },
                    Obstacle(listOf("loc.balancing_rope"), xp = 10.0) {
                        if (coords.x > ROPE_START.x) return@Obstacle false
                        stepTo(ROPE_START)
                        balanceWalk(ROPE_END)
                        mes("You carefully cross the tightrope.")
                        true
                    },
                    Obstacle(listOf("loc.climbing_tree", "loc.climbing_tree2"), xp = 6.5) {
                        mes("You climb down the tree...")
                        climbTo(TREE_BOTTOM, anim = "seq.human_climbing_down")
                        mes("You land on the ground.")
                        true
                    },
                    Obstacle(listOf("loc.obstical_net3"), xp = 10.0) {
                        if (coords.z >= SECOND_NET_Z) return@Obstacle false
                        mes("You climb the netting.")
                        climbTo(CoordGrid(coords.x.coerceIn(2483, 2488), 3427, 0))
                        true
                    },
                    Obstacle(listOf("loc.obstical_pipe3_1", "loc.obstical_pipe3_2"), xp = 7.5) { pipe ->
                        if (pipe.coords.z > PIPE_ENTRY_Z + 1) return@Obstacle false
                        val start = CoordGrid(pipe.coords.x, PIPE_ENTRY_Z, 0)
                        val end = CoordGrid(pipe.coords.x, PIPE_EXIT_Z, 0)
                        stepTo(start)
                        anim("seq.human_doublepipesqueeze")
                        exactMove(start, end, delay1 = 30, delay2 = 210, dir = constants.em_face_north, TeleportType.Exempt)
                        delay(7)
                        mes("You pull yourself through the pipes.")
                        true
                    },
                ),
        )
}
