package org.rsmod.content.bosses.zulrah

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
class ZulrahMapTest {
    private val collision = CollisionFlagMap()
    private val locs = mutableMapOf<String, MutableList<CoordGrid>>()
    private lateinit var cache: AutoCloseable

    private val landings =
        listOf(ZulrahShrine.DOCK, ZulrahShrine.PLAYER_START, ZulrahShrine.EXIT_SCROLL) +
            ZulrahShrine.OPENING_CLOUDS + ZulrahShrine.SNAKELING_SPOTS +
            ZulrahPosition.entries.map(ZulrahShrine::position)

    @BeforeAll
    fun loadMaps() {
        val manager = ServerCacheManager.init(240)
        cache = AutoCloseable { manager.close() }
        for (square in landings.map { MapSquareKey(it.x shr 6, it.z shr 6) }.distinct()) {
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
    fun `dock, start and exit scroll tiles are walkable`() {
        assertTrue(walkable(ZulrahShrine.DOCK))
        assertTrue(walkable(ZulrahShrine.PLAYER_START))
        assertTrue(walkable(ZulrahShrine.EXIT_SCROLL))
    }

    @Test
    fun `the dock sits beside the sacrificial boat`() {
        assertNear("loc.snakeboss_boat", ZulrahShrine.DOCK)
    }

    @Test
    fun `opening clouds centre on the shrine platform`() {
        for (cloud in ZulrahShrine.OPENING_CLOUDS) {
            val centre = cloud.translate(CLOUD_SIZE / 2, CLOUD_SIZE / 2)
            assertTrue(walkable(centre), "Cloud $cloud centres on water at $centre")
        }
    }

    @Test
    fun `snakelings spawn on the shrine platform`() {
        for (spot in ZulrahShrine.SNAKELING_SPOTS) {
            assertTrue(walkable(spot), "Snakeling spot $spot is not walkable")
        }
    }

    @Test
    fun `zulrah surfaces off the platform`() {
        for (position in ZulrahPosition.entries) {
            val sw = ZulrahShrine.position(position)
            val centre = sw.translate(ZULRAH_SIZE / 2, ZULRAH_SIZE / 2)
            assertFalse(walkable(centre), "$position surfaces on dry land at $centre")
        }
    }

    private fun assertNear(loc: String, tile: CoordGrid) {
        val placed = locs[loc].orEmpty()
        assertTrue(placed.any { it.level == tile.level && it.chebyshevDistance(tile) <= 2 }, "$loc is not next to $tile")
    }

    private fun walkable(tile: CoordGrid): Boolean {
        val flags = collision[tile.x, tile.z, tile.level]
        return flags and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC) == 0
    }

    private companion object {
        const val CLOUD_SIZE = 3
        const val ZULRAH_SIZE = 5
    }
}
