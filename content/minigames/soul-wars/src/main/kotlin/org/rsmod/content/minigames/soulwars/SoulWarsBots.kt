package org.rsmod.content.minigames.soulwars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class SoulWarsBots
@Inject
constructor(
    private val npcs: NpcRepository,
    private val interactions: AiPlayerInteractions,
    private val random: GameRandom,
) {
    private val bots = LinkedHashMap<Npc, SwTeam>()
    private val respawnAt = ArrayList<Pair<SwTeam, Int>>()

    fun isBot(npc: Npc): Boolean = npc in bots

    fun teamOf(npc: Npc): SwTeam? = bots[npc]

    fun spawn(counts: Map<SwTeam, Int>) {
        clear()
        for ((team, count) in counts) repeat(count) { add(team) }
    }

    fun clear() {
        for (npc in bots.keys) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        bots.clear()
        respawnAt.clear()
    }

    fun onKilled(npc: Npc, cycle: Int) {
        val team = bots.remove(npc) ?: return
        respawnAt += team to cycle + RESPAWN_TICKS
    }

    fun update(cycle: Int, humans: Map<Player, SwTeam>) {
        val due = respawnAt.filter { it.second <= cycle }
        if (due.isNotEmpty()) {
            respawnAt.removeAll(due.toSet())
            for ((team, _) in due) add(team)
        }
        if (cycle % ACT_INTERVAL != 0) return
        for ((npc, team) in bots.entries.toList()) {
            if (!npc.isSlotAssigned) continue
            val enemy =
                humans
                    .filter { it.value != team }
                    .keys
                    .filter { it.coords.chebyshevDistance(npc.coords) <= AGGRO_RANGE }
                    .minByOrNull { it.coords.chebyshevDistance(npc.coords) }
            when {
                enemy != null -> npc.opPlayer2(enemy, interactions)
                npc.coords.chebyshevDistance(ObeliskControl.CENTRE) > OBELISK_LEASH ->
                    npc.walk(ObeliskControl.CENTRE)
            }
        }
    }

    private fun add(team: SwTeam) {
        val type = ServerCacheManager.getNpc(SYMBOLS.getValue(team).asRSCM(RSCMType.NPC)) ?: return
        val spawn = SoulWarsMap.GRAVEYARD.getValue(team)
        val at = CoordGrid(spawn.x + random.of(-2, 2), spawn.z + random.of(-2, 2), spawn.level)
        val npc = Npc(type, at)
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        bots[npc] = team
    }

    companion object {
        val SYMBOLS = mapOf(SwTeam.Blue to "npc.white_knight", SwTeam.Red to "npc.aggressive_black_knight")
        const val RESPAWN_TICKS = 15
        private const val ACT_INTERVAL = 3
        private const val AGGRO_RANGE = 12
        private const val OBELISK_LEASH = 12
    }
}
