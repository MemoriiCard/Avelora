package org.rsmod.content.minigames.castlewars

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CastleWarsCatapultScript
@Inject
constructor(
    private val service: CastleWarsService,
    private val catapults: CastleWarsCatapults,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (team in CwTeam.entries) {
            val working = CatapultRules.symbol(team, CatapultState.Working)
            val burning = CatapultRules.symbol(team, CatapultState.Burning)
            val broken = CatapultRules.symbol(team, CatapultState.Broken)
            onOpLoc1(working) { operate(team) }
            onOpLoc1(broken) { repair(team) }
            onOpLocU(working, "obj.tinderbox") { ignite(team) }
            onOpLocU(burning, "obj.bucket_water") { douse(team) }
            onOpLocU(broken, "obj.castlewars_toolkit") { repair(team) }
        }
    }

    private suspend fun ProtectedAccess.operate(owner: CwTeam) {
        val team = playingTeam() ?: return
        if (team != owner) {
            mes("That isn't your team's catapult.")
            return
        }
        val wait = catapults.cooldownLeft(team, mapClock)
        if (wait > 0) {
            mes("The catapult is still reloading.")
            return
        }
        if (!inv.contains("obj.castlewars_catapult_rock")) {
            mes("You need a rock from the supply tables to load the catapult.")
            return
        }
        val x = countDialog("X coordinate (0-${CatapultRules.MAX_AXIS})").coerceIn(0, CatapultRules.MAX_AXIS)
        val z = countDialog("Y coordinate (0-${CatapultRules.MAX_AXIS})").coerceIn(0, CatapultRules.MAX_AXIS)
        if (!catapults.operate(team, mapClock) || invDel(inv, "obj.castlewars_catapult_rock").failure) {
            mes("The catapult isn't ready.")
            return
        }
        catapults.fire(team, x, z)
        mes("The catapult hurls a rock across the river.")
    }

    private fun ProtectedAccess.ignite(owner: CwTeam) {
        val team = playingTeam() ?: return
        if (team == owner) {
            mes("You can't set fire to your own team's catapult.")
            return
        }
        if (catapults.ignite(owner, mapClock)) mes("You set fire to the catapult.")
    }

    private fun ProtectedAccess.douse(owner: CwTeam) {
        val team = playingTeam() ?: return
        if (team != owner) {
            mes("You can only put out your own team's catapult.")
            return
        }
        if (invDel(inv, "obj.bucket_water").failure) return
        invAdd(inv, "obj.bucket_empty")
        if (catapults.extinguish(owner)) mes("You put out the fire.")
    }

    private fun ProtectedAccess.repair(owner: CwTeam) {
        val team = playingTeam() ?: return
        if (team != owner) {
            mes("You can only repair your own team's catapult.")
            return
        }
        if (!inv.contains("obj.castlewars_toolkit")) {
            mes("You need a toolkit to repair the catapult.")
            return
        }
        if (catapults.repair(owner)) mes("You repair the catapult.")
    }

    private fun ProtectedAccess.playingTeam(): CwTeam? {
        val team = service.teamOf(player)?.takeIf { service.isPlaying(player) }
        if (team == null) mes("You can only use that during a game.")
        return team
    }
}
