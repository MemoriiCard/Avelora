package org.rsmod.content.raids.cox.raid

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.raids.cox.layout.CoxRoomCategory
import org.rsmod.content.raids.cox.party.CoxParties
import org.rsmod.content.raids.cox.party.CoxPartyScreens
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoxRaidScript
@Inject
constructor(private val parties: CoxParties, private val raids: CoxRaids) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.raids_entrance_steps") { enterChambers() }
        onOpLoc1("loc.raids_exit_steps") { climbExitSteps() }
        onOpLoc2("loc.raids_exit_steps_reload") { reloadRaid() }
        onOpLoc1("loc.raids_bank_chest_lobby_working") { lobbyBank() }

        onIfOverlayButton("component.raids_sidepanel:start") { startRaid() }
        onIfOverlayButton("component.raids_sidepanel:refresh") { refreshPanel() }

        onOpLoc1("loc.raids_doorway") { passDoorway(it.loc) }
        onOpLoc1("loc.raids_descentto2") { descend() }
        onOpLoc1("loc.raids_descentto1") { descend() }
        onOpLoc1("loc.raids_bossentrance") { enterOlm() }
        onOpLoc1("loc.raids_ascentto3") { climbUp() }
        onOpLoc1("loc.raids_ascentto2") { climbUp() }
        onOpLoc1("loc.raids_bossexit") { leaveOlm() }
        onOpLoc1("loc.raids_olm_barrier") { passOlmBarrier(it.loc) }
    }

    private fun ProtectedAccess.enterChambers() {
        val party = parties.of(player) ?: parties.create(player)
        val existing = raids.of(party)
        if (existing != null && existing.started) {
            mes("Your party has already started the raid.")
            return
        }
        val raid = existing ?: raids.create(party)
        if (raid == null) {
            mes("The Chambers are too crowded right now. Please try again shortly.")
            return
        }
        CoxPartyScreens.writeClientState(player, party)
        raids.enter(player, raid)
        if (party.isLeader(player) && party.size == 1) {
            mes("You are the leader of this raid. Press 'Start raid' in the side panel when ready.")
        }
    }

    private fun ProtectedAccess.climbExitSteps() {
        val raid = raids.containing(player) ?: return
        if (raid.inOlmRoom(player.coords) || !raid.started) {
            raids.exit(player, raid)
            return
        }
        if (raid.roomAt(player.coords)?.type?.category == CoxRoomCategory.Start) {
            raids.exit(player, raid, "You leave the Chambers of Xeric.")
        }
    }

    private suspend fun ProtectedAccess.reloadRaid() {
        val raid = raids.containing(player) ?: return
        val party = raid.party
        if (!party.isLeader(player) || raid.started) {
            mes("Only your party leader can reload the raid, and only before it begins.")
            return
        }
        val choice = menu("Reload the raid? Everyone else will be removed.", "Yes", "No")
        if (choice != 0) return
        if (raids.reload(raid) == null) {
            mes("The Chambers are too crowded right now. Please try again shortly.")
        }
    }

    private fun ProtectedAccess.lobbyBank() {
        val raid = raids.containing(player) ?: return
        if (raid.started) {
            mes("The bank chest has closed now that the raid has begun.")
            return
        }
        tryOpenBank()
    }

    private fun ProtectedAccess.startRaid() {
        val raid = raids.containing(player) ?: return
        if (!raid.party.isLeader(player) || raid.started) return
        raids.start(raid)
    }

    private fun ProtectedAccess.refreshPanel() {
        val raid = raids.containing(player) ?: return
        CoxPartyScreens.sendSidePanel(player, raid.party)
    }

    private fun ProtectedAccess.passDoorway(loc: BoundLocInfo) {
        val raid = raids.containing(player) ?: return
        if (!raid.started) {
            mes("Your party leader must start the raid before you can go any further.")
            return
        }
        val destination = acrossLoc(loc, player.coords)
        telejump(destination)
        raid.floorIndexAt(destination)?.let { raids.reachedFloor(raid, it) }
    }

    private fun ProtectedAccess.descend() {
        val raid = raids.containing(player) ?: return
        val floorIndex = raid.floorIndexAt(player.coords) ?: return
        val next = raid.layout.floors.getOrNull(floorIndex + 1) ?: return
        telejump(raids.floorStartArrival(raid, next.start))
        raids.reachedFloor(raid, next.index)
    }

    private fun ProtectedAccess.enterOlm() {
        val raid = raids.containing(player) ?: return
        telejump(raids.olmArrival(raid))
        raids.reachedFloor(raid, raid.layout.floors.size)
    }

    private fun ProtectedAccess.climbUp() {
        val raid = raids.containing(player) ?: return
        val floorIndex = raid.floorIndexAt(player.coords) ?: return
        val previous = raid.layout.floors.getOrNull(floorIndex - 1) ?: return
        telejump(raids.endRoomArrival(raid, previous.end))
    }

    private fun ProtectedAccess.leaveOlm() {
        val raid = raids.containing(player) ?: return
        telejump(raids.endRoomArrival(raid, raid.layout.floors.last().end))
    }

    private fun ProtectedAccess.passOlmBarrier(loc: BoundLocInfo) {
        telejump(acrossLoc(loc, player.coords))
    }

    /** The tile directly across a doorway-style loc from [from]. */
    private fun acrossLoc(loc: BoundLocInfo, from: CoordGrid): CoordGrid {
        val width = loc.adjustedWidth
        val length = loc.adjustedLength
        val sw = loc.coords
        return if (from.x in sw.x until sw.x + width) {
            val z = if (from.z < sw.z) sw.z + length else sw.z - 1
            CoordGrid(from.x, z, sw.level)
        } else {
            val x = if (from.x < sw.x) sw.x + width else sw.x - 1
            val z = from.z.coerceIn(sw.z, sw.z + length - 1)
            CoordGrid(x, z, sw.level)
        }
    }
}
