package org.rsmod.content.bosses.corp

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
class CorpLairMapTest {
    private val collision = CollisionFlagMap()
    private val locs = mutableMapOf<String, MutableList<CoordGrid>>()
    private lateinit var cache: AutoCloseable

    private val landings =
        listOf(CorpLairMap.CAVE_OUTSIDE, CorpLairMap.lobbyFor(false), CorpLairMap.lobbyFor(true), SPAWN, SPAWN.translate(0, CorpLairMap.IRONMAN_ROOM_OFFSET))

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
    fun `cave and lobby landings are walkable`() {
        assertTrue(walkable(CorpLairMap.CAVE_OUTSIDE))
        assertTrue(walkable(CorpLairMap.lobbyFor(false)))
        assertTrue(walkable(CorpLairMap.lobbyFor(true)))
        assertNear("loc.corp_cave_entrance", CorpLairMap.CAVE_OUTSIDE)
        assertNear("loc.corp_cave_exit", CorpLairMap.lobbyFor(false))
    }

    @Test
    fun `the passage lands on open ground on both sides`() {
        for (ironman in listOf(false, true)) {
            val offset = if (ironman) CorpLairMap.IRONMAN_ROOM_OFFSET else 0
            for (z in CorpLairMap.PASSAGE_SOUTH_Z..CorpLairMap.PASSAGE_NORTH_Z) {
                val west = CoordGrid(CorpLairMap.PASSAGE_WEST_X, z + offset, 2)
                val east = CorpLairMap.throughPassage(west)
                assertTrue(walkable(east), "Room side $east is blocked")
                assertTrue(CorpLairMap.inRoom(east))
                val back = CorpLairMap.throughPassage(east)
                assertTrue(walkable(back), "Lobby side $back is blocked")
                assertFalse(CorpLairMap.inRoom(back))
            }
        }
    }

    @Test
    fun `both beasts spawn inside their rooms`() {
        assertTrue(CorpLairMap.inRoom(SPAWN))
        assertTrue(CorpLairMap.inRoom(SPAWN.translate(0, CorpLairMap.IRONMAN_ROOM_OFFSET)))
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
        val SPAWN = CoordGrid(2993, 4254, 2)
    }
}
