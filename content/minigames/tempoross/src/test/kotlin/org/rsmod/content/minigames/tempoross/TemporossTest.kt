package org.rsmod.content.minigames.tempoross

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

class TemporossRoundTest {
    private fun round(roll: Int = 99) = TemporossRound { roll % it }

    private fun started(players: Int = 1, roll: Int = 99): TemporossRound =
        round(roll).also { it.start(players, 0) }

    @Test
    fun `meters scale with party size`() {
        val solo = started(1)
        assertEquals(260, solo.maxEnergy)
        assertEquals(250, solo.maxEssence)
        val full = started(40)
        assertEquals(180 * 40, full.maxEnergy)
        assertEquals(170 * 40, full.maxEssence)
    }

    @Test
    fun `raw and cooked fish drain energy and award points`() {
        val round = started()
        assertTrue(round.deposit(1, cooked = false))
        assertTrue(round.deposit(1, cooked = true))
        assertEquals(260 - 25, round.energy)
        assertEquals(TemporossRound.RAW_DEPOSIT_POINTS + TemporossRound.COOKED_DEPOSIT_POINTS, round.pointsOf(1))
    }

    @Test
    fun `emptying energy submerges tempoross and resets the storm`() {
        val round = started()
        repeat(30) { round.deposit(1, cooked = true) }
        assertEquals(TemporossPhase.Essence, round.phase)
        assertEquals(0.0, round.storm)
        assertFalse(round.deposit(1, cooked = true))
    }

    @Test
    fun `striking the pool until essence is gone wins the round`() {
        val round = started()
        repeat(30) { round.deposit(1, cooked = true) }
        repeat(25) { round.strike(1, 12) }
        assertEquals(TemporossPhase.Won, round.phase)
        assertEquals(0, round.essence)
    }

    @Test
    fun `energy regenerates back into phase one and a fourth return enrages`() {
        val round = started()
        var cycle = 0
        repeat(TemporossRound.MAX_RETURNS) { returned ->
            repeat(30) { round.deposit(1, cooked = true) }
            val events = (1..400).flatMap { round.tick(++cycle + cycle * 0) }
            assertTrue(events.any { it is TemporossEvent.EnergyPhase && it.returns == returned + 1 })
        }
        repeat(30) { round.deposit(1, cooked = true) }
        val events = (1..400).flatMap { round.tick(++cycle) }
        assertTrue(TemporossEvent.Lost in events)
        assertEquals(TemporossPhase.Lost, round.phase)
    }

    @Test
    fun `an unattended storm eventually washes the crew away`() {
        val round = started(roll = 99)
        val events = (1..2000).flatMap { round.tick(it) }
        assertTrue(TemporossEvent.Lost in events)
    }

    @Test
    fun `tethered players survive the wave and earn points, others do not`() {
        val round = started(roll = 0)
        assertTrue(round.tether(1, 0))
        val events = (1..200).flatMap { round.tick(it) }
        val hit = events.filterIsInstance<TemporossEvent.WaveHit>().first()
        assertEquals(setOf(1), hit.tethered)
        assertTrue(round.pointsOf(1) >= TemporossRound.WAVE_POINTS)
        assertEquals(0, round.pointsOf(2))
    }

    @Test
    fun `broken sites cannot be tethered until repaired`() {
        val round = started(roll = 0)
        (1..200).flatMap { round.tick(it) }
        val broken = (0 until TemporossRound.SITES).first { round.siteBroken(it) }
        assertFalse(round.tether(1, broken))
        assertTrue(round.repair(1, broken))
        assertEquals(TemporossRound.REPAIR_POINTS, round.pointsOf(1))
        assertTrue(round.tether(1, broken))
    }

    @Test
    fun `lightning lights a fire that can be doused once`() {
        val round = started(roll = 0)
        val events = (1..60).flatMap { round.tick(it) }
        val slot = events.filterIsInstance<TemporossEvent.Lightning>().first().slot
        assertTrue(round.fireAt(slot))
        assertTrue(round.douse(1, slot))
        assertFalse(round.douse(1, slot))
        assertEquals(TemporossRound.DOUSE_POINTS, round.pointsOf(1))
    }

