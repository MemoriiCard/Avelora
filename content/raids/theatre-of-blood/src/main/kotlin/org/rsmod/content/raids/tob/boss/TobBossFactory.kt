package org.rsmod.content.raids.tob.boss

import jakarta.inject.Inject
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.bloat.BloatRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.maiden.MaidenRoom
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.content.raids.tob.raid.TobRoomController
import org.rsmod.content.raids.tob.raid.TobRoomFactory

class TobBossFactory @Inject constructor(private val services: CoxRoomServices) : TobRoomFactory {
    override fun create(raid: TobRaid, room: TobRoom, onCleared: () -> Unit): TobRoomController? =
        when (room) {
            TobRoom.Maiden -> MaidenRoom(raid, services, onCleared)
            TobRoom.Bloat -> BloatRoom(raid, services, onCleared)
            else -> null
        }
}
