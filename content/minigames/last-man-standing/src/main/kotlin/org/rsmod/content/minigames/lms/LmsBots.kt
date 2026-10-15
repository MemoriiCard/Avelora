package org.rsmod.content.minigames.lms

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class LmsBots
@Inject
constructor(
    private val npcs: NpcRepository,
    private val interactions: AiPlayerInteractions,
) {
    private val bots = LinkedHashMap<Npc, Int>()

    fun isBot(npc: Npc): Boolean = npc in bots

    fun idOf(npc: Npc): Int? = bots[npc]

    fun spawn(ids: Collection<Int>, starts: List<CoordGrid>) {
        clear()
        val type = ServerCacheManager.getNpc(SYMBOL.asRSCM(RSCMType.NPC)) ?: return
        for ((index, id) in ids.withIndex()) {
            val base = starts[index % starts.size]
            val npc = Npc(type, CoordGrid(base.x + index % 3, base.z + index / 3 % 3, base.level))
            npcs.add(npc, Int.MAX_VALUE)
            npc.respawns = false
            bots[npc] = id
        }
    }

    fun clear() {
        for (npc in bots.keys) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        bots.clear()
    }

    fun remove(npc: Npc): Int? = bots.remove(npc)

    fun update(cycle: Int, humans: List<Player>, elapsed: Int) {
        if (cycle % ACT_INTERVAL != 0) return
        for (npc in bots.keys.toList()) {
            if (!npc.isSlotAssigned) continue
            val target =
                humans
                    .filter { it.coords.chebyshevDistance(npc.coords) <= AGGRO_RANGE }
                    .minByOrNull { it.coords.chebyshevDistance(npc.coords) }
            when {
                target != null -> npc.opPlayer2(target, interactions)
                LmsZone.isOutside(npc.coords.x, npc.coords.z, elapsed) ->
                    npc.walk(CoordGrid(LmsZone.CENTER_X, LmsZone.CENTER_Z, npc.coords.level))
            }
        }
    }

    companion object {
        const val SYMBOL = "npc.aggressive_black_knight"
        private const val ACT_INTERVAL = 3
        private const val AGGRO_RANGE = 18
    }
}
