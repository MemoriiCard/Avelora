package org.rsmod.content.travel.gliders

import org.rsmod.map.CoordGrid

enum class GliderDestination(
    val code: Int,
    val label: String,
    val button: String,
    val coords: CoordGrid,
) {
    GrandTree(0, "Ta Quir Priw", "grandtree_button", CoordGrid(2464, 3501, 3)),
    Karamja(1, "Gandius", "karamja_button", CoordGrid(2970, 2973, 0)),
    AlKharid(2, "Kar-Hewo", "alkharid_button", CoordGrid(3284, 3212, 0)),
    Digsite(3, "Lemanto Andra", "varrock_button", CoordGrid(3321, 3431, 0)),
    WhiteWolf(4, "Sindarpos", "whitewolfmountain_button", CoordGrid(2847, 3499, 0)),
    OgreArea(5, "Lemantolly Undri", "ogrearea_button", CoordGrid(2545, 2971, 0)),
    ApeAtoll(6, "Ookookolly Undri", "apeatoll_button", CoordGrid(2711, 2802, 0));

    companion object {
        fun byCode(code: Int): GliderDestination? = entries.firstOrNull { it.code == code }
    }
}
