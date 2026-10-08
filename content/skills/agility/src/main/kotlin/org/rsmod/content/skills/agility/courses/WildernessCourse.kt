package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.FailChance
import org.rsmod.content.skills.agility.MarkChance
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.balanceWalk
import org.rsmod.content.skills.agility.climbTo
import org.rsmod.content.skills.agility.fallTo
import org.rsmod.content.skills.agility.jumpTo
import org.rsmod.content.skills.agility.slips
import org.rsmod.content.skills.agility.stepTo
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal var Player.wildernessTag by intVarp("varp.agility_wilderness_tag")

object WildernessCourse {
    val PIPE_START = CoordGrid(3004, 3937, 0)
    val PIPE_END = CoordGrid(3004, 3950, 0)
    val ROPE_START = CoordGrid(3005, 3953, 0)
    val ROPE_END = CoordGrid(3005, 3958, 0)
    val ROPE_FALL = CoordGrid(3004, 10357, 0)
    val STONES_START = CoordGrid(3002, 3960, 0)
    val STONE_BANK_FALL = CoordGrid(3002, 3961, 0)
    val LOG_START = CoordGrid(3002, 3945, 0)
    val LOG_MIDDLE = CoordGrid(2998, 3945, 0)
    val LOG_END = CoordGrid(2994, 3945, 0)
    val LOG_FALL = CoordGrid(2998, 10345, 0)
    val ROCKS_START = CoordGrid(2994, 3937, 0)
    val ROCKS_END = CoordGrid(2994, 3933, 0)

    const val STONE_COUNT = 6
    const val STONE_FAIL_INDEX = 2
    const val LEVEL = 52

    private val FAIL = FailChance(low = 190, high = 270)

    fun ropeFallDamage(hitpoints: Int): Int = hitpoints * 15 / 100 + 1

    fun stoneFallDamage(hitpoints: Int): Int = hitpoints * 20 / 100 + 1

    val course =
        AgilityCourse(
            id = 5,
            name = "Wilderness",
            level = LEVEL,
            lapVarp = "varp.agility_laps_wilderness",
            bonusXp = 0.0,
            markTiles = emptyList(),
            markChance = MarkChance.STANDARD,
            onLap = { player.wildernessTag = 1 },
            obstacles =
                listOf(
                    Obstacle(listOf("loc.obstical_pipe2"), xp = 12.5) {
                        val entering = coords.z < PIPE_END.z - 4
                        if (!entering) return@Obstacle false
                        stepTo(PIPE_START)
                        balanceWalk(PIPE_END, "seq.human_pipesqueeze", "seq.human_pipesqueeze_ready")
                        true
                    },
                    Obstacle(listOf("loc.obstical_ropeswing2"), xp = 20.0) {
                        stepTo(ROPE_START)
                        if (slips(FAIL)) {
                            jumpTo(ROPE_START.translate(0, 2), "seq.human_ropeswing_long", ticks = 2, constants.em_face_north)
                            val damage = ropeFallDamage(player.hitpoints)
                            fallTo(ROPE_FALL, damage..damage, "You slip and fall to the pit below.")
                            return@Obstacle false
                        }
                        jumpTo(ROPE_END, "seq.human_ropeswing_long", ticks = 4, constants.em_face_north)
                        true
                    },
                    Obstacle(listOf("loc.steppingstone1"), xp = 20.0) {
                        stepTo(STONES_START)
                        for (stone in 1..STONE_COUNT) {
                            val tile = STONES_START.translate(-stone, 0)
                            if (stone == STONE_FAIL_INDEX + 1 && slips(FAIL)) {
                                jumpTo(tile, "seq.human_steppingstonejump", ticks = 1, constants.em_face_west)
                                val damage = stoneFallDamage(player.hitpoints)
                                fallTo(STONE_BANK_FALL, damage..damage, "You slip and fall into the lava.")
                                return@Obstacle false
                            }
                            jumpTo(tile, "seq.human_steppingstonejump", ticks = 1, constants.em_face_west)
                        }
                        true
                    },
                    Obstacle(listOf("loc.wilderness_log_balance1"), xp = 20.0) {
                        stepTo(LOG_START)
                        if (slips(FAIL)) {
                            balanceWalk(LOG_MIDDLE)
                            val damage = ropeFallDamage(player.hitpoints)
                            fallTo(LOG_FALL, damage..damage, "You slip off the log and fall to the pit below.")
                            return@Obstacle false
                        }
                        balanceWalk(LOG_END)
                        true
                    },
                    Obstacle(listOf("loc.wildclimbingrock"), xp = 498.9) {
                        stepTo(ROCKS_START)
                        climbTo(ROCKS_END, anim = "seq.human_climbing", ticks = 5)
                        true
                    },
                ),
        )
}
