package org.rsmod.content.raids.toa.raid

import org.rsmod.content.raids.toa.layout.ToaRoom

fun interface ToaRoomFactory {
    fun create(raid: ToaRaid, room: ToaRoom, onCleared: () -> Unit): ToaRoomController?
}
