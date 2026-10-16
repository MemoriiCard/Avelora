package org.rsmod.content.raids.cox.party

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

private var Player.coxCompletions by intVarp("varp.total_completed_xericchambers")
private var Player.coxChallengeCompletions by intVarp("varp.total_completed_xericchambers_challenge")

internal object CoxPartyScreens {
    const val LIST_ROWS = 40
    const val DETAILS_CHILDREN_PER_ROW = 8
    const val SIDEPANEL_CHILDREN_PER_ROW = 7

    private val partyListAddLine = script("raids_partylist_addline")
    private val partyDetailsAddLine = script("raids_partydetails_addline")
    private val partyDetailsFinish = script("script1524")
    private val sidePanelInitLines = script("raids_sidepanel_initlines")
    private val sidePanelAddLine = script("raids_sidepanel_addline")

    fun sendPartyList(player: Player, parties: List<CoxParty>, clock: Int) {
        player.ifSetEvents("component.raids_lobby_partylist:contents", 0..1, IfEvent.PauseButton)
        player.ifSetEvents(
            "component.raids_lobby_partylist:list",
            0 until LIST_ROWS,
            IfEvent.PauseButton,
        )
        for (row in 0 until LIST_ROWS) {
            val party = parties.getOrNull(row)
            player.runClientScript(partyListAddLine, row, party?.let { listLine(it, clock) } ?: "")
        }
    }

    fun sendPartyDetails(player: Player, party: CoxParty, role: Role) {
        player.ifSetEvents("component.raids_lobby_partydetails:contents", 0..10, IfEvent.PauseButton)
        writeLobbySettings(player, party)
        for (member in party.members) {
            player.runClientScript(partyDetailsAddLine, 0, memberLine(member))
        }
        player.runClientScript(
            partyDetailsFinish,
            party.name,
            role.clientId,
            if (party.isAdvertised) 1 else 0,
            player.raidsDailyAdverts,
        )
    }

    fun sendSidePanel(player: Player, party: CoxParty) {
        writeClientState(player, party)
        player.runClientScript(sidePanelInitLines, party.size)
        party.members.forEachIndexed { index, member ->
            val line = "${member.displayName}|${member.combatLevel}|${member.skillTotal()}"
            player.runClientScript(sidePanelAddLine, index, line)
        }
    }

    fun writeLobbySettings(player: Player, party: CoxParty) {
        player.raidsLobbyPartySize = party.preferredSize
        player.raidsLobbyMinCombat = party.minCombat
        player.raidsLobbyMinSkillTotal = party.minSkillTotal
        player.raidsScaling = party.scaling
        player.raidsMapPool = party.mapPool.id
        player.raidsChallengeMode = party.challengeMode
    }

    fun writeClientState(player: Player, party: CoxParty) {
        writeLobbySettings(player, party)
        player.raidsPartyHolder = party.id
        player.raidsIsLeader = party.isLeader(player)
        player.raidsClientPartySize = party.size.coerceAtMost(MAX_CLIENT_SIZE)
        player.raidsClientScaledSize =
            CoxScaling.scaledPartySize(party).coerceAtMost(MAX_CLIENT_SIZE)
        player.raidsClientHighestCombat =
            party.members.maxOf { it.combatLevel }.coerceAtMost(MAX_CLIENT_COMBAT)
        player.raidsClientProgress = party.progress.clientId
    }

    fun clearClientState(player: Player) {
        player.raidsPartyHolder = -1
        player.raidsIsLeader = false
        player.raidsClientPartySize = 0
        player.raidsClientScaledSize = 0
        player.raidsClientHighestCombat = 0
        player.raidsClientProgress = 0
    }

    private fun listLine(party: CoxParty, clock: Int): String {
        val age = if (party.isAdvertised) (clock - party.advertisedAt).coerceAtLeast(0) else 0
        val challenge = if (party.challengeMode) 1 else 0
        return listOf(
                party.leader.displayName,
                party.size,
                party.preferredSize,
                party.minCombat,
                party.minSkillTotal,
                party.scaling,
                challenge,
                age,
            )
            .joinToString("|")
    }

    private fun memberLine(member: Player): String {
        val stats = STAT_ORDER.joinToString("|") { member.statBase(it).toString() }
        val kills = member.coxCompletions + member.coxChallengeCompletions
        return "${member.displayName}|${member.combatLevel}|${member.skillTotal()}|$kills|$stats"
    }

    private fun script(name: String): Int = "clientscript.[clientscript,$name]".asRSCM(RSCMType.CLIENTSCRIPT)

    enum class Role(val clientId: Int) {
        Outsider(0),
        Member(1),
        Leader(2),
    }

    private const val MAX_CLIENT_SIZE = 127
    private const val MAX_CLIENT_COMBAT = 127

    val STAT_ORDER =
        listOf(
            "stat.attack",
            "stat.strength",
            "stat.ranged",
            "stat.magic",
            "stat.defence",
            "stat.hitpoints",
            "stat.prayer",
            "stat.agility",
            "stat.herblore",
            "stat.thieving",
            "stat.crafting",
            "stat.runecrafting",
            "stat.mining",
            "stat.smithing",
            "stat.fishing",
            "stat.cooking",
            "stat.firemaking",
            "stat.woodcutting",
            "stat.fletching",
            "stat.slayer",
            "stat.farming",
            "stat.construction",
            "stat.hunter",
            "stat.sailing",
        )
}

internal fun Player.skillTotal(): Int = CoxPartyScreens.STAT_ORDER.sumOf { statBase(it) }
