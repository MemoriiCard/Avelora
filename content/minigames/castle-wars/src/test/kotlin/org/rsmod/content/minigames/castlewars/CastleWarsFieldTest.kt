package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.map.CoordGrid

class CastleWarsFieldTest {
    private val tile = CoordGrid(2400, 3100, 0)

    @Test
    fun `a team can hold ten barricades and none share a tile`() {
        val ledger = BarricadeLedger()
        repeat(BarricadeLedger.TEAM_LIMIT) { assertNotNull(ledger.place(CwTeam.Saradomin, CoordGrid(2400 + it, 3100, 0))) }
        assertNull(ledger.place(CwTeam.Saradomin, CoordGrid(2420, 3100, 0)))
        assertNotNull(ledger.place(CwTeam.Zamorak, CoordGrid(2420, 3100, 0)))
        assertFalse(ledger.canPlace(CwTeam.Zamorak, CoordGrid(2400, 3100, 0)))
    }

    @Test
    fun `a burning barricade burns out unless it is put out`() {
        val ledger = BarricadeLedger()
        val entry = checkNotNull(ledger.place(CwTeam.Zamorak, tile))
        assertTrue(ledger.ignite(entry, cycle = 100))
        assertFalse(ledger.ignite(entry, cycle = 101))
        assertTrue(ledger.burntOut(100 + BarricadeLedger.BURN_TICKS - 1).isEmpty())
        assertEquals(listOf(entry), ledger.burntOut(100 + BarricadeLedger.BURN_TICKS))
        assertTrue(ledger.extinguish(entry))
        assertTrue(ledger.burntOut(1000).isEmpty())
    }

    @Test
    fun `barricades that were never lit never burn out`() {
        val ledger = BarricadeLedger()
        ledger.place(CwTeam.Saradomin, tile)
        assertTrue(ledger.burntOut(Int.MAX_VALUE - 1).isEmpty())
    }
}

@ResourceLock("server-cache")
class CastleWarsFieldSymbolsTest {
    @Test
    fun `supply tables, barricade npcs and items resolve in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (table in CastleWarsSupplies.tables) {
                assertNotNull(ServerCacheManager.getObject(table.loc.asRSCM(RSCMType.LOC)), table.loc)
                assertNotNull(ServerCacheManager.getItem(table.obj.asRSCM(RSCMType.OBJ)), table.obj)
            }
            for (team in CwTeam.entries) {
                for (burning in listOf(false, true)) {
                    val npc = CastleWarsBarricades.npcSymbol(team, burning)
                    assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
                }
            }
            listOf("obj.tinderbox", "obj.bucket_water", "obj.bucket_empty", "obj.castlewars_explosives_potion")
                .forEach { assertNotNull(ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ)), it) }
        } finally {
            cache.close()
        }
    }
}
