package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.raids.cox.olm.OlmAttack
import org.rsmod.content.raids.cox.olm.OlmPos
import org.rsmod.content.raids.cox.olm.OlmPower
import org.rsmod.content.raids.cox.olm.OlmRules
import org.rsmod.content.raids.cox.olm.OlmSide
import org.rsmod.content.raids.cox.olm.OlmStep
import org.rsmod.content.raids.cox.olm.OlmZone
import org.rsmod.content.raids.cox.party.CoxScaling

class OlmRulesTest {
    @Test
    fun `rotation matches the wiki order`() {
        val standards = OlmRules.ROTATION.count { it == OlmStep.Standard }
        assertEquals(12, OlmRules.ROTATION.size)
        assertEquals(6, standards)
        assertEquals(OlmStep.CrystalBurst, OlmRules.ROTATION[3])
        assertEquals(OlmStep.Lightning, OlmRules.ROTATION[7])
        assertEquals(OlmStep.Swap, OlmRules.ROTATION[11])
    }

    @Test
    fun `early phases get one power each and every power appears before repeating`() {
        val random = Random(1)
        repeat(100) {
            val plan = OlmRules.powersByPhase(7) { random.nextInt(it) }
            assertEquals(7, plan.size)
            assertTrue(plan.take(5).all { it.size == 1 })
            assertEquals(OlmPower.entries.toSet(), plan[5])
            assertEquals(OlmPower.entries.toSet(), plan[6])
            assertEquals(OlmPower.entries.toSet(), plan.take(3).flatten().toSet())
        }
    }

    @Test
    fun `powers do not repeat an attack within a phase`() {
        val all = OlmPower.entries.toSet()
        assertEquals(6, OlmRules.availableAttacks(all, emptySet()).size)
        val left = OlmRules.availableAttacks(all, setOf(OlmAttack.Burn, OlmAttack.AcidSpray))
        assertFalse(OlmAttack.Burn in left)
        assertEquals(4, left.size)
    }

    @Test
    fun `max hits climb through the last two phases`() {
        assertEquals(27, OlmRules.maxHit(0, 4, false))
        assertEquals(27, OlmRules.maxHit(1, 4, false))
        assertEquals(28, OlmRules.maxHit(2, 4, false))
        assertEquals(29, OlmRules.maxHit(3, 4, false))
        assertEquals(38, OlmRules.maxHit(0, 4, true))
        assertEquals(41, OlmRules.maxHit(3, 4, true))
    }

    @Test
    fun `specials stop in the final phase`() {
        assertTrue(OlmRules.hasSpecials(2, 4))
        assertFalse(OlmRules.hasSpecials(3, 4))
    }

    @Test
    fun `zones flip with the side the head stands on`() {
        assertEquals(OlmZone.Left, OlmRules.zoneOf(5745, OlmSide.West))
        assertEquals(OlmZone.Right, OlmRules.zoneOf(5731, OlmSide.West))
        assertEquals(OlmZone.Middle, OlmRules.zoneOf(5738, OlmSide.West))
        assertEquals(OlmZone.Right, OlmRules.zoneOf(5745, OlmSide.East))
        assertEquals(OlmZone.Left, OlmRules.zoneOf(5731, OlmSide.East))
        assertEquals(OlmZone.Middle, OlmRules.zoneOf(5739, OlmSide.East))
    }

    @Test
    fun `the head turns to the busiest zone and stays put on a tie`() {
        val counts = mapOf(OlmZone.Left to 2, OlmZone.Middle to 2, OlmZone.Right to 1)
        assertEquals(OlmZone.Middle, OlmRules.busiestZone(counts, OlmZone.Middle))
        assertEquals(OlmZone.Left, OlmRules.busiestZone(counts, OlmZone.Right))
        assertEquals(null, OlmRules.busiestZone(mapOf(OlmZone.Left to 0), OlmZone.Left))
    }

    @Test
    fun `swap and bomb damage follow distance`() {
        assertEquals(0, OlmRules.swapDamage(0))
        assertEquals(5, OlmRules.swapDamage(1))
        assertEquals(10, OlmRules.swapDamage(2))
        assertEquals(50, OlmRules.swapDamage(30))
        assertEquals(15, OlmRules.bombDamage(4))
        assertEquals(30, OlmRules.bombDamage(3))
        assertEquals(60, OlmRules.bombDamage(1))
        assertEquals(60, OlmRules.bombDamage(0))
        assertEquals(0, OlmRules.bombDamage(5))
    }

    @Test
    fun `the left claw clenches at a twentieth of its health inside the window`() {
        assertFalse(OlmRules.clenches(29, 600))
        assertTrue(OlmRules.clenches(30, 600))
    }

