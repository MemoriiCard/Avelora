package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.courseId by intVarBit("varbit.agility_course_id")
private var Player.courseProgress by intVarBit("varbit.agility_course_progress")
private var Player.nextMarkMinute by intVarp("varp.agility_mark_cooldown")

class AgilityCourseScript @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        val byLoc = mutableMapOf<String, MutableList<Entry>>()
        for (course in AgilityCourses.all) {
            course.obstacles.forEachIndexed { index, obstacle ->
                for (loc in obstacle.locs) {
                    byLoc.getOrPut(loc) { mutableListOf() } += Entry(course, index, obstacle)
                }
            }
        }
        for ((loc, entries) in byLoc) {
            onOpLoc1(loc) {
                val entry = entries.firstOrNull { e -> e.obstacle.at == null || e.obstacle.at == it.loc.coords }
                if (entry != null) attempt(entry.course, entry.index, entry.obstacle, it.loc)
            }
        }
    }

    private class Entry(val course: AgilityCourse, val index: Int, val obstacle: Obstacle)

    private suspend fun ProtectedAccess.attempt(
        course: AgilityCourse,
        index: Int,
        obstacle: Obstacle,
        loc: BoundLocInfo,
    ) {
        val required = maxOf(course.level, obstacle.level)
        if (player.agilityLvl < required) {
            mes("You need an Agility level of $required to attempt this.")
            return
        }
        if (!obstacle.traverse(this, loc)) {
            return
        }
        statAdvance(STAT, obstacle.xp)

        val progress =
            CourseProgression.advance(CourseProgress(player.courseId, player.courseProgress), course, index)
        player.courseId = progress.courseId
        player.courseProgress = progress.mask
        if (index == course.obstacles.lastIndex && CourseProgression.isLapComplete(progress, course)) {
            completeLap(course)
        }
    }

    private fun ProtectedAccess.completeLap(course: AgilityCourse) {
        player.courseProgress = 0
        statAdvance(STAT, course.bonusXp)
        val laps = vars[course.lapVarp] + 1
        vars[course.lapVarp] = laps
        mes("Your ${course.name} Agility lap count is: <col=ff0000>$laps</col>.")
        course.onLap?.invoke(this)
        rollMarkOfGrace(course)
    }

    private fun ProtectedAccess.rollMarkOfGrace(course: AgilityCourse) {
        val minute = (System.currentTimeMillis() / 60_000).toInt()
        if (course.markTiles.isEmpty() || !MarksOfGrace.isOffCooldown(minute, player.nextMarkMinute)) {
            return
        }
        val spawned =
            MarksOfGrace.rolls(course.markChance, course.level, statBase(STAT)) { random.of(it) }
        if (!spawned) {
            return
        }
        player.nextMarkMinute = minute + MarksOfGrace.COOLDOWN_MINUTES
        val tile = random.pick(course.markTiles)
        objRepo.add(
            MarksOfGrace.OBJ,
            tile,
            duration = MarksOfGrace.DESPAWN_TICKS,
            receiver = player,
            reveal = MarksOfGrace.DESPAWN_TICKS,
        )
    }

    private companion object {
        const val STAT = "stat.agility"
    }
}
