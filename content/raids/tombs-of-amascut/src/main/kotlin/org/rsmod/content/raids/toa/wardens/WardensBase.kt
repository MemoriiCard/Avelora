package org.rsmod.content.raids.toa.wardens

import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.map.CoordGrid

abstract class WardensBase(
    raid: ToaRaid,
    room: ToaRoom,
    services: CoxRoomServices,
    onCleared: () -> Unit,
) : ToaBossRoom(raid, room, services, onCleared) {
    private class Strike(val centre: CoordGrid, val damage: Int, val landsAt: Int)

    private val strikes = mutableListOf<Strike>()

    protected fun queueStrike(centre: CoordGrid, damage: Int, delay: Int = WardensRules.STRIKE_DELAY) {
        strikes += Strike(centre, damage, clock + delay)
    }

    protected fun landStrikes() {
        if (strikes.isEmpty()) return
        val due = strikes.filter { clock >= it.landsAt }
        if (due.isEmpty()) return
        strikes.removeAll(due.toSet())
        for (strike in due) {
            for (player in playersInRoom()) {
                if (WardensRules.inStrike(player.coords, strike.centre)) {
                    hurt(player, scaledDamage(services.random.of(strike.damage / 2, strike.damage)))
                }
            }
        }
    }

    protected fun clearStrikes() = strikes.clear()

    protected fun randomTile(x: IntRange, z: IntRange): CoordGrid =
        world(CoordGrid(x.first + services.random.of(x.count()), z.first + services.random.of(z.count()), 0))
}
