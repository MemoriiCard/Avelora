package org.rsmod.content.raids.toa.layout

enum class ToaPath(
    val label: String,
    val puzzle: ToaRoom,
    val boss: ToaRoom,
    val door: String,
) {
    Crondis("Crondis", ToaRoom.CrondisPuzzle, ToaRoom.Zebak, "loc.toa_nexus_crondis_door"),
    Scabaras("Scabaras", ToaRoom.ScabarasPuzzle, ToaRoom.Kephri, "loc.toa_nexus_scabaras_door"),
    Het("Het", ToaRoom.HetPuzzle, ToaRoom.Akkha, "loc.toa_nexus_het_door"),
    Apmeken("Apmeken", ToaRoom.ApmekenPuzzle, ToaRoom.Baba, "loc.toa_nexus_apmeken_door");

    companion object {
        fun forRoom(room: ToaRoom): ToaPath? = entries.firstOrNull { room == it.puzzle || room == it.boss }
    }
}
