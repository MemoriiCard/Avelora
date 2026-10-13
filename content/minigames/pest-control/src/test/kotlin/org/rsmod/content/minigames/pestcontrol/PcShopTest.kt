package org.rsmod.content.minigames.pestcontrol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PcShopTest {
    @Test
    fun `experience per point scales with level and skill group`() {
        assertEquals(0, PcShop.xpPerPoint(PcSkill.Attack, 24))
        assertEquals(35, PcShop.xpPerPoint(PcSkill.Attack, 25))
        assertEquals(560, PcShop.xpPerPoint(PcSkill.Strength, 98))
        assertEquals(512, PcShop.xpPerPoint(PcSkill.Magic, 99))
        assertEquals(288, PcShop.xpPerPoint(PcSkill.Prayer, 99))
    }

    @Test
    fun `bulk exchanges add one and ten percent`() {
        assertEquals(0, PcShop.bulkBonusPercent(9))
        assertEquals(1, PcShop.bulkBonusPercent(10))
        assertEquals(1, PcShop.bulkBonusPercent(99))
        assertEquals(10, PcShop.bulkBonusPercent(100))
        assertEquals(35L * 100 * 110 / 100, PcShop.experience(PcSkill.Attack, 25, 100))
        assertEquals(35L * 10 + 3, PcShop.experience(PcSkill.Attack, 25, 10))
    }

    @Test
    fun `void prices and set totals match the wiki`() {
        val cost = PcShop.VOID.associate { it.obj to it.cost }
        assertEquals(250, cost.getValue("obj.pest_void_knight_top"))
        assertEquals(250, cost.getValue("obj.pest_void_knight_robes"))
        assertEquals(150, cost.getValue("obj.pest_void_knight_gloves"))
        assertEquals(200, cost.getValue("obj.game_pest_archer_helm"))
        assertEquals(250, cost.getValue("obj.pest_void_knight_mace"))
        val basicSet = 250 + 250 + 150 + 200
        assertEquals(850, basicSet)
        assertEquals(850 + 200 + 200, basicSet + PcShop.ELITE.sumOf { it.cost })
    }
}
