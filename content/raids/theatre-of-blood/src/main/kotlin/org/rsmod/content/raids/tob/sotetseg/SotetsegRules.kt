package org.rsmod.content.raids.tob.sotetseg

import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.map.CoordGrid

object SotetsegRules {
    const val ATTACK_RATE = 5
    const val BALL_EVERY = 10
    const val BALL_DELAY = 5
    const val BALL_MAX = 188
    const val BALL_SPLASH = 1
    const val MELEE_MAX = 45
    const val MELEE_RANGE = 2
    const val ORB_MAX = 50
    const val CHIP_RATE = 7
    const val WRONG_TILE_FLAT = 15
    const val MAZE_TIMEOUT = 150

    const val GRID_WIDTH = 12
    const val GRID_HEIGHT = 16
    const val EXIT_COLUMN = 2

    val ARENA_ORIGIN = CoordGrid(3273, 4310, 0)
    val MAZE_ORIGIN = CoordGrid(3355, 4310, 3)

    val STAGES = listOf(66, 33)

    fun hpPercent(current: Int, max: Int): Int = if (max <= 0) 0 else current * 100 / max

    fun nextStage(percent: Int, stage: Int): Int? =
        STAGES.indices.firstOrNull { it >= stage && percent <= STAGES[it] }

    fun ballDamage(teamSize: Int, mode: TobMode): Int =
        TobScaling.damage(BALL_MAX * teamSize / TobScaling.MAX_PARTY.coerceAtLeast(1), mode)

    fun wrongTileDamage(currentHp: Int): Int = currentHp * 667 / 10_000 + WRONG_TILE_FLAT

    fun ballShare(total: Int, sharing: Int): Int = total / sharing.coerceAtLeast(1)

    /** A random walk from the bottom row to the exit column on the top row; every step is 4-connected. */
    fun path(roll: (Int) -> Int): List<Pair<Int, Int>> {
        val steps = mutableListOf(roll(GRID_WIDTH) to 0)
        val seen = mutableSetOf(steps.first())
        var sideways = 0
        while (steps.last().second < GRID_HEIGHT - 1) {
            val (x, z) = steps.last()
            val options = mutableListOf<Pair<Int, Int>>()
            options += (x to z + 1)
            options += (x to z + 1)
            if (sideways < MAX_SIDEWAYS) {
                if (x > 0) options += (x - 1 to z)
                if (x < GRID_WIDTH - 1) options += (x + 1 to z)
            }
            val next = options.filter { it !in seen }.let { it[roll(it.size)] }
            sideways = if (next.second == z) sideways + 1 else 0
            steps += next
            seen += next
        }
        var (x, z) = steps.last()
        while (x != EXIT_COLUMN) {
            x += if (x < EXIT_COLUMN) 1 else -1
            if (x to z !in seen) {
                steps += (x to z)
                seen += (x to z)
            }
        }
        return steps
    }

    fun onPath(path: List<Pair<Int, Int>>, x: Int, z: Int): Boolean = (x to z) in path

    fun arenaTile(cell: Pair<Int, Int>): CoordGrid =
        CoordGrid(ARENA_ORIGIN.x + cell.first, ARENA_ORIGIN.z + cell.second, ARENA_ORIGIN.level)

    fun mazeTile(cell: Pair<Int, Int>): CoordGrid =
        CoordGrid(MAZE_ORIGIN.x + cell.first, MAZE_ORIGIN.z + cell.second, MAZE_ORIGIN.level)

    private const val MAX_SIDEWAYS = 3
}
