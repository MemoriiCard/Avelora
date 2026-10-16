package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CastleWarsFieldScript
@Inject
constructor(
    private val service: CastleWarsService,
    private val barricades: CastleWarsBarricades,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (table in CastleWarsSupplies.tables) {
            onOpLoc1(table.loc) { take(table.obj) }
        }
        onOpHeld1("obj.castlewars_barricade") { setUpBarricade() }
        for (team in CwTeam.entries) {
            val normal = npcType(CastleWarsBarricades.npcSymbol(team, burning = false))
            val burning = npcType(CastleWarsBarricades.npcSymbol(team, burning = true))
            onOpNpcU(normal, itemType("obj.tinderbox")) { light(it.npc, team) }
            onOpNpcU(normal, itemType("obj.castlewars_explosives_potion")) { explode(it.npc, team) }
            onOpNpcU(burning, itemType("obj.bucket_water")) { douse(it.npc, team) }
        }
    }

    private fun npcType(symbol: String) = checkNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))

    private fun itemType(symbol: String) = checkNotNull(ServerCacheManager.getItem(symbol.asRSCM(RSCMType.OBJ)))

    private fun ProtectedAccess.take(obj: String) {
        if (!service.isPlaying(player)) {
            mes("You can only take supplies during a game.")
            return
        }
        if (invAdd(inv, obj).failure) {
            mes("You don't have enough inventory space to do that.")
        }
    }

    private fun ProtectedAccess.setUpBarricade() {
        val team = service.teamOf(player)?.takeIf { service.isPlaying(player) }
        if (team == null) {
            mes("You can only set up barricades during a game.")
            return
        }
        if (!CastleWarsMap.inside(coords)) {
            return
        }
        if (barricades.teamCount(team) >= BarricadeLedger.TEAM_LIMIT) {
            mes("Your team already has the maximum number of barricades.")
            return
        }
        if (invDel(inv, "obj.castlewars_barricade").failure) {
            return
        }
        if (!barricades.place(team, coords)) {
            invAdd(inv, "obj.castlewars_barricade")
            mes("You can't set up a barricade here.")
        }
    }

    private fun ProtectedAccess.light(npc: Npc, owner: CwTeam) {
        val mine = enemyEntry(npc, owner) ?: return
        if (barricades.ignite(mine, mapClock)) {
            mes("You set fire to the barricade.")
        }
    }

    private fun ProtectedAccess.explode(npc: Npc, owner: CwTeam) {
        val entry = enemyEntry(npc, owner) ?: return
        if (invDel(inv, "obj.castlewars_explosives_potion").failure) {
            return
        }
        barricades.destroy(entry)
        mes("The explosion destroys the barricade.")
    }

    private fun ProtectedAccess.douse(npc: Npc, owner: CwTeam) {
        val team = service.teamOf(player)
        if (!service.isPlaying(player) || team != owner) {
            mes("You can only put out your own team's barricades.")
            return
        }
        val entry = barricades.entryOf(npc) ?: return
        if (invDel(inv, "obj.bucket_water").failure) {
            return
        }
        invAdd(inv, "obj.bucket_empty")
        if (barricades.extinguish(entry)) {
            mes("You put out the fire.")
        }
    }

    private fun ProtectedAccess.enemyEntry(npc: Npc, owner: CwTeam): BarricadeLedger.Entry? {
        val team = service.teamOf(player)
        if (!service.isPlaying(player) || team == null) {
            mes("That isn't yours to touch.")
            return null
        }
        if (team == owner) {
            mes("You can't do that to your own team's barricade.")
            return null
        }
        return barricades.entryOf(npc)
    }
}