    @Test
    fun `attacks stop at ten percent energy`() {
        val round = started(roll = 0)
        repeat(24) { round.deposit(1, cooked = false) }
        assertTrue(round.energy > 0)
        assertTrue(round.energy * 100 <= round.maxEnergy * 10)
        val events = (1..300).flatMap { round.tick(it) }.filter { it is TemporossEvent.WaveWarning || it is TemporossEvent.Lightning }
        assertTrue(events.isEmpty())
    }

    @Test
    fun `points are capped and forfeited players lose theirs`() {
        val round = started()
        round.award(1, 100_000)
        assertEquals(TemporossRound.MAX_POINTS, round.pointsOf(1))
        round.forfeit(1)
        assertEquals(0, round.pointsOf(1))
    }
}

class TemporossLootTest {
    @Test
    fun `permits follow the points thresholds`() {
        val none = { _: Int -> 0 }
        val max = { n: Int -> n - 1 }
        assertEquals(0, TemporossLoot.permits(1999, none))
        assertEquals(1, TemporossLoot.permits(2000, none))
        assertEquals(2, TemporossLoot.permits(2700, none))
        assertEquals(11, TemporossLoot.permits(9000, none))
        assertEquals(21, TemporossLoot.permits(16000, none))
        assertEquals(1, TemporossLoot.permits(2000, max))
        assertEquals(1, TemporossLoot.permits(2699, max))
        assertEquals(2, TemporossLoot.permits(2400) { 350 })
    }

    @Test
    fun `the common roll is spirit flakes and the table has every listed prize`() {
        val (drop, amount) = TemporossLoot.roll { n -> n - 1 }
        assertEquals("obj.spirit_flakes", drop.obj)
        assertTrue(amount in 32..64)
        val last = TemporossLoot.table.last()
        assertEquals("obj.temporosspet", last.obj)
    }
}

@ResourceLock("server-cache")
class TemporossSymbolTest {
    @Test
    fun `every gameval the module references resolves`() {
        ServerCacheManager.init(240)
        val npcs =
            listOf(
                TemporossService.SPOT_SOUTH,
                TemporossService.SPOT_NORTH,
                TemporossService.SPOT_SPECIAL,
                TemporossService.SPIRIT_SPOT,
                TemporossService.FIRE_NPC,
            )
        for (symbol in npcs) assertNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)), symbol)
        val objs = TemporossService.AT_RISK + TemporossLoot.table.map { it.obj } + "obj.net" + "obj.big_net"
        for (symbol in objs) assertNotNull(ServerCacheManager.getItem(symbol.asRSCM(RSCMType.OBJ)), symbol)
        val locs =
            TemporossMap.SITE_LOCS.flatMap { it.toList() } +
                (1..4).flatMap {
                    listOf("loc.tempoross_cannon_$it", "loc.tempoross_cannon_${it}_disabled", "loc.tempoross_crate_ammunition_empty_$it")
                } +
                listOf("loc.tempoross_lobby_ladder", "loc.tempoross_lobby_boat", "loc.tempoross_rewardpool", "loc.tempoross_shrine_fire")
        for (symbol in locs) assertNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)), symbol)
    }
}

@ResourceLock("server-cache")
class TomeOfWaterSymbolsTest {
    @Test
    fun `tome items carry their variant params and the charge varobj and curse spells resolve`() {
        val cache = ServerCacheManager.init(240)
        try {
            val charged = "param.charged_variant".asRSCM(RSCMType.PARAM)
            val uncharged = "param.uncharged_variant".asRSCM(RSCMType.PARAM)
            val empty = checkNotNull(ServerCacheManager.getItem(TomeOfWaterScript.EMPTY.asRSCM(RSCMType.OBJ)))
            val full = checkNotNull(ServerCacheManager.getItem(TomeOfWaterScript.CHARGED.asRSCM(RSCMType.OBJ)))
            assertEquals(full.id, empty.paramsRaw?.get(charged))
            assertEquals(empty.id, full.paramsRaw?.get(uncharged))
            assertNotNull(
                ServerCacheManager.getVarObj(
                    org.rsmod.api.combat.manager.TomeChargeManager.VAROBJ.asRSCM(RSCMType.VAROBJ)
                )
            )
            for (spell in listOf("03_confuse", "11_weaken", "19_curse", "66_vulnerability", "73_enfeeble", "80_stun")) {
                assertNotNull(
                    ServerCacheManager.getItem("obj.$spell".asRSCM(RSCMType.OBJ)),
                    spell,
                )
            }
        } finally {
            cache.close()
        }
    }
}
