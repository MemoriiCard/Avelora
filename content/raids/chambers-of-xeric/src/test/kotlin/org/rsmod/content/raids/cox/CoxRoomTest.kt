package org.rsmod.content.raids.cox

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.raids.cox.party.CoxScaling
import org.rsmod.content.raids.cox.room.CrabsRoom
import org.rsmod.content.raids.cox.room.IceDemonRoom
import org.rsmod.content.raids.cox.room.MysticsRoom
import org.rsmod.content.raids.cox.room.ScavengerDrops
import org.rsmod.content.raids.cox.room.ScavengersRoom
import org.rsmod.content.raids.cox.room.ShamansRoom
import org.rsmod.content.raids.cox.room.TektonRoom
import org.rsmod.content.raids.cox.room.ThievingRoom
import org.rsmod.content.raids.cox.room.ThievingScript
import org.rsmod.content.raids.cox.room.TightropeRoom
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
    fun `puzzle rooms scale with the party`() {
        assertEquals(30, ThievingRoom.grubsNeeded(1))
        assertEquals(48, ThievingRoom.grubsNeeded(3))
        assertEquals(3, CrabsRoom.crabCount(1))
        assertEquals(6, CrabsRoom.crabCount(100))
        assertEquals(2, TightropeRoom.deathlyCount(1))
        assertEquals(4, TightropeRoom.deathlyCount(100))
        assertEquals(3, ScavengersRoom.countFor(1))
        assertEquals(8, ScavengersRoom.countFor(100))
        assertEquals(40, IceDemonRoom.heatNeeded(1))
        assertEquals(88, IceDemonRoom.heatNeeded(4))
    }

    @Test
    fun `chest odds match the wiki at the ends of the range`() {
        assertEquals(392, ThievingScript.openChance(1, lockpick = false))
        assertEquals(607, ThievingScript.openChance(1, lockpick = true))
        assertEquals(607, ThievingScript.openChance(99, lockpick = false))
        assertEquals(823, ThievingScript.openChance(99, lockpick = true))
        assertEquals(1, ThievingScript.maxGrubs(1))
        assertEquals(4, ThievingScript.maxGrubs(100))
    }

    @Test
    fun `scavenger drop table covers every weight`() {
        assertEquals(18, ScavengerDrops.TOTAL_WEIGHT)
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
