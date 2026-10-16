package org.rsmod.content.raids.tob

import java.util.Random
import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.sotetseg.SotetsegRules

class SotetsegRulesTest {
    @Test
    fun `maze triggers at 66 and 33 percent`() {
        assertNull(SotetsegRules.nextStage(80, 0))
        assertEquals(0, SotetsegRules.nextStage(66, 0))
        assertNull(SotetsegRules.nextStage(60, 1))
        assertEquals(1, SotetsegRules.nextStage(33, 1))
    }

    @Test
    fun `every generated path is connected, unique, and ends at the exit column`() {
        repeat(200) { seed ->
            val random = Random(seed.toLong())
            val path = SotetsegRules.path { random.nextInt(it) }
            assertEquals(0, path.first().second)
            assertEquals(SotetsegRules.GRID_HEIGHT - 1, path.last().second)
            assertEquals(SotetsegRules.EXIT_COLUMN, path.last().first)
            assertEquals(path.size, path.toSet().size)
            for ((a, b) in path.zipWithNext()) {
                assertEquals(1, abs(a.first - b.first) + abs(a.second - b.second), "seed $seed")
            }
            for ((x, z) in path) {
                assertTrue(x in 0 until SotetsegRules.GRID_WIDTH && z in 0 until SotetsegRules.GRID_HEIGHT)
            }
        }
    }

    @Test
    fun `wrong tile damage is a flat amount plus a share of current health`() {
        assertEquals(15 + 990 * 667 / 10_000, SotetsegRules.wrongTileDamage(990))
        assertTrue(SotetsegRules.wrongTileDamage(99) < SotetsegRules.wrongTileDamage(990))
    }

    @Test
    fun `the death ball is split between everyone caught in it`() {
        assertEquals(188, SotetsegRules.ballDamage(5, TobMode.Normal))
        assertTrue(SotetsegRules.ballDamage(1, TobMode.Normal) < 188)
        assertEquals(47, SotetsegRules.ballShare(188, 4))
        assertEquals(188, SotetsegRules.ballShare(188, 0))
    }

    @Test
    fun `arena and shadow realm grids line up tile for tile`() {
        val cell = 3 to 7
        val arena = SotetsegRules.arenaTile(cell)
        val maze = SotetsegRules.mazeTile(cell)
        assertEquals(arena.z, maze.z)
        assertEquals(maze.x - arena.x, SotetsegRules.MAZE_ORIGIN.x - SotetsegRules.ARENA_ORIGIN.x)
    }
}
