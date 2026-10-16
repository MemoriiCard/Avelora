package org.rsmod.content.minigames.gotr

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

class GotrRoundTest {
    private fun started(players: Int = 1, roll: Int = 0): GotrRound =
        GotrRound { (roll % it) }.also { it.start(players, 0) }

    @Test
    fun `power starts partly charged and scales with players`() {
        val solo = started()
        assertEquals(500, solo.maxPower)
        assertEquals(150.0, solo.power)
        assertEquals(1500, started(3).maxPower)
    }

    @Test
    fun `stones charge the great guardian and a full charge wins`() {
        val round = started()
        round.deposit(10)
        assertEquals(170.0, round.power)
        round.deposit(0, polyStones = 2)
        assertEquals(176.0, round.power)
        round.deposit(500)
        assertEquals(GotrPhase.Won, round.phase)
    }

    @Test
    fun `an unfed great guardian fades and the round is lost`() {
        val round = started()
        val events = (1..2000).flatMap { round.tick(it) }
        assertTrue(GotrEvent.Lost in events)
        assertEquals(GotrPhase.Lost, round.phase)
    }

    @Test
    fun `portals rotate to a different rune of each kind`() {
        val round = started()
        val first = round.elementalRune to round.catalyticRune
        val events = (1..GotrRound.ROTATION_TICKS).flatMap { round.tick(it) }
        val rotated = events.filterIsInstance<GotrEvent.Rotated>().single()
        assertTrue(rotated.elemental != first.first)
        assertTrue(rotated.catalytic != first.second)
        assertEquals(GotrKind.Elemental, rotated.elemental.kind)
        assertEquals(GotrKind.Catalytic, rotated.catalytic.kind)
    }

    @Test
    fun `guardians give energy of their own kind, are capped, and expire`() {
        val round = started()
        val energy = round.assemble(1, GotrKind.Elemental, 0)
        assertEquals(round.elementalRune.tier.guardianEnergy, energy)
        assertEquals(energy, round.energyOf(1, GotrKind.Elemental))
        assertEquals(0, round.energyOf(1, GotrKind.Catalytic))
        repeat(GotrRound.GUARDIAN_LIMIT - 1) { round.assemble(1, GotrKind.Catalytic, 0) }
        assertEquals(-1, round.assemble(1, GotrKind.Catalytic, 0))
        val events = (1..GotrRound.GUARDIAN_LIFE).flatMap { round.tick(it) }
        assertTrue(events.any { it is GotrEvent.GuardiansExpired && it.count == GotrRound.GUARDIAN_LIMIT })
        assertEquals(0, round.guardianCount)
    }

    @Test
    fun `energy is capped per type and in total`() {
        val round = started()
        round.gain(1, GotrKind.Elemental, 5000)
        assertEquals(GotrRound.TYPE_CAP, round.energyOf(1, GotrKind.Elemental))
        round.gain(1, GotrKind.Catalytic, 5000)
        assertEquals(GotrRound.TOTAL_CAP, round.totalEnergy(1))
    }

    @Test
    fun `cell tiles recharge between placements and award both kinds`() {
        val round = started()
        assertTrue(round.placeCell(1, 0, GotrTier.Strong, 10))
        assertFalse(round.placeCell(1, 0, GotrTier.Strong, 11))
        assertTrue(round.placeCell(1, 1, GotrTier.Strong, 11))
        assertEquals(48, round.energyOf(1, GotrKind.Elemental))
        assertTrue(round.placeCell(1, 0, GotrTier.Weak, 10 + GotrRound.TILE_COOLDOWN))
    }

    @Test
    fun `rewards need three hundred energy and round probabilistically`() {
        val low = started(roll = 99)
        low.gain(1, GotrKind.Elemental, 200)
        assertFalse(low.reward(1).xpBonus)
        val round = started(roll = 99)
        round.gain(1, GotrKind.Elemental, 273)
        round.gain(1, GotrKind.Catalytic, 100)
        val reward = round.reward(1)
        assertTrue(reward.xpBonus)
        assertEquals(2, reward.elementalPoints)
        assertEquals(1, reward.catalyticPoints)
        val lucky = GotrRound { 0 }.also { it.start(1, 0) }
        lucky.gain(1, GotrKind.Elemental, 273)
        lucky.gain(1, GotrKind.Catalytic, 100)
        assertEquals(3, lucky.reward(1).elementalPoints)
    }

