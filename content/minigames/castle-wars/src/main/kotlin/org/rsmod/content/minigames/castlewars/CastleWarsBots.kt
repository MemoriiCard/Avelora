package org.rsmod.content.minigames.castlewars

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
class CastleWarsBots
@Inject
constructor(
    private val npcs: NpcRepository,
    private val interactions: AiPlayerInteractions,
    private val random: GameRandom,
) {
    private val bots = LinkedHashMap<Npc, CwTeam>()
    private val respawnAt = ArrayList<Pair<CwTeam, Int>>()

    fun isBot(npc: Npc): Boolean = npc in bots

    fun teamOf(npc: Npc): CwTeam? = bots[npc]

    fun spawn(counts: Map<CwTeam, Int>) {
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

    fun update(cycle: Int, humans: Map<Player, CwTeam>) {
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
                    .filter { it.coords.level == npc.coords.level }
                    .filter { it.coords.chebyshevDistance(npc.coords) <= AGGRO_RANGE }
                    .minByOrNull { it.coords.chebyshevDistance(npc.coords) }
            if (enemy != null) {
                npc.opPlayer2(enemy, interactions)
            } else {
                val home = CastleWarsMap.SPAWN.getValue(team)
                if (npc.coords.chebyshevDistance(home) > LEASH) npc.walk(home)
            }
        }
    }

    private fun add(team: CwTeam) {
        val symbol = SYMBOLS.getValue(team)
        val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: return
        val spawn = CastleWarsMap.SPAWN.getValue(team)
        val at = CoordGrid(spawn.x + random.of(-2, 2), spawn.z + random.of(-2, 2), spawn.level)
        val npc = Npc(type, at)
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        bots[npc] = team
    }

    companion object {
        val SYMBOLS =
            mapOf(CwTeam.Saradomin to "npc.white_knight", CwTeam.Zamorak to "npc.aggressive_black_knight")
        const val RESPAWN_TICKS = 15
        private const val ACT_INTERVAL = 3
        private const val AGGRO_RANGE = 10
        private const val LEASH = 6
    }
}
