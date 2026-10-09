package org.rsmod.content.skills.agility

import org.rsmod.content.skills.agility.courses.AlKharidRooftopCourse
import org.rsmod.content.skills.agility.courses.ArdougneRooftopCourse
import org.rsmod.content.skills.agility.courses.BarbarianOutpostCourse
import org.rsmod.content.skills.agility.courses.CanifisRooftopCourse
import org.rsmod.content.skills.agility.courses.DraynorRooftopCourse
import org.rsmod.content.skills.agility.courses.FaladorRooftopCourse
import org.rsmod.content.skills.agility.courses.GnomeStrongholdCourse
import org.rsmod.content.skills.agility.courses.PollnivneachRooftopCourse
import org.rsmod.content.skills.agility.courses.SeersRooftopCourse
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
            PollnivneachRooftopCourse.course,
            SeersRooftopCourse.course,
            FaladorRooftopCourse.course,
            CanifisRooftopCourse.course,
            ArdougneRooftopCourse.course,
        )
}
