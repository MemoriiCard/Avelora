package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.raids.cox.party.CoxScaling
import org.rsmod.content.raids.cox.room.MysticsRoom
import org.rsmod.content.raids.cox.room.ShamansRoom
import org.rsmod.content.raids.cox.room.TektonRoom
import org.rsmod.content.raids.cox.room.VespulaRoom
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

@ResourceLock("server-cache")
class CoxRoomTest {
    @Test
    fun `group rooms grow with the party`() {
        assertEquals(2, ShamansRoom.countFor(1))
        assertEquals(3, ShamansRoom.countFor(5))
        assertEquals(5, ShamansRoom.countFor(100))
        assertEquals(3, MysticsRoom.countFor(1))
        assertEquals(4, MysticsRoom.countFor(3))
        assertEquals(12, MysticsRoom.countFor(100))
    }

    @Test
    fun `room stats apply party scaling to the npc`() {
        val cache = ServerCacheManager.init(240)
        try {
            val solo = CoxScaling.Snapshot(1, 126, 99, false, averageMining = 99)
            val tekton = npc(TektonRoom.TEKTON)
            TektonRoom.STATS.applyTo(tekton, solo)
            assertEquals(300, tekton.hitpoints)
            assertEquals(390, tekton.attackLvl)
            assertEquals(205, tekton.defenceLvl)

            val trio = CoxScaling.Snapshot(3, 126, 99, false, averageMining = 99)
            val soldier = npc(VespulaRoom.SOLDIER)
            VespulaRoom.SOLDIER_STATS.applyTo(soldier, trio)
            assertEquals(100, soldier.hitpoints)
            assertEquals(150, soldier.attackLvl)
        } finally {
            cache.close()
        }
    }

    private fun npc(name: String): Npc =
        Npc(ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!, CoordGrid(3200, 5200, 0))
}
