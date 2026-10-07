package org.rsmod.content.skills.hunter

import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

enum class TrapState {
    Set,
    Catching,
    Failing,
    Caught,
    Broken,
}

class Trap(
    val owner: Player,
    val kind: TrapKind,
    var loc: LocInfo,
    var state: TrapState,
    var expiresAt: Int,
    var creature: HunterCreature? = null,
)

/** Every laid trap in the world, keyed by its tile. Runtime only: traps don't survive logout. */
class HunterTraps {
    private val byTile = HashMap<CoordGrid, Trap>()

    operator fun get(coords: CoordGrid): Trap? = byTile[coords]

    fun add(trap: Trap) {
        byTile[trap.loc.coords] = trap
    }

    fun remove(trap: Trap) {
        byTile.remove(trap.loc.coords, trap)
    }

    fun ownedBy(player: Player): List<Trap> = byTile.values.filter { it.owner === player }
}
