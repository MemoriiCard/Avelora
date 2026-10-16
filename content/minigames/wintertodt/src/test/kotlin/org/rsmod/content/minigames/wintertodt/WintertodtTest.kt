package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

class WintertodtRoundTest {
    private fun round(roll: Int = 99) = WintertodtRound { roll % it }

    private fun started(): WintertodtRound {
        val round = round()
        round.join(1)
        assertEquals(listOf(WintertodtEvent.Started), round.tick(0))
        return round
    }

    @Test
    fun `a knockout takes down a standing pyromancer and a revive awards points once`() {
        val roll = WintertodtRound { bound -> if (bound == 100) WintertodtRound.COLD_PERCENT + WintertodtRound.SNOW_PERCENT else 1 }
        roll.join(1)
        roll.tick(0)
        val events = (1..60).flatMap { roll.tick(it) }
        val knock = events.filterIsInstance<WintertodtEvent.Attack>().first { it.kind == AttackKind.Knockout }
        val index = checkNotNull(knock.brazier)
        assertTrue(roll.pyromancerDown(index))
        assertTrue(roll.revive(index, 1))
        assertFalse(roll.revive(index, 1))
        assertFalse(roll.pyromancerDown(index))
        assertEquals(WintertodtRound.REVIVE_POINTS, roll.pointsOf(1))
    }

    @Test
    fun `outsiders cannot revive`() {
        val round = started()
        assertFalse(round.revive(0, 1))
    }

    @Test
    fun `a round only starts once someone has joined`() {
        val round = round()
        assertTrue(round.tick(0).isEmpty())
        assertFalse(round.active)
        round.join(1)
        assertEquals(listOf(WintertodtEvent.Started), round.tick(1))
        assertTrue(round.active)
        assertEquals(WintertodtRound.ENERGY_MAX, round.energy)
    }

    @Test
    fun `lighting, feeding and repairing award points only in the right state`() {
        val round = started()
        assertFalse(round.feed(0, 1, 10))
        assertTrue(round.light(0, 1))
        assertFalse(round.light(0, 1))
        assertTrue(round.feed(0, 1, WintertodtRound.KINDLING_POINTS))
        assertFalse(round.repair(0, 1))
        assertEquals(WintertodtRound.LIGHT_POINTS + WintertodtRound.KINDLING_POINTS, round.pointsOf(1))
    }

    @Test
    fun `outsiders earn nothing`() {
        val round = started()
        assertFalse(round.light(0, 2))
    }

    @Test
    fun `lit braziers drain energy`() {
        val round = started()
        round.light(0, 1)
        round.light(1, 1)
        val before = round.energy
        round.tick(1)
        assertEquals(before - 2 * WintertodtRound.DRAIN, round.energy)
    }

    @Test
    fun `feeding lowers energy and a lit brazier keeps draining it`() {
        val round = WintertodtRound { 0 }
        round.join(1)
        round.tick(0)
        round.light(0, 1)
        round.feed(0, 1, 500)
        val drained = round.energy
        assertTrue(drained < WintertodtRound.ENERGY_MAX)
        round.repair(0, 1)
        round.tick(1)
        assertTrue(round.energy < drained)
    }

    @Test
    fun `a shattered brazier can be repaired`() {
        val roll = WintertodtRound { bound -> if (bound == 100) 99 else if (bound == 2) 1 else 0 }
        roll.join(1)
        roll.tick(0)
        roll.light(2, 1)
        val events = (1..60).flatMap { roll.tick(it) }
        val attack = events.filterIsInstance<WintertodtEvent.Attack>().first { it.kind == AttackKind.Shatter }
        assertEquals(2, attack.brazier)
        assertEquals(BrazierState.Broken, roll.brazier(2))
        assertTrue(roll.repair(2, 1))
        assertEquals(BrazierState.Unlit, roll.brazier(2))
    }

