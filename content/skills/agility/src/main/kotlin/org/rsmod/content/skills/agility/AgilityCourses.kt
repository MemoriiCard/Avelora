package org.rsmod.content.skills.agility

import org.rsmod.content.skills.agility.courses.AlKharidRooftopCourse
import org.rsmod.content.skills.agility.courses.BarbarianOutpostCourse
import org.rsmod.content.skills.agility.courses.DraynorRooftopCourse
import org.rsmod.content.skills.agility.courses.GnomeStrongholdCourse
import org.rsmod.content.skills.agility.courses.VarrockRooftopCourse
import org.rsmod.content.skills.agility.courses.WildernessCourse

object AgilityCourses {
    val all: List<AgilityCourse> =
        listOf(
            GnomeStrongholdCourse.course,
            DraynorRooftopCourse.course,
            AlKharidRooftopCourse.course,
            VarrockRooftopCourse.course,
            WildernessCourse.course,
            BarbarianOutpostCourse.course,
        )
}
