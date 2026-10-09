package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.skills.agility.courses.CanifisRooftopCourse
import org.rsmod.content.skills.agility.courses.FaladorRooftopCourse
import org.rsmod.content.skills.agility.courses.PollnivneachRooftopCourse
import org.rsmod.content.skills.agility.courses.SeersRooftopCourse

@ResourceLock("server-cache")
class AgilityRooftopCoursesTest {
    private fun total(course: AgilityCourse) = course.obstacles.sumOf { it.xp } + course.bonusXp

    @Test
    fun `lap xp matches the wiki totals`() {
        assertEquals(240.0, total(CanifisRooftopCourse.course), 0.001)
        assertEquals(586.0, total(FaladorRooftopCourse.course), 0.001)
        assertEquals(570.0, total(SeersRooftopCourse.course), 0.001)
        assertEquals(890.0, total(PollnivneachRooftopCourse.course), 0.001)
    }

    @Test
    fun `course ids are unique`() {
        val ids = AgilityCourses.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `locs and lap varps exist`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (course in AgilityCourses.all) {
                for (loc in course.obstacles.flatMap { it.locs }) {
                    assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
                }
                assertNotNull(ServerCacheManager.getVarp(course.lapVarp.asRSCM(RSCMType.VARP)), course.lapVarp)
            }
        } finally {
            cache.close()
        }
    }
}
