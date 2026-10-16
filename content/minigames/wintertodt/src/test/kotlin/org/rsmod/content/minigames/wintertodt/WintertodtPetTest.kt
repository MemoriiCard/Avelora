package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.other.pets.Pets

@ResourceLock("server-cache")
class WintertodtPetTest {
    @Test
    fun `the phoenix pet is a registered pet`() {
        val cache = ServerCacheManager.init(240)
        try {
            assertNotNull(Pets.forObj(WintertodtLoot.PET))
        } finally {
            cache.close()
        }
    }
}
