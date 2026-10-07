package org.rsmod.content.raids.cox.layout

data class CoxLayout(val floors: List<CoxFloor>) {
    val rooms: List<CoxRoom>
        get() = floors.flatMap { it.rooms }

    fun roomAt(plane: Int, cell: CoxCell): CoxRoom? =
        floors.firstOrNull { it.plane == plane }?.rooms?.firstOrNull { it.cell == cell }

    fun floorOn(plane: Int): CoxFloor? = floors.firstOrNull { it.plane == plane }
}

data class CoxFloor(val index: Int, val plane: Int, val rooms: List<CoxRoom>) {
    val start: CoxRoom
        get() = rooms.first()

    val end: CoxRoom
        get() = rooms.last()
}

data class CoxRoom(
    val type: CoxRoomType,
    val cell: CoxCell,
    val entrance: CoxDirection?,
    val exit: CoxDirection?,
) {
    /** Clockwise quarter turns that point the template's south entrance at the previous room. */
    val rotation: Int
        get() = (entrance ?: exit ?: CoxDirection.North).ordinal

    val variantOffsetX: Int
        get() {
            if (!type.hasVariants) return 0
            val travel = entrance ?: return VARIANT_STRAIGHT
            val out = exit ?: return VARIANT_STRAIGHT
            return when (out) {
                travel -> VARIANT_STRAIGHT
                travel.left -> VARIANT_LEFT
                else -> VARIANT_RIGHT
            }
        }

    val templateX: Int
        get() = type.templateX + variantOffsetX

    private companion object {
        const val VARIANT_LEFT = 0
        const val VARIANT_STRAIGHT = 32
        const val VARIANT_RIGHT = 64
    }
}

data class CoxCell(val column: Int, val row: Int) {
    fun step(direction: CoxDirection): CoxCell =
        CoxCell(column + direction.deltaColumn, row + direction.deltaRow)

    val inGrid: Boolean
        get() = column in 0 until COLUMNS && row in 0 until ROWS

    companion object {
        const val COLUMNS = 4
        const val ROWS = 2
        const val SIZE = 32

        val ALL: List<CoxCell> = (0 until ROWS).flatMap { r -> (0 until COLUMNS).map { CoxCell(it, r) } }
    }
}

/** Travel directions; ordinal doubles as the room rotation for that direction of travel. */
enum class CoxDirection(val deltaColumn: Int, val deltaRow: Int) {
    North(0, -1),
    East(1, 0),
    South(0, 1),
    West(-1, 0);

    val left: CoxDirection
        get() = entries[(ordinal + 3) % 4]

    val right: CoxDirection
        get() = entries[(ordinal + 1) % 4]
}
