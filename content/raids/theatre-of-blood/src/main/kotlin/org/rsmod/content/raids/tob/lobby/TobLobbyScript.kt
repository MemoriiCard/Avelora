package org.rsmod.content.raids.tob.lobby

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobParties
import org.rsmod.content.raids.tob.party.TobParty
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TobLobbyScript
@Inject
constructor(private val parties: TobParties, private val raids: TobRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogout { leaveParty(player) }
        onOpLoc1("loc.tob_surface_notice_board") { openBoard() }
        onOpLoc1("loc.tob_surface_raid_entrance") { enterTheatre() }
    }

    private suspend fun ProtectedAccess.openBoard() {
        val party = parties.of(player)
        if (party == null) {
            when (menu("Theatre of Blood", "Form a party", "Join a party", "Cancel")) {
                0 -> formParty()
                1 -> joinParty()
            }
            return
        }
        if (party.isLeader(player)) leaderMenu(party) else memberMenu(party)
    }

    private fun ProtectedAccess.formParty() {
        val party = parties.create(player)
        mes("You form a party. Choose its settings from the notice board, then enter the Theatre.")
        describe(party)
    }

    private suspend fun ProtectedAccess.joinParty() {
        val open = parties.open().take(MENU_PARTIES)
        if (open.isEmpty()) {
            mes("No parties are recruiting right now.")
            return
        }
        val labels = open.map { "${it.leader.displayName} (${it.size}/${TobScaling.MAX_PARTY}, ${it.mode.label})" }
        val choice = menu("Join which party?", *labels.toTypedArray(), "Cancel")
        val party = open.getOrNull(choice) ?: return
        when {
            player.combatLevel < party.minCombat ->
                mes("You need a combat level of at least ${party.minCombat} to join this party.")
            else -> report(party, parties.join(party, player))
        }
    }

    private fun ProtectedAccess.report(party: TobParty, result: TobParties.JoinResult) {
        when (result) {
            TobParties.JoinResult.Joined -> {
                mes("You join ${party.name}.")
                tell(party, "${player.displayName} has joined the party.")
            }
            TobParties.JoinResult.AlreadyInParty -> mes("You must leave your current party first.")
            TobParties.JoinResult.AlreadyStarted -> mes("That party is already inside the Theatre.")
            TobParties.JoinResult.Full -> mes("That party is full.")
        }
    }

    private suspend fun ProtectedAccess.leaderMenu(party: TobParty) {
        when (menu("Your party", "Party settings", "View party", "Disband party", "Cancel")) {
            0 -> settings(party)
            1 -> describe(party)
            2 -> disband(party)
        }
    }

    private suspend fun ProtectedAccess.memberMenu(party: TobParty) {
        when (menu("${party.name}", "View party", "Leave party", "Cancel")) {
            0 -> describe(party)
            1 -> {
                leaveParty(player)
                mes("You leave the party.")
            }
        }
    }

    private suspend fun ProtectedAccess.settings(party: TobParty) {
        if (party.started) {
            mes("You can't change the settings once the party is inside the Theatre.")
            return
        }
        val choice =
            menu(
                "Party settings",
                "Mode: ${party.mode.label}",
                "Minimum combat level: ${party.minCombat}",
                "Back",
            )
        when (choice) {
            0 -> chooseMode(party)
            1 -> {
                party.minCombat = countDialog("Minimum combat level (0-126):").coerceIn(0, 126)
                tell(party, "Your leader set the minimum combat level to ${party.minCombat}.")
            }
        }
        if (choice in 0..1) settings(party)
    }

    private suspend fun ProtectedAccess.chooseMode(party: TobParty) {
        val modes = TobMode.entries
        val choice = menu("Choose the difficulty", *modes.map { it.label }.toTypedArray())
        val mode = modes.getOrNull(choice) ?: return
        party.mode = mode
        tell(party, "Your leader set the mode to ${mode.label}.")
    }

    private fun ProtectedAccess.disband(party: TobParty) {
        raids.dissolve(party)
        for (member in parties.disband(party)) {
            if (member !== player) member.mes("Your party has been disbanded.")
        }
        mes("You disband your party.")
    }

    private fun ProtectedAccess.describe(party: TobParty) {
        mes("${party.name}: ${party.mode.label}, ${party.size}/${TobScaling.MAX_PARTY} players.")
        mes("Members: ${party.members.joinToString { it.displayName }}")
        if (party.minCombat > 0) mes("Minimum combat level: ${party.minCombat}.")
        mes("Boss health: ${TobScaling.teamHpPercent(party.size)}% for this team size.")
    }

    private fun ProtectedAccess.enterTheatre() {
        val party = parties.of(player) ?: parties.create(player)
        val raid = raids.of(party)
        if (raid != null) {
            if (raid.engaged || raid.room != TobRoom.entries.first()) {
                mes("Your party has already moved too far into the Theatre to follow.")
                return
            }
            raids.enter(player, raid)
            return
        }
        if (!party.isLeader(player)) {
            mes("Wait for your party leader to enter the Theatre first.")
            return
        }
        val present = party.snapshot().filter { inLobby(it.coords) }
        if (raids.start(party, present) == null) {
            mes("The Theatre is too crowded right now. Please try again shortly.")
        }
    }

    private fun tell(party: TobParty, message: String) {
        for (member in party.members) member.mes(message)
    }

    private fun leaveParty(player: Player) {
        val party = parties.leave(player) ?: return
        for (member in party.members) member.mes("${player.displayName} has left the party.")
    }

    private fun inLobby(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in LOBBY_X && coords.z in LOBBY_Z

    private companion object {
        const val MENU_PARTIES = 4
        val LOBBY_X = 3640..3690
        val LOBBY_Z = 3195..3235
    }
}
