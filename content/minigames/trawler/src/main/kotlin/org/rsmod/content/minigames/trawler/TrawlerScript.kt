package org.rsmod.content.minigames.trawler

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class TrawlerScript
@Inject
internal constructor(
    private val service: TrawlerService,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.trawler_gangplank") { board() }
        onOpLoc1("loc.trawler_gangplankoff") { board() }
        onOpNpc1("npc.murphy") { murphy() }
        onOpNpc1("npc.murphy_normal") { murphy() }
        onOpLoc1("loc.trawlernetland") { collectNet() }
        onOpLoc1("loc.trawler_ladder") { climb(up = true, it.loc.coords) }
        onOpLoc1("loc.trawler_laddertop") { climb(up = false, it.loc.coords) }
        onOpLoc1("loc.trawler_crate_bucket") { take("obj.bucket_bailing") }
        onOpLoc1("loc.trawler_crate_hammer") { take("obj.hammer") }
        onOpLoc1("loc.trawler_crate_axes") { take("obj.bronze_axe") }
        onOpLoc1("loc.trawler_hull_leak") { patch(it.loc.coords) }
        onOpLocU("loc.trawler_hull_leak", "obj.swamppaste") { patch(it.loc.coords) }
        onOpLocU("loc.trawlernet_l", "obj.rope") { mendNet(0) }
        onOpLocU("loc.trawlernet_r", "obj.rope") { mendNet(1) }
        onOpLoc1("loc.trawlernet_l") { inspectNet(0) }
        onOpLoc1("loc.trawlernet_r") { inspectNet(1) }
        onOpHeld1("obj.bucket_bailing") { bail() }
        onPlayerSoftTimer(TrawlerService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toDock = false) }
    }

    private fun ProtectedAccess.board() {
        if (player.fishingLvl < TrawlerLoot.MIN_FISHING) {
            mes("You can board, but you need level ${TrawlerLoot.MIN_FISHING} Fishing to earn a share of the catch.")
        }
        service.board(player)
    }

    private suspend fun ProtectedAccess.murphy() {
        startDialogue {
            if (service.isInside(access.player)) {
                val leave =
                    choice2(
                        "Take me back to shore.", true,
                        "I'll stay aboard.", false,
                        title = "Murphy: Abandoning the trawl?",
                    )
                if (leave) service.leave(access.player)
            } else {
                chatNpc(
                    neutral,
                    "Cross the gangplank to join the next trawl. Patch leaks with swamp paste, " +
                        "mend nets with rope and bail with a bucket. Earn 50 points for a share.",
                )
            }
        }
    }

    private fun ProtectedAccess.climb(up: Boolean, ladder: CoordGrid) {
        if (!service.isInside(player)) return
        val east = ladder.x > SHIP_MID_X
        val x = if (east) EAST_LADDER_X else WEST_LADDER_X
        val level = if (up) 1 else 0
        PathingEntityCommon.telejump(player, collision, CoordGrid(x, LADDER_Z, level))
    }

    private fun ProtectedAccess.take(obj: String) {
        if (!service.isInside(player)) return
        if (invAdd(inv, obj).failure) mes("You don't have enough inventory space.")
    }

    private fun ProtectedAccess.patch(coords: CoordGrid) {
        if (!service.active || !service.isInside(player)) return
        val hull = TrawlerHullMap.indexAt(coords.x, coords.z) ?: return
        if (!inv.contains("obj.swamppaste")) {
            mes("You need swamp paste to patch the leak.")
            return
        }
        if (service.patch(player, hull)) {
            invDel(inv, "obj.swamppaste", 1)
            mes("You plug the leak with swamp paste.")
        }
    }

    private fun ProtectedAccess.mendNet(net: Int) {
        if (!service.active || !service.isInside(player)) return
        if (service.round.netIntact(net)) {
            mes("The net is in one piece.")
            return
        }
        if (!inv.contains("obj.rope")) {
            mes("You need rope to mend the net.")
            return
        }
        val chance = (REPAIR_BASE + player.craftingLvl).coerceAtMost(REPAIR_CAP)
        if (random.of(100) < chance && service.repairNet(player, net)) {
            statAdvance("stat.crafting", NET_CRAFTING_XP)
            mes("You mend the net.")
        } else {
            mes("You fail to mend the net.")
        }
    }

    private fun ProtectedAccess.inspectNet(net: Int) {
        mes(if (service.round.netIntact(net)) "The net looks sound." else "The net is torn.")
    }

    private fun ProtectedAccess.bail() {
        if (!service.active || !service.isInside(player)) {
            mes("There's no water to bail out here.")
            return
        }
        if (service.bail(player)) {
            mes("You bail water out of the hold.")
        } else {
            mes("The hold is dry.")
        }
    }

    private fun ProtectedAccess.collectNet() {
        val rewards = player.invMap.getOrPut(TrawlerService.REWARD_INV)
        if (rewards.isEmpty()) {
            mes("There is nothing in the net.")
            return
        }
        var moved = false
        for (slot in rewards.indices) {
            val obj = rewards[slot] ?: continue
            if (player.invAdd(inv, obj.id, obj.count).failure) break
            rewards[slot] = null
            moved = true
        }
        mes(if (moved) "You take your catch from the net." else "You don't have enough inventory space.")
    }

    private companion object {
        const val REPAIR_BASE = 30
        const val REPAIR_CAP = 95
        const val NET_CRAFTING_XP = 5.0
        const val SHIP_MID_X = 1889
        const val WEST_LADDER_X = 1885
        const val EAST_LADDER_X = 1892
        const val LADDER_Z = 4825
    }
}
