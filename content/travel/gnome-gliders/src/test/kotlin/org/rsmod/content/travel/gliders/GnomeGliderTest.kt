package org.rsmod.content.travel.gliders

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class GnomeGliderTest {
    @Test
    fun `map buttons and pilot variants resolve in the cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (destination in GliderDestination.entries) {
                destination.button.let { "component.glidermap:$it".asRSCM(RSCMType.COMPONENT) }
            }
            val pilot = ServerCacheManager.getNpc("npc.pilot_grand_tree".asRSCM(RSCMType.NPC))
            assertNotNull(pilot)
            assertEquals(8, pilot?.transforms?.size)
            "varbit.pilot_previous_destination".asRSCM(RSCMType.VARBIT)
        } finally {
            cache.close()
        }
    }

    @Test
    fun `destination codes are unique and cover the previous destination varbit`() {
        assertEquals(GliderDestination.entries.size, GliderDestination.entries.map { it.code }.toSet().size)
        assertEquals(GliderDestination.Karamja, GliderDestination.byCode(1))
    }
}
