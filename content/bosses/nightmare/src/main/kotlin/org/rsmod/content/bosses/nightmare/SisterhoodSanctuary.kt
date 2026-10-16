package org.rsmod.content.bosses.nightmare

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.MapClock
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SisterhoodSanctuary
@Inject
constructor(
    private val fight: NightmareFight,
    private val playerList: PlayerList,
    private val clock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (form in ENTRY_FORMS) {
            onOpNpc1(form) { disturb() }
            onOpNpc3(form) { inspect() }
        }
        for (barrier in ESCAPE_BARRIERS) onOpLoc1(barrier) { escape() }
    }

    private suspend fun ProtectedAccess.disturb() {
        when (fight.state) {
            NightmareFight.State.Idle -> openLobby()
            NightmareFight.State.Lobby -> Unit
            else -> {
                mes("The Nightmare is already awake. You'll have to wait for the next fight.")
                return
            }
        }
        val seconds = (fight.lobbyEndsAt - clock.cycle).coerceAtLeast(0) * 3 / 5
        mes("<col=ef1020>You disturb the Nightmare. She will awaken in about $seconds seconds.")
        telejump(NightmareArena.ARRIVALS.random(), TeleportType.Exempt)
    }

    private fun openLobby() {
        fight.state = NightmareFight.State.Lobby
        fight.lobbyEndsAt = clock.cycle + LOBBY_TICKS
        val open = ServerCacheManager.getNpc(ENTRY_OPEN.asRSCM(RSCMType.NPC)) ?: return
        fight.entry?.transmog(open, Int.MAX_VALUE)
    }

    private fun ProtectedAccess.inspect() {
        val count = playerList.count { NightmareArena.contains(it.coords) }
        val status =
            when (fight.state) {
                NightmareFight.State.Idle -> "The Nightmare is sleeping."
                NightmareFight.State.Lobby -> "The Nightmare is stirring."
                else -> "The Nightmare is awake."
            }
        val people = if (count == 1) "1 player is" else "$count players are"
        mes("$status $people in her dream.")
    }

    private suspend fun ProtectedAccess.escape() {
        arriveDelay()
        telejump(NightmareArena.LOBBY, TeleportType.Exempt)
    }

    internal companion object {
        const val LOBBY_TICKS = 17
        const val ENTRY_OPEN = "npc.nightmare_entry_open"
        val ENTRY_FORMS =
            listOf(
                "npc.nightmare_entry_ready",
                ENTRY_OPEN,
                "npc.nightmare_entry_closed_01",
                "npc.nightmare_entry_closed_02",
                "npc.nightmare_entry_closed_03",
            )
        val ESCAPE_BARRIERS = listOf("loc.nightmare_barrier_escape_initial", "loc.nightmare_barrier_escape_fight")
    }
}
