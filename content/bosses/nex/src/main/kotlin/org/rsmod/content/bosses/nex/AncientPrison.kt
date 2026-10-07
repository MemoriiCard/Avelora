package org.rsmod.content.bosses.nex

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statRestore
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AncientPrison
@Inject
constructor(private val playerList: PlayerList, private val fight: NexFight) : PluginScript() {
    override fun ScriptContext.startup() {
        for (door in FROZEN_DOOR_OUTER) onOpLoc1(door) { enterPrison() }
        for (door in FROZEN_DOOR_INNER) onOpLoc1(door) { leavePrison() }
        onOpLoc1(OUTER_PRISON_DOOR) { passDoor(OUTER_PRISON_DOOR_X, OUTER_PRISON_DOOR_Z) }
        onOpLoc1(INNER_PRISON_DOOR) { passDoor(INNER_PRISON_DOOR_X, INNER_PRISON_DOOR_Z) }
        onOpLoc1(BARRIER) { passBarrier() }
        onOpLoc3(BARRIER) { peekBarrier() }
        onOpLoc1(ZAROS_ALTAR) { prayAtAltar() }
        onOpLoc2(ZAROS_ALTAR) { teleportOut() }
        for (first in KEY_PIECES) {
            for (second in KEY_PIECES) {
                if (first < second) onOpHeldU(first, second) { assembleKey() }
            }
        }
    }

    private suspend fun ProtectedAccess.enterPrison() {
        if (invTotal(inv, FROZEN_KEY) == 0) {
            mes("The door is sealed shut by ice. You need a frozen key to open it.")
            return
        }
        arriveDelay()
        telejump(PRISON_ARRIVAL, TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.leavePrison() {
        arriveDelay()
        telejump(DUNGEON_ARRIVAL, TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.passDoor(doorX: Int, doorZ: Int) {
        val dest = if (player.coords.x < doorX) doorX + 1 else doorX - 1
        arriveDelay()
        telejump(CoordGrid(dest, doorZ, player.coords.level), TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.passBarrier() {
        arriveDelay()
        if (NexArena.contains(player.coords)) {
            telejump(NexArena.BARRIER_OUTSIDE, TeleportType.Exempt)
            return
        }
        if (fight.inProgress) mes("<col=ef1020>Nex is already fighting. You join the battle!")
        telejump(NexArena.BARRIER_INSIDE, TeleportType.Exempt)
    }

    private fun ProtectedAccess.peekBarrier() {
        val count = playerList.count { NexArena.contains(it.coords) }
        when (count) {
            0 -> mes("You peek through the barrier and see no one fighting Nex.")
            1 -> mes("You peek through the barrier and see 1 player fighting Nex.")
            else -> mes("You peek through the barrier and see $count players fighting Nex.")
        }
    }

    private fun ProtectedAccess.prayAtAltar() {
        if (fight.inProgress) {
            mes("The altar's power is drowned out while Nex is fighting.")
            return
        }
        anim("seq.human_pray")
        player.statRestore("stat.prayer")
        mes("You recharge your Prayer points at the altar.")
    }

    private suspend fun ProtectedAccess.teleportOut() {
        arriveDelay()
        telejump(NexArena.BANK_ROOM, TeleportType.Exempt)
        mes("The altar teleports you out of Nex's chamber.")
    }

    private fun ProtectedAccess.assembleKey() {
        if (KEY_PIECES.any { invTotal(inv, it) == 0 }) {
            mes("You need all four pieces to assemble the frozen key.")
            return
        }
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                for (piece in KEY_PIECES) {
                    delete {
                        this.from = from
                        this.obj = piece.asRSCM(RSCMType.OBJ)
                        this.strictCount = 1
                    }
                }
                insert {
                    this.into = from
                    this.obj = FROZEN_KEY.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                }
            }
        if (result.failure) return
        mes("You fit the four pieces together to make the frozen key.")
    }

    internal companion object {
        const val FROZEN_KEY = "obj.nex_frozen_key"
        val KEY_PIECES =
            listOf(
                "obj.nex_frozen_key_armadyl",
                "obj.nex_frozen_key_bandos",
                "obj.nex_frozen_key_saradomin",
                "obj.nex_frozen_key_zamorak",
            )

        val FROZEN_DOOR_OUTER = listOf("loc.nex_frozen_door_outer_1", "loc.nex_frozen_door_outer_2")
        val FROZEN_DOOR_INNER = listOf("loc.nex_frozen_door_inner_1", "loc.nex_frozen_door_inner_2")
        val PRISON_ARRIVAL = CoordGrid(2855, 5226, 0)
        val DUNGEON_ARRIVAL = CoordGrid(2884, 5280, 2)

        const val OUTER_PRISON_DOOR = "loc.nex_outer_prison_door"
        const val OUTER_PRISON_DOOR_X = 2862
        const val OUTER_PRISON_DOOR_Z = 5219
        const val INNER_PRISON_DOOR = "loc.nex_inner_prison_door"
        const val INNER_PRISON_DOOR_X = 2899
        const val INNER_PRISON_DOOR_Z = 5203

        const val BARRIER = "loc.nex_fight_barrier_outer"
        const val ZAROS_ALTAR = "loc.nex_zaros_altar"
    }
}
