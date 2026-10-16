package org.rsmod.content.travel.fairyrings

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.table.FairyringRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.dialOne by intVarBit("varbit.fairyring_1")
private var Player.dialTwo by intVarBit("varbit.fairyring_2")
private var Player.dialThree by intVarBit("varbit.fairyring_3")
private var Player.lastDestination by intVarBit("varbit.fairyring_lastloc")
private var Player.lastDestinationVisible by intVarBit("varbit.fairyring_lastloc_opvis")
private var Player.lastDestinationSet by boolVarBit("varbit.fairyring_lastloc_set")

class FairyRingScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (ring in RINGS) {
            onOpLoc2(ring) { configure() }
            onOpLoc3(ring) { lastDestination() }
        }
        onOpLoc1("loc.fairyring_minorhub") { teleport(ZANARIS_HUB) }
        for ((button, dial, step) in DIAL_BUTTONS) {
            onIfModalButton("component.fairyrings:$button") { turn(dial, step) }
        }
        onIfModalButton("component.fairyrings:confirm") { confirm() }
    }

    private fun ProtectedAccess.configure() {
        if (!canUse()) return
        ifOpenMainModal(INTERFACE)
    }

    private fun ProtectedAccess.canUse(): Boolean {
        if (STAVES.any { player.worn.count(it) > 0 }) return true
        mes("The fairy ring only works for those who wield fairy magic.")
        return false
    }

    private fun ProtectedAccess.turn(dial: Int, step: Int) {
        when (dial) {
            1 -> player.dialOne = (player.dialOne + step) and 3
            2 -> player.dialTwo = (player.dialTwo + step) and 3
            else -> player.dialThree = (player.dialThree + step) and 3
        }
    }

    private fun ProtectedAccess.confirm() {
        if (!canUse()) return
        val code = FairyRingCode.of(player.dialOne, player.dialTwo, player.dialThree)
        val row = FairyringRow.all().firstOrNull { it.code.replace(" ", "").equals(code, ignoreCase = true) }
        if (row == null) {
            mes("The fairy ring does not respond to that combination.")
            return
        }
        val value = LAST_DESTINATION_OFFSET + FairyringRow.all().indexOf(row)
        player.lastDestination = value
        player.lastDestinationVisible = value
        player.lastDestinationSet = true
        teleport(row.destCoord)
    }

    private fun ProtectedAccess.lastDestination() {
        if (!canUse()) return
        val row = FairyringRow.all().getOrNull(player.lastDestination - LAST_DESTINATION_OFFSET)
        if (row == null) {
            mes("You have not used a fairy ring yet.")
            return
        }
        teleport(row.destCoord)
    }

    private fun ProtectedAccess.teleport(dest: CoordGrid) {
        if (!canUse()) return
        ifClose()
        telejump(dest)
    }

    private companion object {
        const val INTERFACE = "interface.fairyrings"
        const val LAST_DESTINATION_OFFSET = 64

        val STAVES = listOf("obj.dramen_staff", "obj.lunar_moonclan_liminal_staff")
        val ZANARIS_HUB = CoordGrid(2412, 4434, 0)
        val RINGS = listOf("loc.fairyring_minorhub", "loc.fairyring_homehub")

        val DIAL_BUTTONS =
            listOf(
                Triple("1_clockwise", 1, 1),
                Triple("1_anticlockwise", 1, 3),
                Triple("2_clockwise", 2, 1),
                Triple("2_anticlockwise", 2, 3),
                Triple("3_clockwise", 3, 1),
                Triple("3_anticlockwise", 3, 3),
            )
    }
}
