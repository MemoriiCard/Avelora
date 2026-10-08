package org.rsmod.content.raids.toa.boss

import jakarta.inject.Inject
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.kephri.KephriRoom
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.puzzle.CrondisPuzzleRoom
import org.rsmod.content.raids.toa.puzzle.ScabarasPuzzleRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoomController
import org.rsmod.content.raids.toa.raid.ToaRoomFactory
import org.rsmod.content.raids.toa.zebak.ZebakRoom

class ToaBossFactory @Inject constructor(private val services: CoxRoomServices) : ToaRoomFactory {
    override fun create(raid: ToaRaid, room: ToaRoom, onCleared: () -> Unit): ToaRoomController? =
        when (room) {
            ToaRoom.CrondisPuzzle -> CrondisPuzzleRoom(raid, services, onCleared)
            ToaRoom.Zebak -> ZebakRoom(raid, services, onCleared)
            ToaRoom.ScabarasPuzzle -> ScabarasPuzzleRoom(raid, services, onCleared)
            ToaRoom.Kephri -> KephriRoom(raid, services, onCleared)
            ToaRoom.Nexus -> null
        }
}
