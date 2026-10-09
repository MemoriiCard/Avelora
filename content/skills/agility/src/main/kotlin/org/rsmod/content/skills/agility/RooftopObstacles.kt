package org.rsmod.content.skills.agility

import kotlin.math.abs
import org.rsmod.api.config.constants
import org.rsmod.map.CoordGrid

class Fall(val chance: FailChance, val to: CoordGrid, val damage: IntRange, val message: String)

object Rooftop {
    private const val GAP_ANIM = "seq.agility_pyramid_gap_jump"
    private const val DROP_ANIM = "seq.agility_shortcut_wall_jumpdown"
    private const val CLIMB_ANIM = "seq.human_reachforladder"
    private const val VAULT_ANIM = "seq.human_longjump"

    fun climb(loc: String, xp: Double, dest: CoordGrid) =
        Obstacle(listOf(loc), xp) {
            climbTo(dest, CLIMB_ANIM)
            true
        }

    fun leap(
        locs: List<String>,
        xp: Double,
        dest: CoordGrid,
        fall: Fall? = null,
        anim: String = GAP_ANIM,
    ) =
        Obstacle(locs, xp) {
            if (fall != null && slips(fall.chance)) {
                fallTo(fall.to, fall.damage, fall.message)
                return@Obstacle false
            }
            if (dest.level != coords.level) {
                climbTo(dest, DROP_ANIM)
            } else {
                jumpTo(dest, anim, ticks = leapTicks(coords, dest), faceTowards(coords, dest))
            }
            true
        }

    fun vault(loc: String, xp: Double, dest: CoordGrid) = leap(listOf(loc), xp, dest, anim = VAULT_ANIM)

    fun rope(
        locs: List<String>,
        xp: Double,
        start: CoordGrid,
        end: CoordGrid,
        fall: Fall? = null,
        walk: String = "seq.human_walk_logbalance",
        ready: String = "seq.human_walk_logbalance_ready",
        fallAt: CoordGrid? = null,
    ) =
        Obstacle(locs, xp) {
            stepTo(start)
            if (fall != null && slips(fall.chance)) {
                balanceWalk(fallAt ?: midpoint(start, end), walk, ready)
                fallTo(fall.to, fall.damage, fall.message)
                return@Obstacle false
            }
            balanceWalk(end, walk, ready)
            true
        }

    fun leapTicks(from: CoordGrid, to: CoordGrid): Int = (from.chebyshevDistance(to) / 3 + 2).coerceIn(2, 5)

    fun midpoint(a: CoordGrid, b: CoordGrid): CoordGrid = CoordGrid((a.x + b.x) / 2, (a.z + b.z) / 2, a.level)

    fun faceTowards(from: CoordGrid, to: CoordGrid): Int {
        val dx = to.x - from.x
        val dz = to.z - from.z
        return if (abs(dx) >= abs(dz)) {
            if (dx >= 0) constants.em_face_east else constants.em_face_west
        } else {
            if (dz >= 0) constants.em_face_north else constants.em_face_south
        }
    }
}
