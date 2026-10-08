package org.rsmod.content.raids.toa.puzzle

import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class ScabarasPuzzleRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.ScabarasPuzzle, services, onCleared) {
    private var wave = 0
    private val scarabs = mutableListOf<Npc>()

    override fun begin() {
        sendWave()
    }

    override fun tick() {
        for (scarab in scarabs.toList()) chase(scarab, HIT, RATE)
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        scarabs.removeAll { it === npc }
        if (scarabs.isNotEmpty()) return
        if (wave >= WAVES) {
            tell("The scarabs fall silent. The way east is open.")
            finish()
        } else {
            sendWave()
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        scarabs.removeAll { it === npc }
    }

    private fun sendWave() {
        wave++
        val count = ScabarasPuzzleRules.waveSize(wave, teamSize)
        repeat(count) {
            val hole = HOLES[it % HOLES.size]
            scarabs += spawn("npc.toa_scabaras_scarab", hole, scaledHp(ScabarasPuzzleRules.SCARAB_HP), STAT)
        }
        tell("Wave $wave of $WAVES: the scarabs pour out of the holes.")
    }

    private companion object {
        const val WAVES = ScabarasPuzzleRules.WAVES
        const val HIT = ScabarasPuzzleRules.HIT
        const val RATE = ScabarasPuzzleRules.RATE
        const val STAT = 30
        val HOLES = listOf(CoordGrid(3548, 5277, 0), CoordGrid(3548, 5283, 0))
    }
}
