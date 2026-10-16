package org.rsmod.content.raids.cox.layout

import kotlin.random.Random
import org.rsmod.content.raids.cox.party.CoxMapPool

class CoxLayoutGenerator(private val random: Random = Random.Default) {
    fun generate(pool: CoxMapPool): CoxLayout {
        val floorRooms =
            when (pool) {
                CoxMapPool.Small -> regularFloors()
                CoxMapPool.Large -> largeFloors()
                CoxMapPool.Full -> FULL_FLOORS
            }
        return place(floorRooms)
    }

    private fun regularFloors(): List<List<CoxRoomType>> {
        val (upper, lower) = REGULAR_PATTERNS.random(random).split('.')
        return assign(listOf(upper, lower))
    }

    private fun largeFloors(): List<List<CoxRoomType>> {
        val slots = MutableList(LARGE_COMBAT_ROOMS) { 'C' } + List(LARGE_PUZZLE_ROOMS) { 'P' }
        val shuffled = slots.shuffled(random)
        val floors =
            shuffled.chunked(shuffled.size / 2).map { encounters ->
                val rest = (encounters + 'F').shuffled(random)
                (listOf('S') + rest).joinToString("")
            }
        return assign(floors)
    }

    /** Turns pattern strings (S, F, C, P) into concrete rooms following Jagex's room rotations. */
    private fun assign(patterns: List<String>): List<List<CoxRoomType>> {
        val combatCount = patterns.sumOf { p -> p.count { it == 'C' } }
        val combats = combatOrder(combatCount).iterator()
        val puzzleCount = patterns.sumOf { p -> p.count { it == 'P' } }
        val puzzles = puzzleOrder(puzzleCount).iterator()
        return patterns.map { pattern ->
            val scavengers = pattern.count { it == 'S' }
            var scavengersPlaced = 0
            pattern.map { symbol ->
                when (symbol) {
                    'C' -> combats.next()
                    'P' -> puzzles.next()
                    'F' -> CoxRoomType.FARMING.random(random)
                    'S' -> {
                        scavengersPlaced++
                        scavengerRoom(scavengers, scavengersPlaced)
                    }
                    else -> error("Unknown room symbol '$symbol' in $pattern")
                }
            }
        }
    }

    private fun scavengerRoom(onFloor: Int, placed: Int): CoxRoomType =
        when {
            onFloor >= 2 && placed == onFloor -> CoxRoomType.ScavengersLarge
            onFloor >= 2 -> CoxRoomType.Scavengers
            random.nextBoolean() -> CoxRoomType.ScavengersLarge
            else -> CoxRoomType.Scavengers
        }

    private fun combatOrder(count: Int): List<CoxRoomType> {
        val rotation = CoxRoomType.COMBAT_ROTATIONS.random(random)
        val ordered = if (random.nextBoolean()) rotation else rotation.reversed()
        val start = random.nextInt(ordered.size)
        return List(count) { ordered[(start + it) % ordered.size] }
    }

    private fun puzzleOrder(count: Int): List<CoxRoomType> {
        val result = mutableListOf<CoxRoomType>()
        while (result.size < count) {
            result += CoxRoomType.PUZZLES.shuffled(random)
        }
        return result.take(count)
    }

    private fun place(middles: List<List<CoxRoomType>>): CoxLayout {
        val floorCount = middles.size
        val floors = mutableListOf<CoxFloor>()
        var entryCell: CoxCell? = null
        for ((index, middle) in middles.withIndex()) {
            val start =
                when (index) {
                    0 -> CoxRoomType.Lobby
                    1 -> CoxRoomType.FloorStart
                    else -> CoxRoomType.FloorStartLower
                }
            val end =
                when {
                    index == floorCount - 1 -> CoxRoomType.OlmEntrance
                    index == 0 -> CoxRoomType.FloorEnd
                    else -> CoxRoomType.FloorEndMiddle
                }
            val types = listOf(start) + middle + end
            val path = findPath(types.size, entryCell) ?: error("No layout path for $types")
            floors += buildFloor(index, types, path)
            entryCell = path.last()
        }
        return CoxLayout(floors)
    }

