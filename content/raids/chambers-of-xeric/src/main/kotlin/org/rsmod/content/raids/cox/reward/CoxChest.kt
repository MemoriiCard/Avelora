package org.rsmod.content.raids.cox.reward

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/** Turns the crystal in the Olm's lair into the ancient chest once the raid is complete. */
@Singleton
class CoxChest @Inject constructor(private val services: CoxRoomServices) {
    fun reveal(raid: CoxRaid) {
        val locRepo = services.boss.locRepo
        if (find(raid, CHEST) != null) return
        val crystal = find(raid, CRYSTAL)
        if (crystal != null) {
            locRepo.add(crystal.coords, CHEST, Int.MAX_VALUE, crystal.angle, crystal.shape)
            return
        }
        locRepo.add(
            raid.olmCoords(FALLBACK),
            CHEST,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    private fun find(raid: CoxRaid, internal: String): LocInfo? {
        val id = internal.asRSCM(RSCMType.LOC)
        val origin = raid.olmCoords(CoordGrid(CoxRaid.OLM_TEMPLATE_X, CoxRaid.OLM_TEMPLATE_Z, 0))
        for (zx in 0 until ZONES) {
            for (zz in 0 until ZONES) {
                val zone = ZoneKey.from(origin.translate(zx * ZONE_SIZE, zz * ZONE_SIZE))
                val match = services.boss.locRepo.findAll(zone).firstOrNull { it.id == id }
                if (match != null) return match
            }
        }
        return null
    }

    companion object {
        const val CRYSTAL = "loc.raids_reward_crystal"
        const val CHEST = "loc.raids_reward_chest"
        private val FALLBACK = CoordGrid(3233, 5726, 0)
        private const val ZONES = 8
        private const val ZONE_SIZE = 8
    }
}
