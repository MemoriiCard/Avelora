package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal class BarricadeLedger(private val perTeamLimit: Int = TEAM_LIMIT) {
    class Entry(val team: CwTeam, val coords: CoordGrid, var burnsOutAt: Int? = null)

    private val entries = ArrayList<Entry>()

    fun count(team: CwTeam): Int = entries.count { it.team == team }

    fun canPlace(team: CwTeam, coords: CoordGrid): Boolean =
        count(team) < perTeamLimit && entries.none { it.coords == coords }

    fun place(team: CwTeam, coords: CoordGrid): Entry? =
        if (canPlace(team, coords)) Entry(team, coords).also(entries::add) else null

    fun ignite(entry: Entry, cycle: Int, burnTicks: Int = BURN_TICKS): Boolean {
        if (entry.burnsOutAt != null) return false
        entry.burnsOutAt = cycle + burnTicks
        return true
    }

    fun extinguish(entry: Entry): Boolean {
        if (entry.burnsOutAt == null) return false
        entry.burnsOutAt = null
        return true
    }

    fun remove(entry: Entry) {
        entries.remove(entry)
    }

    fun burntOut(cycle: Int): List<Entry> = entries.filter { (it.burnsOutAt ?: Int.MAX_VALUE) <= cycle }

    fun clear() = entries.clear()

    companion object {
        const val TEAM_LIMIT = 10
        const val BURN_TICKS = 25
    }
}

@Singleton
class CastleWarsBarricades @Inject constructor(private val npcs: NpcRepository) {
    private val ledger = BarricadeLedger()
    private val npcOf = HashMap<BarricadeLedger.Entry, Npc>()

    internal fun entryOf(npc: Npc): BarricadeLedger.Entry? = npcOf.entries.firstOrNull { it.value === npc }?.key

    internal fun place(team: CwTeam, coords: CoordGrid): Boolean {
        val entry = ledger.place(team, coords) ?: return false
        spawn(entry, burning = false)
        return true
    }

    internal fun ignite(entry: BarricadeLedger.Entry, cycle: Int): Boolean {
        if (!ledger.ignite(entry, cycle)) return false
        respawn(entry, burning = true)
        return true
    }

    internal fun extinguish(entry: BarricadeLedger.Entry): Boolean {
        if (!ledger.extinguish(entry)) return false
        respawn(entry, burning = false)
        return true
    }

    internal fun destroy(entry: BarricadeLedger.Entry) {
        despawn(entry)
        ledger.remove(entry)
    }

    internal fun teamCount(team: CwTeam): Int = ledger.count(team)

    fun update(cycle: Int) {
        for (entry in ledger.burntOut(cycle)) destroy(entry)
    }

    fun clear() {
        npcOf.keys.toList().forEach(::despawn)
        ledger.clear()
    }

    private fun respawn(entry: BarricadeLedger.Entry, burning: Boolean) {
        despawn(entry)
        spawn(entry, burning)
    }

    private fun spawn(entry: BarricadeLedger.Entry, burning: Boolean) {
        val type = ServerCacheManager.getNpc(npcSymbol(entry.team, burning).asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, entry.coords)
        npcs.add(npc, Int.MAX_VALUE)
        npcOf[entry] = npc
    }

    private fun despawn(entry: BarricadeLedger.Entry) {
        npcOf.remove(entry)?.takeIf { it.isSlotAssigned }?.let { npcs.del(it, Int.MAX_VALUE) }
    }

    companion object {
        fun npcSymbol(team: CwTeam, burning: Boolean): String =
            "npc.castlewars_barricade_" + (if (burning) "burning_" else "") + team.name.lowercase()
    }
}
