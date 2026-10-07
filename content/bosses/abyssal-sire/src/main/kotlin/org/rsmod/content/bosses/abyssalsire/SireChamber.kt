package org.rsmod.content.bosses.abyssalsire

import org.rsmod.map.CoordGrid

internal object AbyssalNexus {
    const val SLAYER_LEVEL = 85

    val ABYSS = CoordGrid(3039, 4805, 0)
    val NEXUS = CoordGrid(3039, 4799, 0)

    val SIRE = CoordGrid(2967, 4791, 0)
    val CENTRE = CoordGrid(2967, 4772, 0)
    val LUNGS =
        listOf(CoordGrid(2954, 4780, 0), CoordGrid(2957, 4770, 0), CoordGrid(2982, 4779, 0), CoordGrid(2985, 4769, 0))
    val TENTACLES =
        listOf(
            CoordGrid(2958, 4762, 0),
            CoordGrid(2975, 4762, 0),
            CoordGrid(2957, 4780, 0),
            CoordGrid(2974, 4780, 0),
            CoordGrid(2960, 4771, 0),
            CoordGrid(2972, 4771, 0),
        )

    const val MIN_X = 2948
    const val MAX_X = 2994
    const val MIN_Z = 4756
    const val MAX_Z = 4797
}

internal enum class SireChamber(private val dx: Int, private val dz: Int) {
    SouthWest(0, 0),
    NorthWest(10, 64),
    SouthEast(140, 0),
    NorthEast(135, 64);

    val sire: CoordGrid
        get() = shift(AbyssalNexus.SIRE)

    val centre: CoordGrid
        get() = shift(AbyssalNexus.CENTRE)

    val lungs: List<CoordGrid>
        get() = AbyssalNexus.LUNGS.map(::shift)

    val tentacles: List<CoordGrid>
        get() = AbyssalNexus.TENTACLES.map(::shift)

    fun contains(tile: CoordGrid): Boolean =
        tile.level == 0 &&
            tile.x - dx in AbyssalNexus.MIN_X..AbyssalNexus.MAX_X &&
            tile.z - dz in AbyssalNexus.MIN_Z..AbyssalNexus.MAX_Z

    private fun shift(tile: CoordGrid): CoordGrid = tile.translate(dx, dz)

    companion object {
        fun containing(tile: CoordGrid): SireChamber? = entries.firstOrNull { it.contains(tile) }
    }
}
