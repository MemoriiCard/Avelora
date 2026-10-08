package org.rsmod.content.raids.toa.puzzle

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class HetPuzzleRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.HetPuzzle, services, onCleared) {
    private val armed = mutableSetOf<Player>()
    private var hits = 0
    private var goal = spawn("npc.toa_het_goal", GOAL, 119)

    override fun begin() {
        tell("Take a pickaxe from a statue, then destroy the crystal. Beware its orbs.")
    }

    override fun tick() {
        if (clock % HetPuzzleRules.ORB_EVERY != 0) return
        val target = nearestPlayer(goal.coords) ?: return
        hurt(target, scaledDamage(services.random.of(1, HetPuzzleRules.ORB_MAX)))
    }

    fun takePickaxe(player: Player) {
        armed += player
        player.mes("You take a pickaxe from the statue.")
    }

    fun depositPickaxe(player: Player) {
        if (armed.remove(player)) player.mes("You return the pickaxe to the statue.")
    }

    fun destroy(player: Player) {
        if (player !in armed) {
            player.mes("You need a pickaxe from one of the statues.")
            return
        }
        armed -= player
        hits++
        player.mes("Your pickaxe cracks the crystal. (${hits}/${HetPuzzleRules.GOAL_HITS})")
        if (HetPuzzleRules.destroyed(hits)) {
            remove(goal)
            tell("The crystal shatters. The way west is open.")
            finish()
        }
    }

    private companion object {
        val GOAL = CoordGrid(3677, 5277, 0)
    }
}
