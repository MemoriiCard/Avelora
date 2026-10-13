package org.rsmod.content.minigames.tempoross

import org.rsmod.map.CoordGrid

internal object TemporossMap {
    val LOBBY_EXIT = CoordGrid(3140, 2846, 0)
    val DECKS = listOf(CoordGrid(3035, 2842, 1), CoordGrid(3058, 2842, 1))

    val SITES =
        listOf(
            CoordGrid(3036, 2849, 1),
            CoordGrid(3058, 2849, 1),
            CoordGrid(3043, 2833, 0),
            CoordGrid(3044, 2867, 0),
        )

    val SITE_LOCS =
        listOf(
            "loc.tempoross_mast_bottom_west" to "loc.tempoross_mast_bottom_west_broken",
            "loc.tempoross_mast_bottom_east" to "loc.tempoross_mast_bottom_east_broken",
            "loc.tempoross_totem_south" to "loc.tempoross_totem_south_broken",
            "loc.tempoross_totem_north" to "loc.tempoross_totem_north_broken",
        )

    val FIRES =
        listOf(
            CoordGrid(3035, 2856, 1),
            CoordGrid(3035, 2841, 1),
            CoordGrid(3059, 2856, 1),
            CoordGrid(3059, 2841, 1),
        )

    val CANNONS =
        listOf(
            CoordGrid(3038, 2851, 1),
            CoordGrid(3038, 2846, 1),
            CoordGrid(3054, 2851, 1),
            CoordGrid(3054, 2846, 1),
        )

    val CRATES =
        listOf(
            CoordGrid(3038, 2850, 1),
            CoordGrid(3038, 2848, 1),
            CoordGrid(3056, 2850, 1),
            CoordGrid(3056, 2848, 1),
        )

    val SPOTS = listOf(CoordGrid(3047, 2833, 1), CoordGrid(3047, 2865, 1))

    private const val MIN_X = 3028
    private const val MAX_X = 3066
    private const val MIN_Z = 2822
    private const val MAX_Z = 2878

    fun inside(coords: CoordGrid): Boolean = coords.x in MIN_X..MAX_X && coords.z in MIN_Z..MAX_Z

    fun siteAt(coords: CoordGrid): Int? = SITES.indices.firstOrNull { SITES[it].x == coords.x && SITES[it].z == coords.z }

    fun crateAt(coords: CoordGrid): Int? = CRATES.indexOf(coords).takeIf { it >= 0 }
}
