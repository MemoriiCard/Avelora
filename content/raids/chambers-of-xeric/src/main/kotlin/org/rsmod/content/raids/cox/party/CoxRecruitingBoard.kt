package org.rsmod.content.raids.cox.party

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfPauseButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.raids.cox.party.CoxPartyScreens.Role
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoxRecruitingBoard
@Inject
constructor(private val parties: CoxParties, private val raids: CoxRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { player.raidsPartyHolder = -1 }
        onPlayerLogout { leaveParty(player) }

        onOpLoc1("loc.raids_party_recruitment") { openPartyList() }

        onIfPauseButton("component.raids_lobby_partylist:contents") {
            when (it.comsub) {
                LIST_REFRESH -> openPartyList()
                LIST_MY_PARTY -> openOwnParty()
            }
        }
        onIfPauseButton("component.raids_lobby_partylist:list") {
            val party = parties.advertised().getOrNull(it.comsub)
            if (party == null) {
                mes("That party is no longer advertised.")
                openPartyList()
                return@onIfPauseButton
            }
            openPartyDetails(party)
        }
        onIfPauseButton("component.raids_lobby_partydetails:contents") { detailsButton(it.comsub) }
    }

    private fun ProtectedAccess.openPartyList() {
        ifOpenMainModal("interface.raids_lobby_partylist")
        CoxPartyScreens.sendPartyList(player, parties.advertised(), mapClock)
    }

    private fun ProtectedAccess.openOwnParty() {
        val party = parties.of(player) ?: createParty() ?: return
        openPartyDetails(party)
    }

    private fun ProtectedAccess.createParty(): CoxParty? {
        val party = parties.create(player)
        CoxPartyScreens.writeClientState(player, party)
        mes("You have formed a raiding party.")
        return party
    }

    private fun ProtectedAccess.openPartyDetails(party: CoxParty) {
        player.coxViewingParty = party.id
        ifOpenMainModal("interface.raids_lobby_partydetails")
        CoxPartyScreens.sendPartyDetails(player, party, roleIn(party))
    }

    private fun ProtectedAccess.roleIn(party: CoxParty): Role =
        when {
            party.isLeader(player) -> Role.Leader
            player in party -> Role.Member
            else -> Role.Outsider
        }

    private suspend fun ProtectedAccess.detailsButton(button: Int) {
        val party = parties[player.coxViewingParty]
        if (party == null) {
            mes("That party no longer exists.")
            openPartyList()
            return
        }
        when (button) {
            DETAILS_JOIN -> joinOrLeave(party)
            DETAILS_ADVERTISE -> toggleAdvert(party)
            DETAILS_DISBAND -> disband(party)
            DETAILS_REFRESH -> openPartyDetails(party)
            DETAILS_BACK -> openPartyList()
            DETAILS_PREFERRED_SIZE ->
                setNumber(party, "Enter your preferred party size (0-100):", 0..100) {
                    preferredSize = it
                }
            DETAILS_PREFERRED_COMBAT ->
                setNumber(party, "Enter the minimum combat level (0-126):", 0..126) {
                    minCombat = it
                }
            DETAILS_PREFERRED_SKILL_TOTAL ->
                setNumber(party, "Enter the minimum skill total (0-2500):", 0..MAX_SKILL_TOTAL) {
                    minSkillTotal = it
                }
            DETAILS_SCALING ->
                setNumber(party, "Scale the raid as if the party has this many members (0-100):", 0..100) {
                    scaling = it
                }
            DETAILS_CHALLENGE -> toggleChallengeMode(party)
            DETAILS_MAP_POOL -> chooseMapPool(party)
        }
    }

    private fun ProtectedAccess.joinOrLeave(party: CoxParty) {
        if (player in party) {
            leaveParty(player)
            mes("You leave the raiding party.")
            openPartyList()
            return
        }
        val reason = joinRequirementFailure(party)
        if (reason != null) {
            mes(reason)
            return
        }
        when (parties.join(party, player)) {
            CoxParties.JoinResult.Joined -> {
                mes("You join ${party.name}.")
                refreshParty(party)
            }
            CoxParties.JoinResult.AlreadyInParty -> mes("You must leave your current party first.")
            CoxParties.JoinResult.AlreadyStarted -> mes("That party has already started its raid.")
            CoxParties.JoinResult.Full -> mes("That party is full.")
        }
        openPartyDetails(party)
    }

    private fun ProtectedAccess.joinRequirementFailure(party: CoxParty): String? =
        when {
            player.combatLevel < party.minCombat ->
                "You need a combat level of at least ${party.minCombat} to join this party."
            player.skillTotal() < party.minSkillTotal ->
                "You need a skill total of at least ${party.minSkillTotal} to join this party."
            party.preferredSize > 0 && party.size >= party.preferredSize ->
                "That party already has as many members as its leader wants."
            else -> null
        }

    private fun ProtectedAccess.toggleAdvert(party: CoxParty) {
        if (!party.isLeader(player)) return
        if (party.isAdvertised) {
            party.advertisedAt = -1
            mes("Your party is no longer advertised.")
        } else if (party.inRaid) {
            mes("Your party has already started its raid.")
        } else {
            party.advertisedAt = mapClock
            mes("Your party is now advertised on the recruiting board.")
        }
        openPartyDetails(party)
    }

    private fun ProtectedAccess.disband(party: CoxParty) {
        if (!party.isLeader(player)) return
        if (party.inRaid) {
            mes("You can't disband your party once the raid has started.")
            return
        }
        raids.dissolve(party)
        for (member in parties.disband(party)) {
            CoxPartyScreens.clearClientState(member)
            if (member !== player) {
                member.mes("Your raiding party has been disbanded.")
            }
        }
        mes("You disband your raiding party.")
        openPartyList()
    }

    private suspend fun ProtectedAccess.setNumber(
        party: CoxParty,
        prompt: String,
        range: IntRange,
        apply: CoxParty.(Int) -> Unit,
    ) {
        if (!party.isLeader(player) || party.inRaid) return
        val value = countDialog(prompt).coerceIn(range)
        party.apply(value)
        refreshParty(party)
        openPartyDetails(party)
    }

    private fun ProtectedAccess.toggleChallengeMode(party: CoxParty) {
        if (!party.isLeader(player) || party.inRaid) return
        if (!party.canToggleChallengeMode()) {
            mes("Challenge Mode is only available with the full map layout.")
            return
        }
        party.challengeMode = !party.challengeMode
        val state = if (party.challengeMode) "enabled" else "disabled"
        refreshParty(party, "Your leader has $state Challenge Mode.")
        openPartyDetails(party)
    }

    private suspend fun ProtectedAccess.chooseMapPool(party: CoxParty) {
        if (!party.isLeader(player) || party.inRaid) return
        val pools = CoxMapPool.entries
        val choice = menu("Choose the map layout", *pools.map { it.label }.toTypedArray())
        val pool = pools.getOrNull(choice) ?: return
        party.mapPool = pool
        if (pool != CoxMapPool.Full) {
            party.challengeMode = false
        }
        refreshParty(party)
        openPartyDetails(party)
    }

    private fun refreshParty(party: CoxParty, message: String? = null) {
        for (member in party.members) {
            CoxPartyScreens.writeClientState(member, party)
            if (message != null && !party.isLeader(member)) {
                member.mes(message)
            }
        }
    }

    private fun leaveParty(player: Player) {
        val party = parties.leave(player) ?: return
        CoxPartyScreens.clearClientState(player)
        if (party.members.isNotEmpty()) {
            refreshParty(party, "${player.displayName} has left the party.")
        }
    }

    private companion object {
        const val LIST_REFRESH = 0
        const val LIST_MY_PARTY = 1

        const val DETAILS_JOIN = 0
        const val DETAILS_ADVERTISE = 1
        const val DETAILS_DISBAND = 2
        const val DETAILS_REFRESH = 3
        const val DETAILS_BACK = 4
        const val DETAILS_PREFERRED_SIZE = 5
        const val DETAILS_PREFERRED_COMBAT = 6
        const val DETAILS_PREFERRED_SKILL_TOTAL = 7
        const val DETAILS_SCALING = 8
        const val DETAILS_CHALLENGE = 9
        const val DETAILS_MAP_POOL = 10

        const val MAX_SKILL_TOTAL = 2500
    }
}
