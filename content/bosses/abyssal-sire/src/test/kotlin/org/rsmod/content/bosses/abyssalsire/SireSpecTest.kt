package org.rsmod.content.bosses.abyssalsire

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator
import org.rsmod.map.CoordGrid

class SireSpecTest {
    @Test
    fun `every spec is valid`() {
        for (spec in listOf(sireSpec(), tentacleSpec(), spawnSpec(), scionSpec())) {
            val errors = SpecValidator.validate(spec)
            assertTrue(errors.isEmpty(), "${spec.npcTypes}: $errors")
        }
    }

    @Test
    fun `the sire spec covers every sire form`() {
        assertEquals(SIRE_TYPES.toSet(), sireSpec().npcTypes.toSet())
    }

    @Test
    fun `chamber offsets match the map spawns`() {
        val spawns = mapSpawns()
        for (chamber in SireChamber.entries) {
            assertTrue(chamber.sire in spawns[SIRE_SLEEPING].orEmpty(), "No sire at ${chamber.sire}")
            assertTrue(spawns[LUNG].orEmpty().containsAll(chamber.lungs), "Lungs missing in $chamber")
            val tentacles = TENTACLE_SLEEPING.flatMap { spawns[it].orEmpty() }
            assertTrue(tentacles.containsAll(chamber.tentacles), "Tentacles missing in $chamber")
            for (tile in chamber.lungs + chamber.tentacles + chamber.sire + chamber.centre) {
                assertEquals(chamber, SireChamber.containing(tile), "$tile is outside $chamber")
            }
        }
    }

    private fun mapSpawns(): Map<String, List<CoordGrid>> {
        val file = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, ".data/raw-cache/map/npcs/abyssal_areas.toml") }
            .first { it.exists() }
        val regex = Regex("npc = \"([^\"]+)\"\\s*\\ncoords = \"(\\d+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)\"")
        return regex.findAll(file.readText())
            .map { match ->
                val (npc, level, mx, mz, x, z) = match.destructured
                npc to CoordGrid(mx.toInt() * 64 + x.toInt(), mz.toInt() * 64 + z.toInt(), level.toInt())
            }
            .groupBy({ it.first }, { it.second })
    }
}
