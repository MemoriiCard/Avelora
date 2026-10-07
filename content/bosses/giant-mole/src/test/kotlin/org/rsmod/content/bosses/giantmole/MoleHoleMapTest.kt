package org.rsmod.content.bosses.giantmole

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoleHoleMapTest {
    private val collision = CollisionFlagMap()
    private val locs = mutableMapOf<CoordGrid, MutableList<String>>()
    private lateinit var cache: AutoCloseable

    @BeforeAll
    fun loadMaps() {
        val manager = ServerCacheManager.init(240)
        cache = AutoCloseable { manager.close() }
        val squares = (MoleHole.MOLE_HILLS + MoleHole.BURROW_SPOTS + MoleHole.LANDING + MoleHole.EXIT)
        for (square in squares.map { MapSquareKey(it.x shr 6, it.z shr 6) }.distinct()) {
            val group = (square.x shl 8) or square.z
            val tiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(manager.data(MAPS, group, 0))))
            val spawns = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(manager.data(MAPS, group, 1))))
            for (level in 0..3) for (x in 0 until 64 step 8) for (z in 0 until 64 step 8) {
                collision.allocateIfAbsent(square.x * 64 + x, square.z * 64 + z, level)
            }
            val builder = GameMapBuilder()
            GameMapDecoder.putMaps(collision, square, tiles)
            GameMapDecoder.putLocs(builder, collision, square, tiles, spawns)
            for ((packed, zone) in builder.zoneBuilders) {
                val base = ZoneKey(packed).toCoords()
                for (entry in zone.build().byte2IntEntrySet()) {
                    val key = LocZoneKey(entry.byteKey)
                    val name = RSCM.getReverseMapping(RSCMType.LOC, LocEntity(entry.intValue).id)
                    locs.getOrPut(base.translate(key.x, key.z)) { mutableListOf() } += name
                }
            }
        }
    }

    @AfterAll
    fun closeCache() {
        cache.close()
    }

    @Test
    fun `every registered dig tile has a mole hill`() {
        for (tile in MoleHole.MOLE_HILLS) {
            assertTrue("loc.mole_hill" in locs[tile].orEmpty(), "No mole hill at $tile")
        }
    }

    @Test
    fun `landing and exit tiles are walkable`() {
        assertTrue(walkable(MoleHole.LANDING))
        assertTrue(walkable(MoleHole.EXIT))
    }

    @Test
    fun `the exit rope is in the lair`() {
        val rope = CoordGrid(1752, 5136, 0)
        assertTrue("loc.mole_rope_02" in locs[rope].orEmpty())
        assertTrue(MoleHole.contains(rope))
    }

    @Test
    fun `the giant mole fits on every burrow spot`() {
        for (spot in MoleHole.BURROW_SPOTS) {
            assertTrue(MoleHole.contains(spot), "$spot is outside the lair")
            for (dx in 0 until MOLE_SIZE) for (dz in 0 until MOLE_SIZE) {
                assertTrue(walkable(spot.translate(dx, dz)), "Mole blocked at $spot")
            }
        }
    }

    @Test
    fun `surface tiles are not part of the lair`() {
        assertFalse(MoleHole.contains(MoleHole.EXIT))
        MoleHole.MOLE_HILLS.forEach { assertFalse(MoleHole.contains(it)) }
    }

    private fun walkable(tile: CoordGrid): Boolean {
        val flags = collision[tile.x, tile.z, tile.level]
        return flags and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC) == 0
    }

    private companion object {
        const val MOLE_SIZE = 3
    }
}