    @Test
    fun `feeding the boss to zero ends the round with each member's points`() {
        val round = WintertodtRound { 0 }
        round.join(1)
        round.tick(0)
        round.join(2)
        round.light(0, 1)
        var ended: WintertodtEvent.Ended? = null
        var cycle = 1
        while (ended == null && cycle < 10_000) {
            round.feed(0, 2, WintertodtRound.KINDLING_POINTS)
            ended = round.tick(cycle++).filterIsInstance<WintertodtEvent.Ended>().firstOrNull()
        }
        checkNotNull(ended)
        assertEquals(WintertodtRound.LIGHT_POINTS, ended.points.getValue(1))
        assertTrue(ended.points.getValue(2) > WintertodtRound.MIN_POINTS)
        assertFalse(round.active)
        assertEquals(BrazierState.Unlit, round.brazier(0))
        assertTrue(round.tick(cycle).isEmpty())
        assertEquals(listOf(WintertodtEvent.Started), round.tick(cycle + WintertodtRound.RESPAWN_TICKS))
        assertEquals(0, round.pointsOf(2))
    }
}

class WintertodtLootTest {
    @Test
    fun `no reward under the minimum and the wiki worked example above it`() {
        assertEquals(0, WintertodtLoot.rolls(499) { 0 })
        assertEquals(2, WintertodtLoot.rolls(500) { 499 })
        assertEquals(3, WintertodtLoot.rolls(1200) { 499 })
        assertEquals(4, WintertodtLoot.rolls(1200) { 0 })
        assertEquals(WintertodtLoot.MAX_ROLLS, WintertodtLoot.rolls(Int.MAX_VALUE / 2) { 0 })
    }

    @Test
    fun `drops respect the level ladders`() {
        val low = Levels { 1 }
        val high = Levels { 99 }
        val lowDrops = (0 until 2000).map { WintertodtLoot.roll(low) { bound -> (it * 31 + bound * 7) % bound } }
        assertTrue(lowDrops.none { it.obj == "obj.runite_ore" || it.obj == "obj.raw_anglerfish" })
        assertTrue(lowDrops.all { it.count > 0 })
        val highDrops = (0 until 4000).map { WintertodtLoot.roll(high) { bound -> (it * 31 + bound * 7) % bound } }
        assertTrue(highDrops.any { it.obj == "obj.runite_ore" })
    }
}

@ResourceLock("server-cache")
class WintertodtSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val locs =
                listOf(
                    "loc.wint_door",
                    "loc.wint_roots",
                    "loc.wint_herb_roots",
                    "loc.wint_brazier",
                    "loc.wint_brazier_lit",
                    "loc.wint_brazier_broken",
                    "loc.wint_reward_pile",
                ) + listOf("axe", "tinderbox", "hammer", "knife", "vial").map { "loc.wint_chest_$it" }
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            val objs =
                WintertodtService.WARM_ITEMS +
                    WintertodtLoot.ladders.flatMap { l -> l.tiers.map { it.obj } } +
                    WintertodtLoot.uniques.map { it.first.obj } +
                    listOf(
                        "obj.wint_bruma_root",
                        "obj.wint_bruma_kindling",
                        "obj.wint_vial",
                        "obj.wint_herb",
                        "obj.wint_potion1",
                        "obj.wint_potion2",
                        "obj.wint_potion3",
                        "obj.wint_potion4",
                        "obj.wint_torch",
                        "obj.tinderbox",
                        "obj.hammer",
                        "obj.knife",
                        "obj.bronze_axe",
                        "obj.iron_axe",
                        "obj.steel_axe",
                        "obj.black_axe",
                        "obj.mithril_axe",
                        "obj.adamant_axe",
                        "obj.rune_axe",
                        "obj.dragon_axe",
                    )
            for (obj in objs) assertNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)), obj)
            for (stat in WintertodtLoot.ladders.map { it.stat }) stat.asRSCM(RSCMType.STAT)
            for (varbit in listOf(WintertodtService.WARMTH, WintertodtService.POOL)) {
                assertNotNull(ServerCacheManager.getVarbit(varbit.asRSCM(RSCMType.VARBIT)), varbit)
            }
            assertNotNull(ServerCacheManager.getVarp(WintertodtService.KILLS.asRSCM(RSCMType.VARP)))
            assertTrue(WintertodtService.TIMER.asRSCM(RSCMType.TIMER) > 0)
            WintertodtService.OVERLAY.asRSCM(RSCMType.INTERFACE)
            for (npc in listOf(WintertodtService.PYROMANCER, WintertodtService.PYROMANCER_DOWN)) {
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
            "clientscript.[clientscript,wint_update]".asRSCM(RSCMType.CLIENTSCRIPT)
        } finally {
            cache.close()
        }
    }
}
