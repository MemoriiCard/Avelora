package org.rsmod.content.minigames.soulwars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal enum class SoulKind(val symbol: String, val fragments: Int, val weight: Int) {
    WeakA("npc.soul_wars_soul_weak_1", 1, 3),
    WeakB("npc.soul_wars_soul_weak_2", 1, 3),
    StrongA("npc.soul_wars_soul_strong_1", 2, 2),
    StrongB("npc.soul_wars_soul_strong_2", 2, 2),
    Wolf("npc.soul_wars_wolf", 1, 2),
    ;

    companion object {
        val TOTAL_WEIGHT = entries.sumOf { it.weight }

        fun roll(value: Int): SoulKind {
            var left = value
            for (kind in entries) {
                left -= kind.weight
                if (left < 0) return kind
            }
            return entries.last()
        }
    }
}

@Singleton
class SoulWarsSouls
@Inject
constructor(
    private val npcs: NpcRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
) {
    private val souls = LinkedHashMap<Npc, SoulKind>()
    private val respawnAt = ArrayList<Int>()

    fun isSoul(npc: Npc): Boolean = npc in souls

    fun fragmentsFor(npc: Npc): Int = souls[npc]?.fragments ?: 0

    fun spawn() {
        clear()
        repeat(TARGET) { add() }
    }

    fun clear() {
        for (npc in souls.keys) if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        souls.clear()
        respawnAt.clear()
    }

    fun onKilled(npc: Npc, cycle: Int) {
        if (souls.remove(npc) != null) respawnAt += cycle + RESPAWN_TICKS
    }

    fun update(cycle: Int) {
        val due = respawnAt.filter { it <= cycle }
        if (due.isEmpty()) return
        respawnAt.removeAll(due.toSet())
        repeat(due.size) { add() }
    }

    private fun add() {
        val kind = SoulKind.roll(random.of(SoulKind.TOTAL_WEIGHT))
        val type = ServerCacheManager.getNpc(kind.symbol.asRSCM(RSCMType.NPC)) ?: return
        val at = pickTile() ?: return
        val npc = Npc(type, at)
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        souls[npc] = kind
    }

    private fun pickTile(): CoordGrid? {
        val centre = ObeliskControl.CENTRE
        repeat(ATTEMPTS) {
            val tile = CoordGrid(centre.x + random.of(-SPREAD, SPREAD), centre.z + random.of(-SPREAD_Z, SPREAD_Z), 0)
            if (collision[tile.x, tile.z, tile.level] and BLOCKED == 0) return tile
        }
        return null
    }

    private companion object {
        const val TARGET = 14
        const val RESPAWN_TICKS = 30
        const val SPREAD = 30
        const val SPREAD_Z = 14
        const val ATTEMPTS = 40
        const val BLOCKED =
            CollisionFlag.LOC or CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_PLAYERS
    }
}