    private fun buildFloor(index: Int, types: List<CoxRoomType>, path: List<CoxCell>): CoxFloor {
        val rooms =
            types.mapIndexed { i, type ->
                val entrance = if (i == 0) null else direction(path[i - 1], path[i])
                val exit = if (i == types.lastIndex) null else direction(path[i], path[i + 1])
                CoxRoom(type, path[i], entrance, exit)
            }
        return CoxFloor(index, FIRST_FLOOR_PLANE - index, rooms)
    }

    private fun direction(from: CoxCell, to: CoxCell): CoxDirection =
        CoxDirection.entries.first { from.step(it) == to }

    private fun findPath(length: Int, start: CoxCell?): List<CoxCell>? {
        val starts = start?.let { listOf(it) } ?: CoxCell.ALL.shuffled(random)
        for (cell in starts) {
            val path = mutableListOf(cell)
            if (extend(path, length)) return path
        }
        return null
    }

    private fun extend(path: MutableList<CoxCell>, length: Int): Boolean {
        if (path.size == length) return true
        val current = path.last()
        for (direction in CoxDirection.entries.shuffled(random)) {
            val next = current.step(direction)
            if (!next.inGrid || next in path) continue
            path += next
            if (extend(path, length)) return true
            path.removeAt(path.lastIndex)
        }
        return false
    }

    companion object {
        const val FIRST_FLOOR_PLANE = 3
        const val OLM_PLANE = 0

        private const val LARGE_COMBAT_ROOMS = 5
        private const val LARGE_PUZZLE_ROOMS = 3

        /** Room orders observed in live raids (RuneLite's layout solver), between start and end. */
        val REGULAR_PATTERNS =
            listOf(
                "FSCCP.PCSCF",
                "FSCCS.PCPSF",
                "FSCPC.CSCPF",
                "SCCFC.PSCSF",
                "SCCFP.CCSPF",
                "SCFCP.CCSPF",
                "SCFCP.CSCFS",
                "SCFCPC.CSPCSF",
                "SCFPC.CSPCF",
                "SCFPC.PCCSF",
                "SCFPC.SCPCF",
                "SCPFC.CCPSF",
                "SCPFC.CSPCF",
                "SCPFC.CSPSF",
                "SCSPF.CCSPF",
                "SFCCP.CSCPF",
                "SFCCS.PCPSF",
                "SPCFC.CSPCF",
                "SPCFC.SCCPF",
                "SPSFP.CCCSF",
                "SCFCP.CSCPF",
                "SCPFC.PCSCF",
                "SFCCPC.PCSCPF",
                "FSPCC.PSCCF",
                "SCPFC.CCSSF",
            )

        val FULL_FLOORS: List<List<CoxRoomType>> =
                listOf(
                    listOf(
                        CoxRoomType.Tekton,
                        CoxRoomType.Crabs,
                        CoxRoomType.Scavengers,
                        CoxRoomType.IceDemon,
                        CoxRoomType.Shamans,
                        CoxRoomType.FarmingFishing,
                    ),
                    listOf(
                        CoxRoomType.Vanguards,
                        CoxRoomType.Thieving,
                        CoxRoomType.Scavengers,
                        CoxRoomType.Vespula,
                        CoxRoomType.FarmingBats,
                        CoxRoomType.Tightrope,
                    ),
                    listOf(
                        CoxRoomType.Guardians,
                        CoxRoomType.Vasa,
                        CoxRoomType.ScavengersLarge,
                        CoxRoomType.Mystics,
                        CoxRoomType.FarmingFishing,
                        CoxRoomType.Muttadiles,
                    ),
                )
    }
}
