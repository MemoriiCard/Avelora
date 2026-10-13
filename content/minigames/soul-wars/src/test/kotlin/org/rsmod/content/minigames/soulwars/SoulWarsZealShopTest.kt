package org.rsmod.content.minigames.soulwars

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SoulWarsZealShopTest {
    @Test
    fun `experience per zeal matches the wiki endpoints`() {
        assertEquals(30, ZealShop.xpPerZeal(ZealSkill.Attack, 30))
        assertEquals(30, ZealShop.xpPerZeal(ZealSkill.Attack, 34))
        assertEquals(480, ZealShop.xpPerZeal(ZealSkill.Strength, 98))
        assertEquals(480, ZealShop.xpPerZeal(ZealSkill.Hitpoints, 99))
        assertEquals(27, ZealShop.xpPerZeal(ZealSkill.Magic, 30))
        assertEquals(432, ZealShop.xpPerZeal(ZealSkill.Ranged, 99))
        assertEquals(14, ZealShop.xpPerZeal(ZealSkill.Prayer, 30))
        assertEquals(224, ZealShop.xpPerZeal(ZealSkill.Prayer, 98))
    }

    @Test
    fun `levels below thirty earn nothing`() {
        assertEquals(0, ZealShop.xpPerZeal(ZealSkill.Attack, 29))
    }

    @Test
    fun `stock prices follow the wiki`() {
        assertEquals(2500, ZealShop.ALL.single { it.obj == "obj.soul_cape_blue" }.cost)
        assertEquals(250, ZealShop.ALL.single { it.obj == "obj.soul_wars_ectoplasmator" }.cost)
        assertEquals(30, ZealShop.ALL.single { it.obj == "obj.soul_wars_spoils" }.cost)
        assertTrue((ZealShop.SUPPLIES + ZealShop.SACKS).all { it.cost == 10 })
        assertEquals(70, ZealShop.SACKS.single { it.obj == "obj.blighted_sack_entangle" }.quantity)
    }
}
