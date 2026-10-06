package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rsmod.content.skills.agility.courses.GnomeStrongholdCourse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgilityCourseTest {
    private val gnome = GnomeStrongholdCourse.course

    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
    }

    @Test
    fun `every obstacle loc exists`() {
        for (loc in AgilityCourses.all.flatMap { c -> c.obstacles.flatMap { it.locs } }) {
            assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
        }
        assertNotNull(ServerCacheManager.getItem(MarksOfGrace.OBJ.asRSCM(RSCMType.OBJ)))
    }

    @Test
    fun `course state vars are packed into the cache`() {
        val varps = listOf("varp.agility_course_state", "varp.agility_mark_cooldown") + AgilityCourses.all.map { it.lapVarp }
        for (varp in varps) {
            assertNotNull(ServerCacheManager.getVarp(varp.asRSCM(RSCMType.VARP)), varp)
        }
        for (varbit in listOf("varbit.agility_course_progress", "varbit.agility_course_id")) {
            assertNotNull(ServerCacheManager.getVarbit(varbit.asRSCM(RSCMType.VARBIT)), varbit)
        }
    }

    @Test
    fun `gnome lap pays the wiki total`() {
        assertEquals(110.5, gnome.obstacles.sumOf { it.xp } + gnome.bonusXp)
    }

    @Test
    fun `draynor lap pays the wiki total and course ids are unique`() {
        val draynor = AgilityCourses.all.single { it.name.startsWith("Draynor") }
        assertEquals(120.0, draynor.obstacles.sumOf { it.xp } + draynor.bonusXp)
        assertEquals(AgilityCourses.all.size, AgilityCourses.all.map { it.id }.distinct().size)
    }

    @Test
    fun `a lap only completes when every obstacle was cleared in this course`() {
        var progress = CourseProgress(0, 0)
        for (index in gnome.obstacles.indices) {
            progress = CourseProgression.advance(progress, gnome, index)
        }
        assertTrue(CourseProgression.isLapComplete(progress, gnome))

        var skipped = CourseProgress(0, 0)
        for (index in gnome.obstacles.indices) {
            if (index != 3) skipped = CourseProgression.advance(skipped, gnome, index)
        }
        assertFalse(CourseProgression.isLapComplete(skipped, gnome))
    }

    @Test
    fun `starting the first obstacle resets progress and other courses don't count`() {
        val partial = CourseProgress(gnome.id, 0b111)
        assertEquals(CourseProgress(gnome.id, 1), CourseProgression.advance(partial, gnome, 0))
        val otherCourse = CourseProgress(gnome.id + 1, 0b111111)
        assertEquals(CourseProgress(gnome.id, 1 shl 6), CourseProgression.advance(otherCourse, gnome, 6))
    }

    @Test
    fun `marks are five times rarer twenty levels above the course`() {
        var bound = 0
        MarksOfGrace.rolls(MarkChance.STANDARD, courseLevel = 1, baseLevel = 20) { bound = it; 0 }
        assertEquals(3, bound)
        MarksOfGrace.rolls(MarkChance.STANDARD, courseLevel = 1, baseLevel = 21) { bound = it; 0 }
        assertEquals(15, bound)
        assertFalse(MarksOfGrace.rolls(MarkChance.STANDARD, 1, 1) { 1 })
        assertTrue(MarksOfGrace.isOffCooldown(nowMinute = 10, nextAllowedMinute = 10))
    }
}
