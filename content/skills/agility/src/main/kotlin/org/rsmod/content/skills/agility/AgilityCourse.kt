package org.rsmod.content.skills.agility

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

/**
 * Runs the obstacle's movement. Returns false when the player is on the wrong side or otherwise
 * can't take it, in which case no experience or course progress is given.
 */
typealias Traverse = suspend ProtectedAccess.(loc: BoundLocInfo) -> Boolean

class Obstacle(
    val locs: List<String>,
    val xp: Double,
    val level: Int = 1,
    val at: CoordGrid? = null,
    val traverse: Traverse,
)

/**
 * An agility course: obstacles in lap order, ending with the obstacle that awards [bonusXp], a lap
 * and a mark of grace roll when every earlier obstacle was cleared since the last lap.
 *
 * [id] tags the shared progress varbit so obstacles from two courses never count toward each
 * other's lap. It must be unique and stay stable, since it's saved with the player.
 */
class AgilityCourse(
    val id: Int,
    val name: String,
    val level: Int,
    val lapVarp: String,
    val bonusXp: Double,
    val obstacles: List<Obstacle>,
    val markTiles: List<CoordGrid>,
    val markChance: MarkChance = MarkChance.STANDARD,
    val onLap: (ProtectedAccess.() -> Unit)? = null,
) {
    val allObstaclesMask: Int
        get() = (1 shl obstacles.size) - 1
}

data class CourseProgress(val courseId: Int, val mask: Int)

object CourseProgression {
    /** Progress after clearing obstacle [index] of [course], starting from [current]. */
    fun advance(current: CourseProgress, course: AgilityCourse, index: Int): CourseProgress {
        val bit = 1 shl index
        if (index == 0 || current.courseId != course.id) {
            return CourseProgress(course.id, bit)
        }
        return CourseProgress(course.id, current.mask or bit)
    }

    fun isLapComplete(progress: CourseProgress, course: AgilityCourse): Boolean =
        progress.courseId == course.id && progress.mask == course.allObstaclesMask
}
