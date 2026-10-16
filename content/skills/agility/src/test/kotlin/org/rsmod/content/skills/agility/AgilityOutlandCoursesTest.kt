package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.skills.agility.courses.BarbarianOutpostCourse
import org.rsmod.content.skills.agility.courses.WildernessCourse

@ResourceLock("server-cache")
class AgilityOutlandCoursesTest {
    @Test
    fun `wilderness lap pays the wiki total`() {
        val course = WildernessCourse.course
        assertEquals(571.4, course.obstacles.sumOf { it.xp } + course.bonusXp, 0.001)
        assertEquals(5, course.obstacles.size)
    }

    @Test
    fun `barbarian outpost lap pays the wiki total`() {
        val course = BarbarianOutpostCourse.course
        assertEquals(107.0, course.obstacles.sumOf { it.xp }, 0.001)
        assertEquals(153.3, course.obstacles.sumOf { it.xp } + course.bonusXp, 0.001)
        assertEquals(8, course.obstacles.size)
    }

    @Test
    fun `crumbling walls share a loc but are told apart by position`() {
        val walls = BarbarianOutpostCourse.course.obstacles.filter { "loc.castlecrumbly1" in it.locs }
        assertEquals(3, walls.size)
        assertEquals(3, walls.mapNotNull { it.at }.distinct().size)
    }

    @Test
    fun `wilderness fall damage follows the wiki formulas`() {
        assertEquals(15, WildernessCourse.ropeFallDamage(99))
        assertEquals(1, WildernessCourse.ropeFallDamage(0))
        assertEquals(20, WildernessCourse.stoneFallDamage(99))
    }

    @Test
    fun `tickets pay more in bulk`() {
        assertEquals(200.0, AgilityExtrasScript.tokenXp(1))
        assertEquals(20_000.0, AgilityExtrasScript.tokenXp(100))
        assertEquals(23_230.0, AgilityExtrasScript.tokenXp(101), 0.001)
    }

    @Test
    fun `dispenser and pipe locs and ticket exist`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (loc in listOf("loc.wildy_agility_pillar", "loc.agility_obstical_pipe_barbarian")) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
            assertNotNull(ServerCacheManager.getItem(AgilityExtrasScript.TOKEN.asRSCM(RSCMType.OBJ)))
            assertNotNull(ServerCacheManager.getVarp("varp.agility_wilderness_tag".asRSCM(RSCMType.VARP)))
        } finally {
            cache.close()
        }
    }
}
