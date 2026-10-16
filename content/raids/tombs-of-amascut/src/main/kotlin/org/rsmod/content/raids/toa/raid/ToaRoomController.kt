package org.rsmod.content.raids.toa.raid

interface ToaRoomController {
    fun begin()

    fun tick() {}

    fun destroy() {}

    val cleared: Boolean
}
