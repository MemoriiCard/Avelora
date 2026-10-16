package org.rsmod.content.raids.tob.raid

import org.rsmod.content.raids.tob.layout.TobRoom

fun interface TobRoomFactory {
    fun create(raid: TobRaid, room: TobRoom, onCleared: () -> Unit): TobRoomController?
}
