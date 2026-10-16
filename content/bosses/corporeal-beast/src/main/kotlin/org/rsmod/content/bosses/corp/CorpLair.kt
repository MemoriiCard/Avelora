package org.rsmod.content.bosses.corp

import jakarta.inject.Inject
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CorpLair @Inject constructor(private val players: PlayerList) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CAVE_ENTRANCE) { enterCave() }
        onOpLoc1(CAVE_EXIT) { telejump(CorpLairMap.CAVE_OUTSIDE) }
        onOpLoc1(PASSAGE) { telejump(CorpLairMap.throughPassage(player.coords)) }
        onOpLoc2(PASSAGE) { peek() }
    }

    private fun ProtectedAccess.enterCave() {
        val ironmanRoom = player.isAnyIronman && player.combatLevel >= CorpLairMap.IRONMAN_COMBAT
        telejump(CorpLairMap.lobbyFor(ironmanRoom))
    }

    private fun ProtectedAccess.peek() {
        val ironmanRoom = player.coords.z >= CorpLairMap.LOBBY.z + CorpLairMap.IRONMAN_ROOM_OFFSET / 2
        val inside =
            players.count { other ->
                CorpLairMap.inRoom(other.coords) &&
                    (other.coords.z >= CorpLairMap.LOBBY.z + CorpLairMap.IRONMAN_ROOM_OFFSET / 2) == ironmanRoom
            }
        val noun = if (inside == 1) "adventurer" else "adventurers"
        mes("You peek inside and see $inside $noun in the cave.")
    }

    private companion object {
        const val CAVE_ENTRANCE = "loc.corp_cave_entrance"
        const val CAVE_EXIT = "loc.corp_cave_exit"
        const val PASSAGE = "loc.corp_beast_entrance"
    }
}
