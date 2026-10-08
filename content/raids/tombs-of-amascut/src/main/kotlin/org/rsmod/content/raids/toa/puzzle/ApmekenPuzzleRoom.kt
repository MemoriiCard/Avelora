package org.rsmod.content.raids.toa.puzzle

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class ApmekenPuzzleRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.ApmekenPuzzle, services, onCleared) {
    private val repaired = mutableSetOf<Int>()
    private val hammers = mutableSetOf<Player>()

    override fun begin() {
        tell("Grab a hammer and repair the four pillars. Stay clear of the vents.")
    }

    override fun tick() {
        if (clock % ApmekenPuzzleRules.VENT_EVERY != 0) return
        val vents = VENTS.map { world(it) }
        for (player in playersInRoom()) {
            val near = vents.any { it.chebyshevDistance(player.coords) <= ApmekenPuzzleRules.VENT_REACH }
            if (near) hurt(player, scaledDamage(services.random.of(1, ApmekenPuzzleRules.VENT_MAX)))
        }
    }

    fun takeHammer(player: Player) {
        hammers += player
        player.mes("You take a hammer.")
    }

    fun repair(player: Player, pillar: CoordGrid) {
        val source = raid.source(pillar) ?: return
        val index = PILLARS.indexOf(source)
        if (index < 0 || index in repaired) {
            player.mes("This pillar is already repaired.")
            return
        }
        if (player !in hammers) {
            player.mes("You need a hammer first.")
            return
        }
        repaired += index
        player.mes("You repair the pillar. (${repaired.size}/${ApmekenPuzzleRules.PILLARS})")
        if (ApmekenPuzzleRules.complete(repaired)) {
            tell("All pillars stand again. The way east is open.")
            finish()
        }
    }

    private companion object {
        val PILLARS =
            listOf(
                CoordGrid(3803, 5289, 0),
                CoordGrid(3811, 5289, 0),
                CoordGrid(3803, 5269, 0),
                CoordGrid(3811, 5269, 0),
            )
        val VENTS =
            listOf(
                CoordGrid(3800, 5276, 0),
                CoordGrid(3800, 5284, 0),
                CoordGrid(3816, 5276, 0),
                CoordGrid(3816, 5284, 0),
            )
    }
}
