package org.rsmod.content.raids.tob.raid

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobParty
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class TobRaid(val party: TobParty, val region: Region, val mode: TobMode) {
    internal val insiders = mutableListOf<Player>()
    internal val downed = mutableSetOf<Player>()
    internal var room: TobRoom = TobRoom.Maiden
    internal var engaged: Boolean = false
    internal var roomCleared: Boolean = false
    internal var controller: TobRoomController? = null
    internal var startedAt: Int = -1
    internal var emptyTicks: Int = 0
    internal var sizeAtStart: Int = 0
    internal val rewards = mutableMapOf<Player, List<CoxItem>>()
    internal val chests = mutableMapOf<CoordGrid, Player>()

    val southWest: CoordGrid
        get() = region.southWest

    val started: Boolean
        get() = startedAt >= 0

    val alive: List<Player>
        get() = insiders.filter { it !in downed }

    operator fun contains(coords: CoordGrid): Boolean =
        coords.x in southWest.x until southWest.x + TobRoom.REGION_LENGTH &&
            coords.z in southWest.z until southWest.z + TobRoom.REGION_LENGTH

    fun roomAt(coords: CoordGrid): TobRoom? = TobRoom.at(southWest, coords)

    fun coords(room: TobRoom, source: CoordGrid): CoordGrid = room.toInstance(southWest, source)

    fun arrival(room: TobRoom): CoordGrid = coords(room, room.arrival)

    fun source(instance: CoordGrid): CoordGrid? =
        roomAt(instance)?.toSource(southWest, instance)

    companion object {
        fun template(): RegionTemplate =
            RegionTemplate.create {
                for (room in TobRoom.entries) {
                    for (zx in 0 until TobRoom.ZONES) {
                        for (zz in 0 until TobRoom.ZONES) {
                            val source =
                                ZoneKey(room.zoneSourceX + zx, room.zoneSourceZ + zz, room.level)
                            val x = room.slotColumn * TobRoom.ZONES + zx
                            val z = room.slotRow * TobRoom.ZONES + zz
                            this[x, z, room.plane] = source
                        }
                    }
                }
            }
    }
}
