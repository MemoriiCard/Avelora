package org.rsmod.content.bosses.dagannothkings

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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
class WaterbirthLaddersMapTest {
    private val collision = CollisionFlagMap()
    private val locs = mutableMapOf<String, MutableList<CoordGrid>>()
    private lateinit var cache: AutoCloseable

    private val destinations =
        WaterbirthLadders.SUBLEVEL_LADDERS.values +
            listOf(
                WaterbirthLadders.CENTRAL_ROOM,
                WaterbirthLadders.SUBLEVEL_ENTRANCE,
                WaterbirthLadders.KINGS_LAIR,
                WaterbirthLadders.KINGS_LADDER,
            )

    @BeforeAll
    fun loadMaps() {
        val manager = ServerCacheManager.init(240)
        cache = AutoCloseable { manager.close() }
        for (square in destinations.map { MapSquareKey(it.x shr 6, it.z shr 6) }.distinct()) {
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
                    locs.getOrPut(name) { mutableListOf() } += base.translate(key.x, key.z)
                }
            }
        }
    }

    @AfterAll
    fun closeCache() {
        cache.close()
    }

    @Test
    fun `every ladder destination is walkable`() {
        for (dest in destinations) {
            assertTrue(walkable(dest), "Blocked landing at $dest")
        }
    }

    @Test
    fun `every sublevel ladder lands beside its partner one level away`() {
        for ((ladder, dest) in WaterbirthLadders.SUBLEVEL_LADDERS) {
            val from = locs[ladder]?.singleOrNull()
            assertNotNull(from, "$ladder is not in the map")
            assertEquals(1, kotlin.math.abs(from!!.level - dest.level), "$ladder changes level by one")
            val partner = locs.entries.firstOrNull { (name, tiles) ->
                name.startsWith("loc.dagexp_ladder") && tiles.any { it.level == dest.level && it.chebyshevDistance(dest) == 1 }
            }
            assertNotNull(partner, "No ladder next to the landing of $ladder")
        }
    }

    @Test
    fun `the kings ladder and lair exit are where the table expects`() {
        val down = locs.getValue("loc.dagexp_bossroomladder_down").single()
        assertEquals(1, down.chebyshevDistance(WaterbirthLadders.KINGS_LADDER))
        val up = locs.getValue("loc.dagexp_bossroomladder_up")
        assertTrue(up.any { it.level == 0 && it.chebyshevDistance(WaterbirthLadders.KINGS_LAIR) == 1 })
    }

    private fun walkable(tile: CoordGrid): Boolean {
        val flags = collision[tile.x, tile.z, tile.level]
        return flags and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC) == 0
    }
}
