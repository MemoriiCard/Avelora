package org.rsmod.content.raids.cox

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.cox.reward.CoxLoot
import org.rsmod.content.raids.cox.reward.CoxRewardRoller

class CoxLootTest {
    @Test
    fun `common table weighs 99 and uniques weigh 60 or 56`() {
        assertEquals(99, CoxLoot.COMMONS.sumOf { it.weight })
        assertEquals(60, CoxLoot.UNIQUES.sumOf { it.normalWeight })
        assertEquals(56, CoxLoot.UNIQUES.sumOf { it.cmWeight })
    }

    @Test
    fun `roll count and chances follow the 570k point steps`() {
        assertEquals(0, CoxLoot.rollCount(0))
        assertEquals(1, CoxLoot.rollCount(1))
        assertEquals(1, CoxLoot.rollCount(570_000))
        assertEquals(2, CoxLoot.rollCount(570_001))
        assertEquals(6, CoxLoot.rollCount(10_000_000))
        val chances = CoxLoot.uniqueChances(855_000)
        assertEquals(2, chances.size)
        assertEquals(6570, chances[0])
        assertTrue(chances[1] in 3284..3286)
        assertEquals(6, CoxLoot.uniqueChances(Int.MAX_VALUE).size)
    }

    @Test
    fun `quantities match the wiki maximums at the point cap`() {
        fun max(obj: String) = CoxLoot.quantity(CoxLoot.COMMONS.first { it.obj == obj }, 200_000)
        assertEquals(3640, max("obj.deathrune"))
        assertEquals(4095, max("obj.bloodrune"))
        assertEquals(6553, max("obj.soulrune"))
        assertEquals(9362, max("obj.rune_arrow"))
        assertEquals(648, max("obj.dragon_arrow"))
        assertEquals(65, max("obj.runite_ore"))
        assertEquals(65535, max("obj.blankrune_high"))
        assertEquals(1, CoxLoot.quantity(CoxLoot.COMMONS.first { it.obj == "obj.runite_ore" }, 5))
        assertEquals(1, max("obj.raids_ancient_relic"))
    }

    @Test
    fun `target times match the party brackets`() {
        assertEquals(7000, CoxLoot.targetTicks(1))
        assertEquals(6500, CoxLoot.targetTicks(2))
        assertEquals(5000, CoxLoot.targetTicks(3))
        assertEquals(4500, CoxLoot.targetTicks(4))
        assertEquals(4200, CoxLoot.targetTicks(5))
        assertEquals(4200, CoxLoot.targetTicks(10))
        assertEquals(4500, CoxLoot.targetTicks(11))
        assertEquals(6000, CoxLoot.targetTicks(16))
        assertEquals(8000, CoxLoot.targetTicks(30))
    }

    @Test
    fun `recipients are weighted by personal points`() {
        val random = Random(7)
        val points = mapOf("high" to 90_000, "low" to 10_000)
        val wins = (1..2_000).count { CoxLoot.pickRecipient(points) { random.nextInt(it) } == "high" }
        assertTrue(wins in 1_650..1_950, "high won $wins of 2000")
    }

    @Test
    fun `players without a unique get two different commons`() {
        val random = Random(11)
        repeat(500) {
            val set =
                CoxRewardRoller.roll(
                    points = mapOf("a" to 40_000),
                    teamPoints = 0,
                    challengeMode = false,
                    underTarget = false,
                    present = { true },
                    hasTablet = { true },
                    hasJournal = { true },
                    below = { random.nextInt(it) },
                )
            val loot = set.items.getValue("a").filter { it.obj != CoxLoot.CLUE }
            assertEquals(2, loot.size)
            assertTrue(set.uniques.isEmpty())
        }
    }

    @Test
    fun `the first completion hands out the dark journal once`() {
        val random = Random(3)
        fun roll(has: Boolean) =
            CoxRewardRoller.roll(
                points = mapOf("a" to 1_000),
                teamPoints = 0,
                challengeMode = false,
                underTarget = false,
                present = { true },
                hasTablet = { true },
                hasJournal = { has },
                below = { random.nextInt(it) },
            ).items.getValue("a")
        assertTrue(roll(false).any { it.obj == CoxLoot.JOURNAL })
        assertTrue(roll(true).none { it.obj == CoxLoot.JOURNAL })
    }

    @Test
    fun `a rich raid almost always finds uniques and unique winners get no commons`() {
        val random = Random(5)
        var uniqueRaids = 0
        repeat(400) {
            val set =
                CoxRewardRoller.roll(
                    points = mapOf("a" to 400_000, "b" to 400_000),
                    teamPoints = 3_420_000,
                    challengeMode = true,
                    underTarget = false,
                    present = { true },
                    hasTablet = { true },
                    hasJournal = { true },
                    below = { random.nextInt(it) },
                )
            if (set.uniques.isNotEmpty()) uniqueRaids++
            assertTrue(set.uniques.size <= CoxLoot.MAX_ROLLS)
            for ((winner, item) in set.uniques) {
                val loot = set.items.getValue(winner)
                assertTrue(CoxItem(item.obj, 1) in loot)
                assertTrue(loot.none { c -> CoxLoot.COMMONS.any { it.obj == c.obj && it.fixedCount == null } })
            }
        }
        assertTrue(uniqueRaids > 390, "only $uniqueRaids of 400 raids found a unique")
    }

    @Test
    fun `kit and dust need a Challenge Mode run under the target`() {
        val none = { _: Int -> 0 }
        fun roll(cm: Boolean, under: Boolean) =
            CoxRewardRoller.roll(
                points = mapOf("a" to 1_000),
                teamPoints = 0,
                challengeMode = cm,
                underTarget = under,
                present = { true },
                hasTablet = { true },
                hasJournal = { true },
                below = none,
            ).items.getValue("a")
        assertTrue(roll(true, true).any { it.obj == CoxLoot.KIT })
        assertTrue(roll(true, true).any { it.obj == CoxLoot.DUST })
        assertNotEquals(true, roll(true, false).any { it.obj == CoxLoot.KIT })
        assertNotEquals(true, roll(false, true).any { it.obj == CoxLoot.DUST })
    }

    @Test
    fun `a winner who left the raid has the unique discarded`() {
        val set =
            CoxRewardRoller.roll(
                points = mapOf("gone" to 500_000, "here" to 1),
                teamPoints = 3_000_000,
                challengeMode = false,
                underTarget = false,
                present = { it == "here" },
                hasTablet = { true },
                hasJournal = { true },
                below = { 0 },
            )
        assertTrue("gone" !in set.items)
        assertTrue(set.uniques.isEmpty())
    }
}
