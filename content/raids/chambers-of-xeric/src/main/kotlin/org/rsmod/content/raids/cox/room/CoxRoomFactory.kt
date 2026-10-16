package org.rsmod.content.raids.cox.room

import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.layout.CoxRoomType
import org.rsmod.content.raids.cox.raid.CoxRaid

object CoxRoomFactory {
    fun create(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices): CoxRoomController? =
        when (room.type) {
            CoxRoomType.Tekton -> TektonRoom(raid, room, services)
            CoxRoomType.Vasa -> VasaRoom(raid, room, services)
            CoxRoomType.Vanguards -> VanguardsRoom(raid, room, services)
            CoxRoomType.Guardians -> GuardiansRoom(raid, room, services)
            CoxRoomType.Shamans -> ShamansRoom(raid, room, services)
            CoxRoomType.Mystics -> MysticsRoom(raid, room, services)
            CoxRoomType.Muttadiles -> MuttadilesRoom(raid, room, services)
            CoxRoomType.Vespula -> VespulaRoom(raid, room, services)
            CoxRoomType.IceDemon -> IceDemonRoom(raid, room, services)
            CoxRoomType.Thieving -> ThievingRoom(raid, room, services)
            CoxRoomType.Crabs -> CrabsRoom(raid, room, services)
            CoxRoomType.Tightrope -> TightropeRoom(raid, room, services)
            CoxRoomType.Scavengers,
            CoxRoomType.ScavengersLarge -> ScavengersRoom(raid, room, services)
            else -> null
        }
}
