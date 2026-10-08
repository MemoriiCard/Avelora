package org.rsmod.content.raids.toa.lobby

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.raids.toa.invocation.ToaCategory
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.invocation.ToaInvocations
import org.rsmod.content.raids.toa.party.ToaParties
import org.rsmod.content.raids.toa.party.ToaParty
import org.rsmod.content.raids.toa.party.ToaScaling
import org.rsmod.content.raids.toa.raid.ToaRaids
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaLobbyScript
@Inject
constructor(private val parties: ToaParties, private val raids: ToaRaids) : PluginScript() {
    private val presets = mutableMapOf<String, Array<Set<ToaInvocation>?>>()

    override fun ScriptContext.startup() {
        onPlayerLogout { leaveParty(player) }
        onOpLoc1("loc.toa_invocation_board") { openBoard() }
        onOpLoc1("loc.toa_lobby_raid_entry") { enterTombs() }
        onOpLoc1("loc.toa_lobby_exit") { leaveLobby() }
    }

    private suspend fun ProtectedAccess.openBoard() {
        val party = parties.of(player)
        if (party == null) {
            when (menu("Tombs of Amascut", "Form a party", "Join a party", "Cancel")) {
                0 -> formParty()
                1 -> joinParty()
            }
            return
        }
        if (party.isLeader(player)) leaderMenu(party) else memberMenu(party)
    }

    private fun ProtectedAccess.formParty() {
        val party = parties.create(player)
        mes("You form a party. Choose invocations from the board, then enter the tombs.")
        describe(party)
    }

    private suspend fun ProtectedAccess.joinParty() {
        val open = parties.open().take(MENU_PARTIES)
        if (open.isEmpty()) {
            mes("No parties are recruiting right now.")
            return
        }
        val labels =
            open.map {
                "${it.leader.displayName} (${it.size}/${ToaScaling.MAX_PARTY}, level ${it.raidLevel})"
            }
        val choice = menu("Join which party?", *labels.toTypedArray(), "Cancel")
        val party = open.getOrNull(choice) ?: return
        if (player.combatLevel < party.minCombat) {
            mes("You need a combat level of at least ${party.minCombat} to join this party.")
            return
        }
        report(party, parties.join(party, player))
    }

    private fun ProtectedAccess.report(party: ToaParty, result: ToaParties.JoinResult) {
        when (result) {
            ToaParties.JoinResult.Joined -> {
                mes("You join ${party.name}.")
                tell(party, "${player.displayName} has joined the party.")
            }
            ToaParties.JoinResult.AlreadyInParty -> mes("You must leave your current party first.")
            ToaParties.JoinResult.AlreadyStarted -> mes("That party is already inside the tombs.")
            ToaParties.JoinResult.Full -> mes("That party is full.")
        }
    }

    private suspend fun ProtectedAccess.leaderMenu(party: ToaParty) {
        when (
            menu(
                "Your party",
                "Invocations",
                "Presets",
                "Minimum combat level: ${party.minCombat}",
                "View party",
                "Disband party",
            )
        ) {
            0 -> invocations(party)
            1 -> presetMenu(party)
            2 -> {
                party.minCombat = countDialog("Minimum combat level (0-126):").coerceIn(0, 126)
                tell(party, "Your leader set the minimum combat level to ${party.minCombat}.")
            }
            3 -> describe(party)
            4 -> disband(party)
        }
    }

    private suspend fun ProtectedAccess.memberMenu(party: ToaParty) {
        when (menu(party.name, "View party", "Leave party", "Cancel")) {
            0 -> describe(party)
            1 -> {
                leaveParty(player)
                mes("You leave the party.")
            }
        }
    }

    private suspend fun ProtectedAccess.invocations(party: ToaParty) {
        if (party.started) {
            mes("You can't change invocations once the party is inside the tombs.")
            return
        }
        val categories = ToaCategory.entries
        val labels =
            categories.map { category ->
                val on = ToaInvocation.of(category).count { it in party.invocations }
                "${category.label} ($on active)"
            }
        val choice =
            menu("Raid level ${party.raidLevel} - ${party.mode.label}", *labels.toTypedArray(), "Done")
        val category = categories.getOrNull(choice) ?: return
        category(party, category)
        invocations(party)
    }

    private suspend fun ProtectedAccess.category(party: ToaParty, category: ToaCategory) {
        val options = ToaInvocation.of(category)
        val labels =
            options.map { invocation ->
                val state = if (invocation in party.invocations) "ON" else "off"
                val locked = if (ToaInvocations.canEnable(party.invocations, invocation)) "" else " (locked)"
                "$state  ${invocation.label} +${invocation.level}$locked"
            }
        val choice = menu(category.label, *labels.toTypedArray(), "Back")
        val invocation = options.getOrNull(choice) ?: return
        val next = ToaInvocations.toggle(party.invocations, invocation)
        if (next == party.invocations) {
            mes("${invocation.label} needs ${invocation.requires?.label} to be active first.")
            return
        }
        party.invocations = next
        tell(party, "Raid level is now ${party.raidLevel} (${party.mode.label}).")
        category(party, category)
    }

    private suspend fun ProtectedAccess.presetMenu(party: ToaParty) {
        val slots = presets.getOrPut(player.displayName) { arrayOfNulls(PRESET_SLOTS) }
        val labels =
            (0 until PRESET_SLOTS).flatMap { slot ->
                val saved = slots[slot]
                listOf(
                    "Save to preset ${slot + 1}",
                    if (saved == null) "Load preset ${slot + 1} (empty)"
                    else "Load preset ${slot + 1} (level ${ToaInvocations.raidLevel(saved)})",
                )
            }
        val choice = menu("Invocation presets", *labels.toTypedArray(), "Back")
        if (choice !in labels.indices) return
        val slot = choice / 2
        if (choice % 2 == 0) {
            slots[slot] = party.invocations
            mes("Saved your invocations to preset ${slot + 1}.")
        } else {
            val saved = slots[slot]
            if (saved == null) {
                mes("Preset ${slot + 1} is empty.")
            } else if (party.started) {
                mes("You can't change invocations once the party is inside the tombs.")
            } else {
                party.invocations = saved
                tell(party, "Loaded preset ${slot + 1}: raid level ${party.raidLevel} (${party.mode.label}).")
            }
        }
        presetMenu(party)
    }

    private fun ProtectedAccess.disband(party: ToaParty) {
        raids.dissolve(party)
        for (member in parties.disband(party)) {
            if (member !== player) member.mes("Your party has been disbanded.")
        }
        mes("You disband your party.")
    }

    private fun ProtectedAccess.describe(party: ToaParty) {
        mes("${party.name}: raid level ${party.raidLevel} (${party.mode.label}), ${party.size}/${ToaScaling.MAX_PARTY} players.")
        mes("Members: ${party.members.joinToString { it.displayName }}")
        if (party.minCombat > 0) mes("Minimum combat level: ${party.minCombat}.")
        val active = party.invocations.joinToString { it.label }
        mes("Invocations: ${active.ifEmpty { "none" }}")
        mes(
            "Boss health: ${ToaScaling.teamHpPercent(party.size)}% for this team size, " +
                "+${ToaScaling.levelBonusPercent(party.raidLevel)}% from raid level."
        )
    }

    private fun ProtectedAccess.enterTombs() {
        val party = parties.of(player) ?: parties.create(player)
        val raid = raids.of(party)
        if (raid != null) {
            raids.enter(player, raid)
            return
        }
        if (!party.isLeader(player)) {
            mes("Wait for your party leader to enter the tombs first.")
            return
        }
        val present = party.snapshot().filter { inLobby(it.coords) }
        if (raids.start(party, present) == null) {
            mes("The tombs are too crowded right now. Please try again shortly.")
        }
    }

    private fun ProtectedAccess.leaveLobby() {
        leaveParty(player)
        telejump(ToaRaids.LOBBY_EXIT)
    }

    private fun tell(party: ToaParty, message: String) {
        for (member in party.members) member.mes(message)
    }

    private fun leaveParty(player: Player) {
        val party = parties.leave(player) ?: return
        for (member in party.members) member.mes("${player.displayName} has left the party.")
    }

    private fun inLobby(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in LOBBY_X && coords.z in LOBBY_Z

    private companion object {
        const val MENU_PARTIES = 8
        const val PRESET_SLOTS = 3
        val LOBBY_X = 3328..3391
        val LOBBY_Z = 9088..9151
    }
}
