package org.rsmod.content.raids.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.map.CoordGrid

class TobRoomTest {
    private val southWest = CoordGrid(6400, 6400, 0)

    @Test
    fun `every room stays inside its own source square`() {
        for (room in TobRoom.entries) {
            assertTrue(room.contains(room.arrival), "${room.name} arrival")
            assertTrue(room.contains(room.arena), "${room.name} arena")
            room.exit?.let { assertTrue(room.contains(it), "${room.name} exit") }
        }
    }

    @Test
    fun `instance and source coordinates round trip`() {
        for (room in TobRoom.entries) {
            val instance = room.toInstance(southWest, room.arrival)
            assertEquals(room, TobRoom.at(southWest, instance))
            assertEquals(room.arrival, room.toSource(southWest, instance))
        }
    }

    @Test
    fun `no two rooms share an instance slot`() {
        val slots = TobRoom.entries.map { Triple(it.plane, it.slotColumn, it.slotRow) }
        assertEquals(slots.size, slots.toSet().size)
    }

    @Test
    fun `rooms run in raid order and end at the treasure room`() {
        assertEquals(TobRoom.Bloat, TobRoom.Maiden.next)
        assertEquals(TobRoom.Treasure, TobRoom.Verzik.next)
        assertEquals(TobRoom.Xarpus, TobRoom.Sotetseg.next)
        assertFalse(TobRoom.Maze.inSequence)
        assertEquals(null, TobRoom.Treasure.next)
        assertNotEquals(TobRoom.Treasure.isFight, true)
    }
}
