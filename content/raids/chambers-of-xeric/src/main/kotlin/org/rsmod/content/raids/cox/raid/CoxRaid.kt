package org.rsmod.content.raids.cox.raid

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.raids.cox.layout.CoxCell
import org.rsmod.content.raids.cox.layout.CoxLayout
import org.rsmod.content.raids.cox.layout.CoxLayoutGenerator
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.party.CoxParty
import org.rsmod.content.raids.cox.party.CoxScaling
import org.rsmod.content.raids.cox.reward.CoxItem
import org.rsmod.content.raids.cox.room.CoxRoomController
import org.rsmod.content.raids.cox.storage.CoxStorage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class CoxRaid(val party: CoxParty, val layout: CoxLayout, val region: Region) {
    internal var scaling: CoxScaling.Snapshot = CoxScaling.snapshot(party)
    internal var startedAt: Int = -1
    internal var completedAt: Int = -1
    internal var challengeMode: Boolean = party.challengeMode
    internal val points = mutableMapOf<Player, Int>()
    internal val insiders = mutableListOf<Player>()
    internal var totalPoints: Int = 0
    internal var deepestFloor: Int = 0
    internal var emptyTicks: Int = 0
    internal var foodUnits: Int = 0
    internal val storage = CoxStorage()
    internal val rakes = mutableMapOf<Player, Int>()
    internal var potionUnits: Int = 0
    internal val rewards = mutableMapOf<Player, List<CoxItem>>()
    internal val rooms = mutableListOf<CoxRoomController>()

    val started: Boolean
        get() = startedAt >= 0

    val southWest: CoordGrid
        get() = region.southWest

    operator fun contains(coords: CoordGrid): Boolean =
        coords.x in southWest.x until southWest.x + REGION_LENGTH &&
            coords.z in southWest.z until southWest.z + REGION_LENGTH

    /** Converts a tile in a room's (unrotated) template to its instance coordinates. */
    fun coords(room: CoxRoom, localX: Int, localZ: Int): CoordGrid {
        val plane = layout.floors.first { room in it.rooms }.plane
        val (rx, rz) = rotate(room.rotation, localX, localZ)
        return cellBase(room.cell, plane).translate(rx, rz)
    }

    fun controllerOf(npc: Npc): CoxRoomController? = rooms.firstOrNull { it.owns(npc) }

    fun controllerOf(room: CoxRoom): CoxRoomController? = rooms.firstOrNull { it.room === room }

    /** True when [to] lies further along the raid than [from] on the same floor. */
    fun isForward(from: CoxRoom, to: CoxRoom): Boolean {
        val floor = layout.floors.firstOrNull { from in it.rooms } ?: return false
        return floor.rooms.indexOf(to) > floor.rooms.indexOf(from)
    }

    fun roomAt(coords: CoordGrid): CoxRoom? {
        val dx = coords.x - southWest.x
        val dz = coords.z - southWest.z
        if (dx !in 0 until GRID_WIDTH || dz !in 0 until GRID_HEIGHT) return null
        val cell = CoxCell(dx / CoxCell.SIZE, CoxCell.ROWS - 1 - dz / CoxCell.SIZE)
        return layout.roomAt(coords.level, cell)
    }

    fun floorIndexAt(coords: CoordGrid): Int? =
        if (coords.level == CoxLayoutGenerator.OLM_PLANE) layout.floors.size
        else layout.floorOn(coords.level)?.index

    fun olmCoords(staticCoords: CoordGrid): CoordGrid =
        CoordGrid(
            southWest.x + staticCoords.x - OLM_TEMPLATE_X,
            southWest.z + OLM_REGION_OFFSET_Z + staticCoords.z - OLM_TEMPLATE_Z,
            CoxLayoutGenerator.OLM_PLANE,
        )

    fun inOlmRoom(coords: CoordGrid): Boolean =
        coords.level == CoxLayoutGenerator.OLM_PLANE && coords in this

    private fun cellBase(cell: CoxCell, plane: Int): CoordGrid =
        CoordGrid(
            southWest.x + cell.column * CoxCell.SIZE,
            southWest.z + (CoxCell.ROWS - 1 - cell.row) * CoxCell.SIZE,
            plane,
        )

    companion object {
        const val REGION_LENGTH = 128
        private const val GRID_WIDTH = CoxCell.COLUMNS * CoxCell.SIZE
        private const val GRID_HEIGHT = CoxCell.ROWS * CoxCell.SIZE
        private const val ROOM_ZONES = CoxCell.SIZE / 8

        const val OLM_TEMPLATE_X = 3200
        const val OLM_TEMPLATE_Z = 5696
        private const val OLM_TEMPLATE_ZONES = 8
        private const val OLM_REGION_OFFSET_Z = GRID_HEIGHT

        fun rotate(rotation: Int, x: Int, z: Int, size: Int = CoxCell.SIZE): Pair<Int, Int> {
            val max = size - 1
            return when (rotation) {
                1 -> z to max - x
                2 -> max - x to max - z
                3 -> max - z to x
                else -> x to z
            }
        }

        fun template(layout: CoxLayout) =
            RegionTemplate.create {
                for (floor in layout.floors) {
                    for (room in floor.rooms) {
                        val baseZoneX = room.cell.column * ROOM_ZONES
                        val baseZoneZ = (CoxCell.ROWS - 1 - room.cell.row) * ROOM_ZONES
                        for (zx in 0 until ROOM_ZONES) {
                            for (zz in 0 until ROOM_ZONES) {
                                val source =
                                    ZoneKey(
                                        room.templateX / 8 + zx,
                                        room.type.templateZ / 8 + zz,
                                        room.type.templateLevel,
                                    )
                                val (dx, dz) = rotate(room.rotation, zx, zz, ROOM_ZONES)
                                val x = baseZoneX + dx
                                val z = baseZoneZ + dz
                                when (room.rotation) {
                                    1 -> this[x, z, floor.plane] = source.rotate90()
                                    2 -> this[x, z, floor.plane] = source.rotate180()
                                    3 -> this[x, z, floor.plane] = source.rotate270()
                                    else -> this[x, z, floor.plane] = source
                                }
                            }
                        }
                    }
                }
                for (zx in 0 until OLM_TEMPLATE_ZONES) {
                    for (zz in 0 until OLM_TEMPLATE_ZONES) {
                        val source = ZoneKey(OLM_TEMPLATE_X / 8 + zx, OLM_TEMPLATE_Z / 8 + zz, 0)
                        this[zx, OLM_REGION_OFFSET_Z / 8 + zz, CoxLayoutGenerator.OLM_PLANE] = source
                    }
                }
            }
    }
}
