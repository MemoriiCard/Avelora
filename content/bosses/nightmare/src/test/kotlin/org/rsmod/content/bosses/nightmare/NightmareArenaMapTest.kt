package org.rsmod.content.bosses.nightmare

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
class NightmareArenaMapTest {
    private val collision = CollisionFlagMap()
    private val locs = mutableMapOf<String, MutableList<CoordGrid>>()
    private lateinit var cache: AutoCloseable

    @BeforeAll
    fun loadMaps() {
        val manager = ServerCacheManager.init(240)
        cache = AutoCloseable { manager.close() }
        for (square in listOf(MapSquareKey(60, 155), MapSquareKey(59, 152))) {
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
    fun `nightmare spawn, surge ends and arrivals are open floor`() {
        assertFootprintOpen(NightmareArena.SPAWN)
        assertFootprintOpen(NightmareArena.SURGE_WEST)
        assertFootprintOpen(NightmareArena.SURGE_EAST)
        val path = NightmareArena.surgePath(NightmareArena.SURGE_WEST, NightmareArena.SURGE_EAST)
        assertTrue(path.all(::walkable), "surge path crosses a wall")
        for (tile in NightmareArena.ARRIVALS) {
            assertTrue(walkable(tile), "$tile is blocked")
            assertTrue(NightmareArena.contains(tile))
        }
        assertTrue(walkable(NightmareArena.LOBBY), "lobby tile is blocked")
        assertFalse(NightmareArena.contains(NightmareArena.LOBBY))
        assertTrue(NightmareArena.edgeTiles().count(::walkable) >= 6)
    }

    @Test
    fun `escape barriers sit at the arena's ends`() {
        val barriers = locs["loc.nightmare_barrier_escape_initial"].orEmpty()
        val edges = setOf(NightmareArena.MIN_Z - 1, NightmareArena.MAX_Z + 1)
        assertTrue(barriers.size == 2 && barriers.all { it.z in edges && it.x in NightmareArena.MIN_X..NightmareArena.MAX_X }, "$barriers")
    }

    @Test
    fun `fight symbols resolve`() {
        listOf(
                "spotanim.nightmare_magic_travel",
                "spotanim.nightmare_magic_impact",
                "spotanim.nightmare_ranged_travel",
                "spotanim.nightmare_rift",
                "spotanim.nightmare_totem_spell_travel",
                "spotanim.nightmare_impact_blast_spotanim",
                "spotanim.nightmare_parasite_travel",
                "spotanim.nightmare_parasite_heal_travel",
                "spotanim.nightmare_parasite_vomit",
                "spotanim.nightmare_husk_magic_travel",
                "spotanim.nightmare_husk_ranged_travel",
            )
            .forEach { it.asRSCM(RSCMType.SPOTANIM) }
        listOf(
                "seq.nightmare_attack_melee",
                "seq.nightmare_attack_magic",
                "seq.nightmare_attack_ranged",
                "seq.nightmare_attack_rift",
                "seq.nightmare_attack_summon",
                "seq.nightmare_attack_parasite",
                "seq.nightmare_attack_surge",
                "seq.nightmare_attack_infection",
                "seq.nightmare_attack_blast",
                "seq.nightmare_spawn_initial",
                "seq.nightmare_totem_fully_charged",
                "seq.nightmare_parasite_spawn",
                "seq.husk_spawn",
                "seq.husk_magic_attack",
                "seq.husk_ranged_attack",
            )
            .forEach { it.asRSCM(RSCMType.SEQ) }
        (SisterhoodSanctuary.ESCAPE_BARRIERS + "loc.nightmare_spores").forEach { it.asRSCM(RSCMType.LOC) }
        SisterhoodSanctuary.ENTRY_FORMS.forEach { checkNotNull(ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC))) }
    }

    @Test
    fun `totems only take charge while the totem stage runs`() {
        val fight = NightmareFight()
        val totem = Npc(checkNotNull(ServerCacheManager.getNpc(NightmareTotem.SouthWest.dormant.asRSCM(RSCMType.NPC))), NightmareTotem.SouthWest.tile)
        fight.totems[NightmareTotem.SouthWest] = totem
        assertTrue(fight.isTotemShut(totem))
        fight.state = NightmareFight.State.Totems
        assertFalse(fight.isTotemShut(totem))
        fight.chargedTotems += NightmareTotem.SouthWest
        assertTrue(fight.isTotemShut(totem))
    }

    private fun assertFootprintOpen(sw: CoordGrid) {
        for (dx in 0 until NIGHTMARE_SIZE) for (dz in 0 until NIGHTMARE_SIZE) {
            val tile = sw.translate(dx, dz)
            assertTrue(walkable(tile), "$tile under the Nightmare at $sw is blocked")
        }
    }

    private fun walkable(tile: CoordGrid): Boolean {
        val flags = collision[tile.x, tile.z, tile.level]
        return flags and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC) == 0
    }
}
