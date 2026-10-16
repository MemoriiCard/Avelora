package org.rsmod.content.minigames.soulwars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class SoulWarsSymbolsTest {
    private val locs =
        listOf(
            "loc.soul_wars_leave_soulwars_portal",
            "loc.soul_wars_blue_exit_portal",
            "loc.soul_wars_red_exit_portal",
            "loc.soul_wars_central_obelisk_neutral",
            "loc.soul_wars_central_obelisk_blue",
            "loc.soul_wars_central_obelisk_red",
        ) +
            listOf("blue", "red", "neutral").flatMap {
                listOf("loc.soul_wars_counter_bandages_$it", "loc.soul_wars_counter_buff_potions_$it")
            } +
            listOf("blue", "red").flatMap {
                listOf("loc.soul_wars_counter_potions_$it", "loc.soul_wars_counter_barricades_$it")
            }

    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            val items =
                listOf(
                    SoulWarsItems.FRAGMENT,
                    SoulWarsItems.BONES,
                    SoulWarsItems.BANDAGES,
                    SoulWarsItems.POWER_POTION,
                    SoulWarsItems.EXPLOSIVE_POTION,
                    "obj.soul_cape_red",
                ) +
                    SwTeam.entries.map { SoulWarsItems.cape(it) } +
                    SwTeam.entries.map { SoulWarsItems.barricade(it) } +
                    ZealShop.ALL.map { it.obj }
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            for (nomad in listOf("npc.soul_wars_nomad_1op", "npc.soul_wars_nomad_2op", "npc.soul_wars_nomad")) {
                assertNotNull(ServerCacheManager.getNpc(nomad.asRSCM(RSCMType.NPC)), nomad)
            }
            for (team in SwTeam.entries) {
                val npc = SoulWarsItems.avatar(team)
                assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            }
            assertNotNull(ServerCacheManager.getVarp(SoulWarsService.ZEAL.asRSCM(RSCMType.VARP)))
            for (bot in SoulWarsBots.SYMBOLS.values) assertNotNull(ServerCacheManager.getNpc(bot.asRSCM(RSCMType.NPC)), bot)
            for (soul in SoulKind.entries) assertNotNull(ServerCacheManager.getNpc(soul.symbol.asRSCM(RSCMType.NPC)), soul.symbol)
            for (dose in 1..SoulWarsSupplies.POTION_DOSES) assertNotNull(ServerCacheManager.getItem(SoulWarsSupplies.potion(dose).asRSCM(RSCMType.OBJ)))
            assertEquals(65531, SoulWarsService.TIMER.asRSCM(RSCMType.TIMER))
        } finally {
            cache.close()
        }
    }
}
