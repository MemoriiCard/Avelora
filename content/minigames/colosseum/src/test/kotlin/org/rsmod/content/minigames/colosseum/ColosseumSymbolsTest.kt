package org.rsmod.content.minigames.colosseum

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class ColosseumSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val npcs = ColosseumMonster.entries.map { it.npc } + MINIMUS_NPC
            for (npc in npcs) assertNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)), npc)
            for (loc in listOf("loc.colosseum_entrance", "loc.colosseum_exit", "loc.colosseum_exit_lobby_noop")) {
                assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            }
            val items =
                listOf(
                    ColosseumRewards.SPLINTERS,
                    ColosseumRewards.QUIVER,
                    ColosseumRewards.PET,
                    "obj.sunfire_helm",
                    "obj.sunfire_body",
                    "obj.sunfire_legs",
                    "obj.echo_crystal",
                    "obj.tonalztics_of_ralos_uncharged",
                )
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            for (varp in listOf("varp.colosseum_glory", "varp.total_colosseum_waves_completed")) {
                assertNotNull(ServerCacheManager.getVarp(varp.asRSCM(RSCMType.VARP)), varp)
            }
            for (seq in ColosseumMonster.entries.map { it.deathSeq }) seq.asRSCM(RSCMType.SEQ)
        } finally {
            cache.close()
        }
    }
}
