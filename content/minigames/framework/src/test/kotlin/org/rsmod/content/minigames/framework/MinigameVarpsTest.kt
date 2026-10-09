package org.rsmod.content.minigames.framework

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("server-cache")
class MinigameVarpsTest {
    @Test
    fun `profile varps are packed`() {
        val cache = ServerCacheManager.init(240)
        try {
            for (varp in listOf("varp.minigame_tokens", "varp.minigame_first_win_day")) {
                assertNotNull(ServerCacheManager.getVarp(varp.asRSCM(RSCMType.VARP)), varp)
            }
        } finally {
            cache.close()
        }
    }
}
