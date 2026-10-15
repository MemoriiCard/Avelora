package org.rsmod.content.minigames.wintertodt

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class WintertodtScript
@Inject
constructor(
    private val service: WintertodtService,
    private val random: GameRandom,
    private val objs: ObjRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.wint_door") { passDoor() }
        onOpLoc1("loc.wint_roots") { chop() }
        onOpLoc1("loc.wint_herb_roots") { pickHerb() }
        onOpLoc1("loc.wint_brazier") { light(it.loc) }
        onOpLoc1("loc.wint_brazier_lit") { feed(it.loc) }
        onOpLoc1("loc.wint_brazier_broken") { repair(it.loc) }
        onOpLoc1("loc.wint_reward_pile") { search(1) }
        onOpLoc3("loc.wint_reward_pile") { search(ALL) }
        onOpNpc1("npc.wint_wizard_down") { help(it.npc) }
        for ((chest, obj) in CHESTS) {
            onOpLoc1(chest) { take(obj) }
        }
        onOpHeldU("obj.knife", "obj.wint_bruma_root") { fletch() }
        onOpHeldU("obj.wint_herb", "obj.wint_vial") { mix("obj.wint_vial") }
        for (dose in 1..3) {
            onOpHeldU("obj.wint_herb", "obj.wint_potion$dose") { mix("obj.wint_potion$dose") }
        }
        for (dose in 1..4) {
            onOpHeld1("obj.wint_potion$dose") { drink(dose) }
        }
        onPlayerSoftTimer(WintertodtService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toLobby = false) }
        onPlayerLogin {
            if (WintertodtMap.inside(player.coords)) {
                PathingEntityCommon.telejump(player, collision, WintertodtMap.LOBBY_EXIT)
            }
        }
    }

    private fun ProtectedAccess.passDoor() {
        if (service.isInside(player)) {
            service.leave(player)
        } else {
            service.enter(player)
        }
    }

    private fun ProtectedAccess.arenaActive(): Boolean {
        if (!service.isInside(player)) return false
        if (!service.round.active) {
            mes("The Wintertodt is resting. Wait for it to awaken.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.help(npc: Npc) {
        if (!arenaActive()) return
        anim("seq.human_pickupfloor")
        delay(2)
        if (service.revive(player, npc)) mes("You help the pyromancer back to their feet.")
    }

    private fun ProtectedAccess.take(obj: String) {
        if (!service.isInside(player)) return
        if (inv.contains(obj) && obj != "obj.wint_vial") {
            mes("You already have one of those.")
            return
        }
        if (invAdd(inv, obj).failure) mes("You don't have enough inventory space to do that.")
    }

    private suspend fun ProtectedAccess.chop() {
        if (!arenaActive()) return
        if (AXES.none { inv.contains(it) }) {
            mes("You need an axe to cut the roots. There is one in the chest nearby.")
            return
        }
        while (service.round.active && service.isInside(player)) {
            if (inv.isFull()) {
                mes("Your inventory is too full to hold any more roots.")
                return
            }
            delay(CHOP_TICKS)
            if (random.of(100) >= minOf(MAX_CHOP_CHANCE, BASE_CHOP_CHANCE + player.woodcuttingLvl / 2)) continue
            invAdd(inv, "obj.wint_bruma_root")
            statAdvance("stat.woodcutting", player.woodcuttingLvl * 0.3)
        }
    }

    private suspend fun ProtectedAccess.pickHerb() {
        if (!arenaActive()) return
        delay(2)
        if (invAdd(inv, "obj.wint_herb").failure) mes("You don't have enough inventory space to do that.")
    }

    private suspend fun ProtectedAccess.fletch() {
        if (!service.isInside(player)) return
        while (inv.contains("obj.wint_bruma_root") && service.isInside(player)) {
            delay(FLETCH_TICKS)
            if (invDel(inv, "obj.wint_bruma_root").failure) return
            invAdd(inv, "obj.wint_bruma_kindling")
            statAdvance("stat.fletching", player.fletchingLvl * 0.6)
        }
    }

    private suspend fun ProtectedAccess.mix(base: String) {
        if (!service.isInside(player)) return
        val dose = DOSES[base] ?: 0
        if (invDel(inv, "obj.wint_herb").failure || invDel(inv, base).failure) return
        invAdd(inv, "obj.wint_potion${dose + 1}")
        statAdvance("stat.herblore", player.herbloreLvl * 0.1)
    }

    private fun ProtectedAccess.drink(dose: Int) {
        if (!service.isInside(player)) return
        if (invDel(inv, "obj.wint_potion$dose").failure) return
        if (dose > 1) invAdd(inv, "obj.wint_potion${dose - 1}") else invAdd(inv, "obj.wint_vial")
        service.restore(player, POTION_WARMTH)
        mes("The rejuvenation potion warms you up.")
    }

    private fun ProtectedAccess.light(loc: BoundLocInfo) {
        val index = WintertodtMap.brazierAt(loc.coords) ?: return
        if (!arenaActive()) return
        if (!inv.contains("obj.tinderbox") && !inv.contains("obj.wint_torch")) {
            mes("You need a tinderbox to light the brazier.")
            return
        }
        if (service.light(player, index)) {
            statAdvance("stat.firemaking", player.firemakingLvl * LIGHT_XP)
            mes("You light the brazier.")
        }
    }

    private suspend fun ProtectedAccess.feed(loc: BoundLocInfo) {
        val index = WintertodtMap.brazierAt(loc.coords) ?: return
        if (!arenaActive()) return
        while (service.round.active && service.isInside(player)) {
            val (obj, points, xp) =
                when {
                    inv.contains("obj.wint_bruma_kindling") -> Feed("obj.wint_bruma_kindling", WintertodtRound.KINDLING_POINTS, KINDLING_XP)
                    inv.contains("obj.wint_bruma_root") -> Feed("obj.wint_bruma_root", WintertodtRound.ROOT_POINTS, ROOT_XP)
                    else -> return
                }
            delay(FEED_TICKS)
            if (invDel(inv, obj).failure) return
            if (!service.feed(player, index, points)) {
                invAdd(inv, obj)
                return
            }
            statAdvance("stat.firemaking", player.firemakingLvl * xp)
        }
    }

    private fun ProtectedAccess.repair(loc: BoundLocInfo) {
        val index = WintertodtMap.brazierAt(loc.coords) ?: return
        if (!arenaActive()) return
        if (!inv.contains("obj.hammer")) {
            mes("You need a hammer to fix the brazier.")
            return
        }
        if (service.repair(player, index)) {
            statAdvance("stat.construction", player.constructionLvl * REPAIR_XP)
            mes("You fix the brazier.")
        }
    }

    private fun ProtectedAccess.search(count: Int) {
        val pool = service.poolOf(player)
        if (pool == 0) {
            mes("The reward cart is empty. Subdue the Wintertodt with at least ${WintertodtRound.MIN_POINTS} points to earn a search.")
            return
        }
        val levels = Levels { stat -> player.stat(stat) }
        var searched = 0
        while (searched < minOf(count, pool) && inv.hasFreeSpace()) {
            val drop = WintertodtLoot.roll(levels) { random.of(it) }
            invAddOrDrop(objs, drop.obj, drop.count)
            mes("You find ${drop.count} x ${drop.obj.substringAfter('.').replace('_', ' ')}.")
            searched++
        }
        if (searched == 0) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        service.spendPool(player, searched)
    }

    private class Feed(val obj: String, val points: Int, val xp: Double) {
        operator fun component1() = obj

        operator fun component2() = points

        operator fun component3() = xp
    }

    private companion object {
        const val ALL = 28
        const val CHOP_TICKS = 3
        const val FLETCH_TICKS = 2
        const val FEED_TICKS = 2
        const val BASE_CHOP_CHANCE = 35
        const val MAX_CHOP_CHANCE = 90
        const val POTION_WARMTH = 300
        const val LIGHT_XP = 6.0
        const val ROOT_XP = 3.0
        const val KINDLING_XP = 3.8
        const val REPAIR_XP = 4.0

        val DOSES =
            mapOf(
                "obj.wint_vial" to 0,
                "obj.wint_potion1" to 1,
                "obj.wint_potion2" to 2,
                "obj.wint_potion3" to 3,
            )

        val AXES =
            listOf(
                "obj.bronze_axe",
                "obj.iron_axe",
                "obj.steel_axe",
                "obj.black_axe",
                "obj.mithril_axe",
                "obj.adamant_axe",
                "obj.rune_axe",
                "obj.dragon_axe",
            )

        val CHESTS =
            listOf(
                "loc.wint_chest_axe" to "obj.bronze_axe",
                "loc.wint_chest_tinderbox" to "obj.tinderbox",
                "loc.wint_chest_hammer" to "obj.hammer",
                "loc.wint_chest_knife" to "obj.knife",
                "loc.wint_chest_vial" to "obj.wint_vial",
            )
    }
}