    @Test
    fun `swaps pair players off without reusing anyone`() {
        val random = Random(9)
        val pairs = OlmRules.pairUp((1..9).toList()) { random.nextInt(it) }
        assertEquals(OlmRules.MAX_SWAP_PAIRS, pairs.size)
        val flat = pairs.flatMap { listOf(it.first, it.second) }
        assertEquals(flat.size, flat.toSet().size)
        assertEquals(1, OlmRules.pairUp(listOf(1, 2, 3)) { 0 }.size)
    }

    @Test
    fun `siphon heals five times the damage`() {
        assertEquals(90, OlmRules.siphonHeal(18))
        assertEquals(16, OlmRules.crystalDamage(0) { a, _ -> a })
        assertEquals(10, OlmRules.crystalDamage(1) { a, _ -> a })
        assertEquals(0, OlmRules.crystalDamage(2) { a, _ -> a })
    }

    @Test
    fun `claws sit five tiles either side of the head`() {
        for (layout in listOf(OlmRules.WEST, OlmRules.EAST)) {
            assertEquals(5, OlmRules.distance(layout.head, layout.leftHand))
            assertEquals(5, OlmRules.distance(layout.head, layout.rightHand))
        }
        assertEquals(OlmPos(3220, 5743), OlmRules.WEST.leftHand)
        assertEquals(OlmPos(3238, 5733), OlmRules.EAST.leftHand)
    }

    @Test
    fun `head and claw health scale with party size`() {
        fun snapshot(size: Int) =
            CoxScaling.Snapshot(size, 126, 99, false, 99)
        assertEquals(4, snapshot(1).olmPhases)
        assertEquals(5, snapshot(8).olmPhases)
        assertEquals(6, snapshot(16).olmPhases)
        assertEquals(800, snapshot(1).olmHeadHitpoints)
        assertEquals(600, snapshot(1).olmHandHitpoints)
        assertEquals(800 + 400 * 4, snapshot(5).olmHeadHitpoints)
        assertTrue(snapshot(24).olmHeadHitpoints > snapshot(7).olmHeadHitpoints - 1)
    }
}

@ResourceLock("server-cache")
class OlmGamevalTest {
    @Test
    fun `every symbol the fight uses resolves`() {
        val cache = ServerCacheManager.init(240)
        try {
            for ((type, names) in SYMBOLS) for (name in names) name.asRSCM(type)
        } finally {
            cache.close()
        }
    }

    private companion object {
        val SYMBOLS =
            mapOf(
                RSCMType.NPC to
                    listOf("npc.olm_head", "npc.olm_hand_left", "npc.olm_hand_right"),
                RSCMType.LOC to
                    listOf(
                        "loc.olm_acid_pool",
                        "loc.olm_crystal_bomb",
                        "loc.olm_crystal_attack_small",
                    ),
                RSCMType.SEQ to
                    listOf(
                        "seq.olm_head_spawn",
                        "seq.olm_head_spawn_enraged",
                        "seq.olm_head_idle_front",
                        "seq.olm_head_idle_left",
                        "seq.olm_head_idle_right",
                        "seq.olm_head_idle_front_enraged",
                        "seq.olm_head_idle_left_enraged",
                        "seq.olm_head_idle_right_enraged",
                        "seq.olm_head_turn_left",
                        "seq.olm_head_turn_right",
                        "seq.olm_head_attack_acid_front",
                        "seq.olm_head_attack_acid_left",
                        "seq.olm_head_attack_acid_right",
                        "seq.olm_hand_left_spawn",
                        "seq.olm_hand_right_spawn",
                        "seq.olm_hand_left_cast_stun",
                        "seq.olm_hand_left_cast_earthquake",
                        "seq.olm_hand_left_cast_earthshock",
                    ),
                RSCMType.SPOTANIM to
                    listOf(
                        "spotanim.olm_weak_mage_proj",
                        "spotanim.olm_weak_mage_impact",
                        "spotanim.olm_weak_range_proj",
                        "spotanim.olm_weak_range_impact",
                        "spotanim.olm_weak_melee_proj",
                        "spotanim.olm_playerswap_0",
                        "spotanim.olm_playerswap_1",
                        "spotanim.olm_playerswap_2",
                        "spotanim.olm_playerswap_3",
                        "spotanim.olm_healme_spotanim",
                        "spotanim.olm_crystalrock_falling",
                        "spotanim.olm_crystal_explode",
                        "spotanim.olm_crystalwave_shatter",
                        "spotanim.olm_shockwave",
                    ),
                RSCMType.VARBIT to
                    listOf(
                        "varbit.prayer_protectfrommelee",
                        "varbit.prayer_protectfrommissiles",
                        "varbit.prayer_protectfrommagic",
                    ),
            )
    }
}
