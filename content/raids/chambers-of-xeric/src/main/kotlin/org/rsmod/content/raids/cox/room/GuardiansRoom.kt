package org.rsmod.content.raids.cox.room

import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc

/** Two stone guardians flank the passage; only pickaxes can chip them down. */
class GuardiansRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    override fun spawn() {
        val (left, right) =
            pick(
                (11 to 22) to (11 to 16),
                (12 to 23) to (18 to 23),
                (17 to 25) to (17 to 19),
            )
        val hitpoints = raid.scaling.guardianHitpoints()
        spawnNpc(LEFT, left.first, left.second, STATS, hitpoints = hitpoints).movementLocked = true
        spawnNpc(RIGHT, right.first, right.second, STATS, hitpoints = hitpoints).movementLocked = true
    }

    override fun onNpcRemoved(npc: Npc) {
        val rubble = if (npc.isType(LEFT)) LEFT_DEAD else RIGHT_DEAD
        spawnAt(rubble, npc.coords, stats = null, required = false)
    }

    companion object {
        const val LEFT = "npc.raids_stoneguardians_left"
        const val RIGHT = "npc.raids_stoneguardians_right"
        private const val LEFT_DEAD = "npc.raids_stoneguardians_left_dead"
        private const val RIGHT_DEAD = "npc.raids_stoneguardians_right_dead"

        val STATS = CoxNpcStats(hitpoints = 151, attack = 140, strength = 140, defence = 100)
    }
}
