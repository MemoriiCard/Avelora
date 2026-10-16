package org.rsmod.content.raids.tob.raid

interface TobRoomController {
    fun begin()

    fun tick() {}

    fun destroy() {}

    val cleared: Boolean
}
