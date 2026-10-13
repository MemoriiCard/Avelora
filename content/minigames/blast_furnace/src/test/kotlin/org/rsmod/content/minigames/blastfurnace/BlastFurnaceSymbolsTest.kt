package org.rsmod.content.minigames.blastfurnace

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class BlastFurnaceSymbolsTest {
    @Test
    fun `every referenced symbol resolves in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val items = Ore.entries.map { it.obj } + Bar.entries.map { it.obj } +
                listOf("obj.coal", "obj.spade", "obj.bucket_empty", "obj.bucket_water", "obj.coins", "obj.ice_gloves")
            for (item in items) assertNotNull(ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)), item)
            val varbits = Ore.entries.map { it.varbit } + Bar.entries.map { it.barVarbit } +
                listOf(
                    "varbit.blast_furnace_coal",
                    "varbit.blast_furnace_temperature_reading",
                    "varbit.blast_furnace_bars_hot",
                    "varbit.blast_furnace_coal_required",
                )
            for (varbit in varbits) assertNotNull(ServerCacheManager.getVarbit(varbit.asRSCM(RSCMType.VARBIT)), varbit)
            val locs =
                listOf(
                    "loc.dwarf_keldagrim_factory_stairs",
                    "loc.blast_furnace_stairs_up",
                    "loc.blast_furnace_conveyer_belt_clickable",
                    "loc.blast_furnace_stove_low",
                    "loc.blast_furnace_stove_medium",
                    "loc.blast_furnace_stove_full",
                    "loc.blast_furnace_coke",
                    "loc.blast_furnace_pump",
                    "loc.blast_furnace_pedals",
                    "loc.blast_furnace_gauge",
                    "loc.blast_furnace_chimney",
                    "loc.blast_furnace_sink",
                    "loc.blast_furnace_ore_dispenser_empty",
                    "loc.blast_furnace_ore_dispenser_full",
                    "loc.blast_furnace_ore_dispenser_cooled",
                )
            for (loc in locs) assertNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)), loc)
            assertNotNull(ServerCacheManager.getNpc("npc.blast_furnace_instructor".asRSCM(RSCMType.NPC)))
            "timer.blast_furnace".asRSCM(RSCMType.TIMER)
            "obj.smithing_uniform_gloves_ice".asRSCM(RSCMType.OBJ)
            "obj.gauntlets_of_goldsmithing".asRSCM(RSCMType.OBJ)
        } finally {
            cache.close()
        }
    }
}
