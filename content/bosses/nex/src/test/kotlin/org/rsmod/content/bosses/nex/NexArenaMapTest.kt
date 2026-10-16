package org.rsmod.content.bosses.nex

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NexArenaMapTest {
    private val collision = CollisionFlagMap()
    private val locs = mutableMapOf<String, MutableList<CoordGrid>>()
    private lateinit var cache: AutoCloseable

    @BeforeAll
    fun loadMaps() {
        val manager = ServerCacheManager.init(240)
        cache = AutoCloseable { manager.close() }
        for (square in listOf(MapSquareKey(44, 81), MapSquareKey(45, 81), MapSquareKey(45, 82))) {
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
    fun `nex and her mages stand on open floor`() {
        assertFootprintOpen(NexArena.SPAWN, 3)
        for (mage in NexMage.entries) assertTrue(walkable(mage.tile), "${mage.name} at ${mage.tile}")
        for (dash in NexArena.DASH_ENDS) {
            assertFootprintOpen(dash.start, 3)
            assertTrue(dash.path(3).all(::walkable), "dash $dash crosses a wall")
        }
    }

    @Test
    fun `travel tiles are walkable and beside their locs`() {
        val tiles = listOf(NexArena.BARRIER_INSIDE, NexArena.BARRIER_OUTSIDE, NexArena.BANK_ROOM, AncientPrison.PRISON_ARRIVAL, AncientPrison.DUNGEON_ARRIVAL)
        for (tile in tiles) assertTrue(walkable(tile), "$tile is blocked")
        assertNear("loc.nex_fight_barrier", NexArena.BARRIER_INSIDE)
        assertNear("loc.nex_fight_barrier", NexArena.BARRIER_OUTSIDE)
        assertNear("loc.nex_frozen_door_inner_1", AncientPrison.PRISON_ARRIVAL)
        assertNear("loc.nex_frozen_door_outer_1", AncientPrison.DUNGEON_ARRIVAL)
        assertNear(AncientPrison.OUTER_PRISON_DOOR, CoordGrid(AncientPrison.OUTER_PRISON_DOOR_X, AncientPrison.OUTER_PRISON_DOOR_Z, 0), reach = 0)
        assertNear(AncientPrison.INNER_PRISON_DOOR, CoordGrid(AncientPrison.INNER_PRISON_DOOR_X, AncientPrison.INNER_PRISON_DOOR_Z, 0), reach = 0)
        for (door in listOf(AncientPrison.OUTER_PRISON_DOOR_X to AncientPrison.OUTER_PRISON_DOOR_Z, AncientPrison.INNER_PRISON_DOOR_X to AncientPrison.INNER_PRISON_DOOR_Z)) {
            assertTrue(walkable(CoordGrid(door.first - 1, door.second, 0)))
            assertTrue(walkable(CoordGrid(door.first + 1, door.second, 0)))
        }
        assertFalse(NexArena.contains(NexArena.BARRIER_OUTSIDE))
        assertTrue(NexArena.contains(NexArena.BARRIER_INSIDE))
    }

    @Test
    fun `fight symbols resolve`() {
        val spotanims =
            listOf(
                "spotanim.nex_smoke_attack_proj",
                "spotanim.nex_smoke_attack_impact",
                "spotanim.nex_shadow_attack_proj",
                "spotanim.nex_blood_attack_proj",
                "spotanim.nex_blood_attack_impact",
                "spotanim.nex_ice_attack_proj",
                "spotanim.nex_ice_attack_impact",
                "spotanim.nex_finale_attack_proj",
                "spotanim.nex_finale_attack_impact",
                "spotanim.nex_ice_prison_proj",
                "spotanim.nex_blood_siphon",
                "spotanim.nex_turmoil",
                "spotanim.nex_mushroom_cloud_spotanim",
            )
        val seqs =
            listOf(
                "seq.nex_attack",
                "seq.nex_cast_attack",
                "seq.nex_alternate_cast_attack",
                "seq.nex_spin_out",
                "seq.nex_dash_attack",
                "seq.nex_blood_siphon",
                "seq.nex_smash_attack",
                "seq.nex_ready",
                "seq.nex_turmoil",
                "seq.human_casting",
                "seq.human_pray",
            )
        spotanims.forEach { it.asRSCM(RSCMType.SPOTANIM) }
        seqs.forEach { it.asRSCM(RSCMType.SEQ) }
        listOf("loc.nex_shadow_smash", "loc.nex_icicle_1", AncientPrison.BARRIER, AncientPrison.ZAROS_ALTAR).forEach { it.asRSCM(RSCMType.LOC) }
        (AncientPrison.KEY_PIECES + AncientPrison.FROZEN_KEY).forEach { it.asRSCM(RSCMType.OBJ) }
        "varbit.nex_barrier".asRSCM(RSCMType.VARBIT)
    }

    @Test
    fun `mages are shielded until their own turn`() {
        val fight = NexFight()
        val fumus = Npc(checkNotNull(ServerCacheManager.getNpc(NexMage.Fumus.npc.asRSCM(RSCMType.NPC))), NexMage.Fumus.tile)
        fight.mages[NexMage.Fumus] = fumus
        assertTrue(fight.isShielded(fumus))
        fight.mageTurn = true
        assertFalse(fight.isShielded(fumus))
        fight.phase = NexPhase.Shadow
        assertTrue(fight.isShielded(fumus))
    }

    private fun assertFootprintOpen(sw: CoordGrid, size: Int) {
        for (dx in 0 until size) for (dz in 0 until size) {
            val tile = sw.translate(dx, dz)
            assertTrue(walkable(tile), "$tile under a size-$size npc at $sw is blocked")
        }
    }

    private fun assertNear(loc: String, tile: CoordGrid, reach: Int = 2) {
        val placed = locs[loc].orEmpty()
        assertTrue(placed.any { it.level == tile.level && it.chebyshevDistance(tile) <= reach }, "$loc is not next to $tile (at $placed)")
    }

    private fun walkable(tile: CoordGrid): Boolean {
        val flags = collision[tile.x, tile.z, tile.level]
        return flags and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC) == 0
    }
}
