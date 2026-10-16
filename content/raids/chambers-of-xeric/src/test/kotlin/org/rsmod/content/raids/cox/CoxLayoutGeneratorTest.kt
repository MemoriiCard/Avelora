package org.rsmod.content.raids.cox

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.cox.layout.CoxCell
import org.rsmod.content.raids.cox.layout.CoxDirection
import org.rsmod.content.raids.cox.layout.CoxLayout
import org.rsmod.content.raids.cox.layout.CoxLayoutGenerator
import org.rsmod.content.raids.cox.layout.CoxRoomCategory
import org.rsmod.content.raids.cox.layout.CoxRoomType
import org.rsmod.content.raids.cox.party.CoxMapPool
import org.rsmod.content.raids.cox.raid.CoxRaid

class CoxLayoutGeneratorTest {
    private fun layouts(pool: CoxMapPool): List<CoxLayout> =
        (0 until 200).map { CoxLayoutGenerator(Random(it)).generate(pool) }

    @Test
    fun `every pool builds connected floors that stack on each other`() {
        for (pool in CoxMapPool.entries) {
            for (layout in layouts(pool)) {
                var previousEnd: CoxCell? = null
                for (floor in layout.floors) {
                    val cells = floor.rooms.map { it.cell }
                    assertTrue(cells.all { it.inGrid }, "$pool $cells")
                    assertEquals(cells.size, cells.toSet().size, "$pool rooms overlap: $cells")
                    for ((a, b) in floor.rooms.zipWithNext()) {
                        assertEquals(b.cell, a.cell.step(a.exit!!), "$pool exit mismatch")
                        assertEquals(a.exit, b.entrance)
                    }
                    if (previousEnd != null) assertEquals(previousEnd, floor.start.cell)
                    previousEnd = floor.end.cell
                    assertEquals(CoxRoomCategory.Start, floor.start.type.category)
                    assertEquals(CoxRoomCategory.End, floor.end.type.category)
                }
                assertEquals(CoxRoomType.Lobby, layout.floors.first().start.type)
                assertEquals(CoxRoomType.OlmEntrance, layout.floors.last().end.type)
            }
        }
    }

    @Test
    fun `regular raids have two floors of seven or eight rooms`() {
        for (layout in layouts(CoxMapPool.Small)) {
            assertEquals(2, layout.floors.size)
            for (floor in layout.floors) assertTrue(floor.rooms.size in 7..8, "${floor.rooms.size}")
            val combats = layout.rooms.filter { it.type.category == CoxRoomCategory.Combat }
            assertEquals(combats.size, combats.map { it.type }.toSet().size, "boss rooms repeat")
        }
    }

    @Test
    fun `large raids have five combat and three puzzle rooms`() {
        for (layout in layouts(CoxMapPool.Large)) {
            assertEquals(2, layout.floors.size)
            for (floor in layout.floors) assertEquals(8, floor.rooms.size)
            assertEquals(5, layout.rooms.count { it.type.category == CoxRoomCategory.Combat })
            assertEquals(3, layout.rooms.count { it.type.category == CoxRoomCategory.Puzzle })
        }
    }

    @Test
    fun `combat rooms follow one of the jagex rotations`() {
        for (layout in layouts(CoxMapPool.Small) + layouts(CoxMapPool.Large)) {
            val combats = layout.rooms.map { it.type }.filter { it.category == CoxRoomCategory.Combat }
            val matches =
                CoxRoomType.COMBAT_ROTATIONS.any { rotation ->
                    listOf(rotation, rotation.reversed()).any { order ->
                        order.indices.any { start ->
                            combats.indices.all { combats[it] == order[(start + it) % order.size] }
                        }
                    }
                }
            assertTrue(matches, "$combats")
        }
    }

    @Test
    fun `full layout uses the challenge mode room order on three floors`() {
        for (layout in layouts(CoxMapPool.Full)) {
            assertEquals(3, layout.floors.size)
            val middles = layout.floors.map { f -> f.rooms.drop(1).dropLast(1).map { it.type } }
            assertEquals(CoxLayoutGenerator.FULL_FLOORS, middles)
            assertEquals(listOf(3, 2, 1), layout.floors.map { it.plane })
        }
    }

    @Test
    fun `room variants match the turn into the next room`() {
        for (layout in layouts(CoxMapPool.Small)) {
            for (room in layout.rooms.filter { it.type.isCombatOrPuzzle }) {
                val expected =
                    when (room.exit) {
                        room.entrance -> 32
                        room.entrance!!.left -> 0
                        else -> 64
                    }
                assertEquals(expected, room.variantOffsetX)
                assertEquals(room.entrance!!.ordinal, room.rotation)
            }
        }
    }

    @Test
    fun `rotating a tile matches the engine's zone rotation`() {
        assertEquals(0 to 0, CoxRaid.rotate(0, 0, 0))
        assertEquals(5 to 31, CoxRaid.rotate(1, 0, 5))
        assertEquals(31 to 26, CoxRaid.rotate(2, 0, 5))
        assertEquals(26 to 0, CoxRaid.rotate(3, 0, 5))
        assertEquals(CoxDirection.West, CoxDirection.North.left)
        assertEquals(CoxDirection.East, CoxDirection.North.right)
    }

    @Test
    fun `region templates place every zone once`() {
        for (pool in CoxMapPool.entries) {
            for (layout in layouts(pool).take(20)) {
                CoxRaid.template(layout)
            }
        }
    }
}
