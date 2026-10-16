package org.rsmod.content.travel.spirittrees

import org.rsmod.map.CoordGrid

class SpiritTreeDestination(val name: String, val coords: CoordGrid)

object SpiritTrees {
    val destinations =
        listOf(
            SpiritTreeDestination("Tree Gnome Village", CoordGrid(2542, 3170, 0)),
            SpiritTreeDestination("Tree Gnome Stronghold", CoordGrid(2461, 3444, 0)),
            SpiritTreeDestination("Battlefield of Khazard", CoordGrid(2555, 3259, 0)),
            SpiritTreeDestination("Grand Exchange", CoordGrid(3185, 3508, 0)),
            SpiritTreeDestination("Feldip Hills", CoordGrid(2488, 2850, 0)),
            SpiritTreeDestination("Prifddinas", CoordGrid(3274, 6123, 0)),
        )
}
