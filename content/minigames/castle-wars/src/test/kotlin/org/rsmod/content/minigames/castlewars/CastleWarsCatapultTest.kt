package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.map.CoordGrid

class CastleWarsCatapultTest {
    @Test
    fun `thirty by thirty lands beside the enemy catapult`() {
        val sara = CatapultRules.target(CwTeam.Saradomin, 30, 30)
        val zam = CatapultRules.target(CwTeam.Zamorak, 30, 30)
        val zamHome = CatapultRules.HOME.getValue(CwTeam.Zamorak)
        val saraHome = CatapultRules.HOME.getValue(CwTeam.Saradomin)
        assertTrue(kotlin.math.abs(sara.x - zamHome.x) <= 1 && kotlin.math.abs(sara.z - zamHome.z) <= 1)
        assertTrue(kotlin.math.abs(zam.x - saraHome.x) <= 1 && kotlin.math.abs(zam.z - saraHome.z) <= 1)
    }

    @Test
    fun `splash covers a five by five area`() {
        val area = CatapultRules.splash(CoordGrid(2400, 3100, 0))
        assertEquals(25, area.size)
        assertTrue(CoordGrid(2398, 3102, 0) in area)
        assertFalse(CoordGrid(2397, 3100, 0) in area)
    }

    @Test
    fun `scatter stays within two tiles`() {
        val target = CoordGrid(2400, 3100, 0)
        repeat(200) { seed ->
            var step = seed
            val landed = CatapultRules.scatter(target) { bound -> (step++ * 7 + 3) % bound }
            assertTrue(kotlin.math.abs(landed.x - target.x) <= CatapultRules.SCATTER)
            assertTrue(kotlin.math.abs(landed.z - target.z) <= CatapultRules.SCATTER)
        }
    }

    @Test
    fun `operating needs a working catapult and respects the cooldown`() {
        val ledger = CatapultLedger()
        assertTrue(ledger.operate(CwTeam.Saradomin, 100))
        assertFalse(ledger.operate(CwTeam.Saradomin, 101))
        assertTrue(ledger.operate(CwTeam.Saradomin, 100 + CatapultRules.COOLDOWN_TICKS))
        assertTrue(ledger.operate(CwTeam.Zamorak, 101))
    }

    @Test
    fun `fire breaks a catapult until it is repaired`() {
        val ledger = CatapultLedger()
        assertTrue(ledger.ignite(CwTeam.Zamorak, 10))
        assertFalse(ledger.ignite(CwTeam.Zamorak, 11))
        assertFalse(ledger.operate(CwTeam.Zamorak, 12))
        assertEquals(emptyList<CwTeam>(), ledger.update(10 + CatapultRules.BURN_TICKS - 1))
        assertEquals(listOf(CwTeam.Zamorak), ledger.update(10 + CatapultRules.BURN_TICKS))
        assertEquals(CatapultState.Broken, ledger.state(CwTeam.Zamorak))
        assertTrue(ledger.repair(CwTeam.Zamorak))
        assertEquals(CatapultState.Working, ledger.state(CwTeam.Zamorak))
    }

    @Test
    fun `water puts out a burning catapult in time`() {
        val ledger = CatapultLedger()
        ledger.ignite(CwTeam.Saradomin, 0)
        assertTrue(ledger.extinguish(CwTeam.Saradomin))
        assertEquals(emptyList<CwTeam>(), ledger.update(1000))
        assertFalse(ledger.extinguish(CwTeam.Saradomin))
    }

    @Test
    @ResourceLock("server-cache")
    fun `catapult symbols resolve`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (team in CwTeam.entries) {
                for (state in CatapultState.entries) {
                    val symbol = CatapultRules.symbol(team, state)
                    assertNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)), symbol)
                }
            }
            for (item in listOf("obj.tinderbox", "obj.bucket_water", "obj.bucket_empty", "obj.castlewars_toolkit", "obj.castlewars_catapult_rock")) {
                assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            }
        } finally {
            cache.close()
        }
    }
}