    @Test
    fun `rune table matches the wiki levels`() {
        assertEquals(27, GotrRune.Cosmic.level)
        assertEquals(77, GotrRune.Blood.level)
        assertEquals(4, GotrRune.elemental.size)
        assertEquals(8, GotrRune.catalytic.size)
        assertEquals(1, GotrRune.Air.runesPer(10))
        assertEquals(2, GotrRune.Air.runesPer(11))
    }
}

class GotrLootTest {
    @Test
    fun `lantern rare table is rolled first`() {
        val result = GotrLoot.roll(99) { 0 }
        assertEquals("obj.abyssal_lantern", result.obj)
    }

    @Test
    fun `pearls roll the quoted quantity and low level scales rune amounts down`() {
        val noRare = { n: Int -> if (n == GotrLoot.LANTERN_ODDS) 1 else 0 }
        assertEquals("obj.airrune", GotrLoot.roll(99, noRare).obj)
        assertEquals(400, GotrLoot.roll(99, noRare).amount)
        val blood = GotrLoot.table.first { it.rune == GotrRune.Blood }
        assertEquals(5, blood.min)
    }
}

@ResourceLock("server-cache")
class GotrSymbolTest {
    @Test
    fun `every gameval the module references resolves`() {
        ServerCacheManager.init(240)
        val locs =
            GotrRune.entries.map { it.portalLoc } +
                listOf(
                    "loc.gotr_barrier",
                    "loc.gotr_workbench",
                    "loc.gotr_uncharged_cells",
                    "loc.gotr_weak_cells",
                    "loc.gotr_guardian_spawn_east",
                    "loc.gotr_guardian_spawn_west",
                    "loc.gotr_cell_tile_inactive",
                    "loc.gotr_reward_rift",
                    "loc.gotr_essence_tier_1",
                    "loc.gotr_essence_tier_1b",
                    "loc.gotr_essence_tier_1_large",
                    "loc.gotr_essence_tier_1_large_b",
                    "loc.gotr_essence_tier_2",
                    "loc.gotr_essence_tier_3",
                )
        for (symbol in locs) assertNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)), symbol)
        assertNotNull(ServerCacheManager.getNpc("npc.gotr_great_guardian".asRSCM(RSCMType.NPC)))
        val objs =
            listOf(
                "obj.gotr_guardian_fragment",
                "obj.gotr_guardian_essence",
                "obj.gotr_cell_uncharged",
                "obj.gotr_guardian_stone_elemental",
                "obj.gotr_guardian_stone_catalytic",
                "obj.gotr_guardian_stone_polyelemental",
                "obj.gotr_guardian_stone_polycatalytic",
                "obj.gotr_intricate_pouch",
                "obj.abyssal_pearl",
                "obj.abyssal_needle",
                "obj.abyssal_lantern",
                "obj.chisel",
            ) +
                GotrTier.entries.map { it.cellObj } +
                GotrLoot.table.map { it.obj } +
                GotrRune.entries.flatMap { listOf(it.runeObj, it.portalTalismanObj) } +
                GotrRune.entries.filter { it != GotrRune.Law && it != GotrRune.Death && it != GotrRune.Blood }.map { it.talismanObj }
        for (symbol in objs) assertNotNull(ServerCacheManager.getItem(symbol.asRSCM(RSCMType.OBJ)), symbol)
        assertTrue("varp.gotr_rewards_perm".asRSCM(RSCMType.VARP) > 0)
        assertTrue(GotrService.OVERLAY.asRSCM(RSCMType.INTERFACE) > 0)
    }
}

class GotrPetTest {
    @Test
    fun `the abyssal protector pet is a registered pet`() {
        assertNotNull(org.rsmod.content.other.pets.Pets.forObj(GotrLoot.PET))
    }
}
