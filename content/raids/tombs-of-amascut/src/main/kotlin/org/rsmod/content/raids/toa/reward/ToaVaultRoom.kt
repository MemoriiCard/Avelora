package org.rsmod.content.raids.toa.reward

import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoomController

class ToaVaultRoom(
    private val raid: ToaRaid,
    private val services: CoxRoomServices,
    private val rewards: ToaRewards,
    private val onCleared: () -> Unit,
) : ToaRoomController {
    private var openedAt = 0
    private var done = false

    override val cleared: Boolean
        get() = done

    override fun begin() {
        openedAt = services.cycle
        rewards.recordCompletion(raid)
        rewards.deal(raid)
    }

    override fun tick() {
        if (done) return
        val waited = services.cycle - openedAt
        if (waited < MIN_TICKS) return
        if (raid.rewards.isEmpty() || waited >= MAX_TICKS) {
            done = true
            onCleared()
        }
    }

    private companion object {
        const val MIN_TICKS = 10
        const val MAX_TICKS = 600
    }
}
