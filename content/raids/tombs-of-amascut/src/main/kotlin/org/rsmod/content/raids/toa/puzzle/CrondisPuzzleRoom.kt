package org.rsmod.content.raids.toa.puzzle

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class CrondisPuzzleRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.CrondisPuzzle, services, onCleared) {
    private val trees = mutableListOf<Npc>()
    private val watered = mutableSetOf<Int>()
    private val carrying = mutableSetOf<Player>()
    private var croc: Npc? = null

    override fun begin() {
        TREE_SPOTS.forEachIndexed { index, tile ->
            trees += spawn(CrondisPuzzleRules.treeName(index), tile, 1)
        }
        croc = spawn("npc.toa_crondis_crocodile", CROC_START, scaledHp(CrondisPuzzleRules.CROC_HP), 60)
        tell("Fill a container at the pool, then water each palm. Mind the crocodile.")
    }

    override fun tick() {
        croc?.let { chase(it, CrondisPuzzleRules.BITE_MAX, CrondisPuzzleRules.BITE_RATE) }
    }

    fun fill(player: Player) {
        carrying += player
        player.mes("You fill the container with water.")
    }

    fun water(player: Player, tree: Npc) {
        val index = trees.indexOfFirst { it === tree }
        if (index < 0 || index in watered) {
            player.mes("This palm is already watered.")
            return
        }
        if (player !in carrying) {
            player.mes("You need to fill a container at the pool first.")
            return
        }
        carrying -= player
        watered += index
        player.mes("The palm drinks greedily. (${watered.size}/${CrondisPuzzleRules.TREES})")
        if (CrondisPuzzleRules.complete(watered)) {
            tell("Every palm is watered. The way west is open.")
            finish()
        }
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc === croc) croc = null
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === croc) croc = null
    }

    private companion object {
        val TREE_SPOTS =
            listOf(
                CoordGrid(3944, 5286, 0),
                CoordGrid(3936, 5286, 0),
                CoordGrid(3944, 5272, 0),
                CoordGrid(3936, 5272, 0),
            )
        val CROC_START = CoordGrid(3938, 5279, 0)
    }
}
