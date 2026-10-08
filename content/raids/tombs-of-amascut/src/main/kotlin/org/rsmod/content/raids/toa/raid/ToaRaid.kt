package org.rsmod.content.raids.toa.raid

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.invocation.ToaInvocations
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.party.ToaMode
import org.rsmod.content.raids.toa.party.ToaParty
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class ToaRaid(
    val party: ToaParty,
    val region: Region,
    val invocations: Set<ToaInvocation>,
    val sizeAtStart: Int,
) {
    internal val insiders = mutableListOf<Player>()
    internal val downed = mutableSetOf<Player>()
    internal var room: ToaRoom = ToaRoom.Nexus
    internal var startedAt: Int = -1
    internal var emptyTicks: Int = 0
    internal var attemptsLeft: Int = ToaInvocations.attempts(invocations) ?: Int.MAX_VALUE
    internal var timeExpired: Boolean = false
    internal var levelPenalty: Int = 0

    val baseLevel: Int = ToaInvocations.raidLevel(invocations)

    val raidLevel: Int
        get() = (baseLevel - levelPenalty).coerceAtLeast(0)

    val mode: ToaMode
        get() = ToaMode.of(raidLevel)

    val southWest: CoordGrid
        get() = region.southWest

    val started: Boolean
        get() = startedAt >= 0

    val alive: List<Player>
        get() = insiders.filter { it !in downed }

    operator fun contains(coords: CoordGrid): Boolean =
        coords.x in southWest.x until southWest.x + ToaRoom.REGION_LENGTH &&
            coords.z in southWest.z until southWest.z + ToaRoom.REGION_LENGTH

    fun roomAt(coords: CoordGrid): ToaRoom? = ToaRoom.at(southWest, coords)

    fun coords(room: ToaRoom, source: CoordGrid): CoordGrid = room.toInstance(southWest, source)

    fun arrival(room: ToaRoom): CoordGrid = coords(room, room.arrival)

    companion object {
        fun template(): RegionTemplate =
            RegionTemplate.create {
                for (room in ToaRoom.entries) {
                    for (zx in 0 until ToaRoom.ZONES) {
                        for (zz in 0 until ToaRoom.ZONES) {
                            val source = ZoneKey(room.zoneSourceX + zx, room.zoneSourceZ + zz, 0)
                            val x = room.slotColumn * ToaRoom.ZONES + zx
                            val z = room.slotRow * ToaRoom.ZONES + zz
                            this[x, z, room.plane] = source
                        }
                    }
                }
            }
    }
}
