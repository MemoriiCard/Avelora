package org.rsmod.content.bosses.dagannothkings

import org.rsmod.map.CoordGrid

internal object WaterbirthLadders {
    val CENTRAL_ROOM = CoordGrid(2545, 10143, 0)
    val SUBLEVEL_ENTRANCE = CoordGrid(1799, 4406, 3)
    val KINGS_LAIR = CoordGrid(2900, 4449, 0)
    val KINGS_LADDER = CoordGrid(1912, 4367, 0)

    val SUBLEVEL_LADDERS: Map<String, CoordGrid> =
        mapOf(
            "loc.dagexp_ladder1" to CoordGrid(1810, 4405, 2),
            "loc.dagexp_ladder2" to CoordGrid(1807, 4405, 3),
            "loc.dagexp_ladder3" to CoordGrid(1822, 4404, 2),
            "loc.dagexp_ladder4" to CoordGrid(1825, 4404, 3),
            "loc.dagexp_ladder5" to CoordGrid(1834, 4387, 2),
            "loc.dagexp_ladder6" to CoordGrid(1834, 4390, 3),
            "loc.dagexp_ladder7" to CoordGrid(1809, 4394, 1),
            "loc.dagexp_ladder8" to CoordGrid(1812, 4394, 2),
            "loc.dagexp_ladder9" to CoordGrid(1799, 4386, 2),
            "loc.dagexp_ladder10" to CoordGrid(1799, 4389, 1),
            "loc.dagexp_ladder11" to CoordGrid(1799, 4382, 1),
            "loc.dagexp_ladder12" to CoordGrid(1796, 4382, 2),
            "loc.dagexp_ladder13" to CoordGrid(1800, 4369, 2),
            "loc.dagexp_ladder14" to CoordGrid(1803, 4369, 1),
            "loc.dagexp_ladder15" to CoordGrid(1828, 4362, 1),
            "loc.dagexp_ladder16" to CoordGrid(1825, 4362, 2),
            "loc.dagexp_ladder17" to CoordGrid(1863, 4373, 2),
            "loc.dagexp_ladder18" to CoordGrid(1863, 4370, 1),
            "loc.dagexp_ladder19" to CoordGrid(1864, 4390, 1),
            "loc.dagexp_ladder20" to CoordGrid(1864, 4387, 2),
            "loc.dagexp_ladder21" to CoordGrid(1890, 4409, 0),
            "loc.dagexp_ladder22" to CoordGrid(1890, 4406, 1),
            "loc.dagexp_ladder23" to CoordGrid(1957, 4373, 1),
            "loc.dagexp_ladder24" to CoordGrid(1957, 4370, 0),
            "loc.dagexp_ladder25" to CoordGrid(1824, 4379, 3),
            "loc.dagexp_ladder26" to CoordGrid(1824, 4382, 2),
            "loc.dagexp_ladder27" to CoordGrid(1838, 4374, 2),
            "loc.dagexp_ladder28" to CoordGrid(1838, 4377, 3),
            "loc.dagexp_ladder29" to CoordGrid(1850, 4384, 1),
            "loc.dagexp_ladder30" to CoordGrid(1850, 4387, 2),
            "loc.dagexp_ladder31" to CoordGrid(1932, 4377, 1),
            "loc.dagexp_ladder32" to CoordGrid(1932, 4380, 2),
            "loc.dagexp_ladder33" to CoordGrid(1960, 4391, 2),
            "loc.dagexp_ladder34" to CoordGrid(1961, 4393, 3),
        )

    val KINGS_LADDERS =
        listOf(
            "loc.dagexp_bossroomladder_down",
            "loc.dagexp_bossroomladder_down_normal",
            "loc.dagexp_bossroomladder_down_private",
        )
}
