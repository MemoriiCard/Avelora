package org.rsmod.content.minigames.tempoross

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.torso
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class TemporossScript
@Inject
constructor(
    private val service: TemporossService,
    private val random: GameRandom,
    private val objs: ObjRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.tempoross_lobby_ladder") { board() }
        onOpLoc1("loc.tempoross_lobby_boat") { board() }
        onOpLoc1("loc.tempoross_rewardpool") { search(1) }
        onOpLoc3("loc.tempoross_rewardpool") { search(ALL) }
        for ((loc, obj) in CRATES) {
            onOpLoc1(loc) { take(obj) }
        }
        for (pump in PUMPS) {
            onOpLoc1(pump) { fillBucket() }
            onOpLocU(pump, "obj.bucket_empty") { fillBucket() }
        }
        for (spot in listOf(TemporossService.SPOT_SOUTH, TemporossService.SPOT_NORTH, TemporossService.SPOT_SPECIAL)) {
            onOpNpc1(spot) { fish(spot == TemporossService.SPOT_SPECIAL) }
        }
        onOpNpc1(TemporossService.SPIRIT_SPOT) { spiritPool() }
        onOpLocU("loc.tempoross_shrine_fire", "obj.tempoross_raw_harpoonfish") { cook() }
        for (index in 1..TemporossRound.CANNONS) {
            onOpLoc1("loc.tempoross_crate_ammunition_empty_$index") { deposit(it.loc) }
        }
        for (site in 0 until TemporossRound.SITES) {
            val (whole, broken) = TemporossMap.SITE_LOCS[site]
            onOpLoc1(whole) { tether(site) }
            onOpLocU(whole, "obj.rope") { tether(site) }
            onOpLoc1(broken) { repair(site) }
            onOpLocU(broken, "obj.hammer") { repair(site) }
        }
        val fire = checkNotNull(ServerCacheManager.getNpc(TemporossService.FIRE_NPC.asRSCM(RSCMType.NPC)))
        val bucket = checkNotNull(ServerCacheManager.getItem("obj.bucket_water".asRSCM(RSCMType.OBJ)))
        onOpNpcU(fire, bucket) { douse(it.npc) }
        onPlayerSoftTimer(TemporossService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toLobby = false) }
        onPlayerLogin {
            if (TemporossMap.inside(player.coords)) {
                PathingEntityCommon.telejump(player, collision, TemporossMap.LOBBY_EXIT)
            }
        }
    }

    private fun ProtectedAccess.board() {
        if (service.isInside(player)) {
            mes("You are already aboard the ship.")
            return
        }
        service.board(player)
    }

    private fun ProtectedAccess.playing(): Boolean {
        if (!service.isInside(player)) return false
        if (!service.round.active) {
            mes("The storm hasn't broken yet.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.take(obj: String) {
        if (!service.isInside(player)) return
        if (inv.contains(obj)) {
            mes("You already have one of those.")
            return
        }
        if (invAdd(inv, obj).failure) mes("You don't have enough inventory space to do that.")
    }

    private fun ProtectedAccess.fillBucket() {
        if (!service.isInside(player)) return
        var filled = 0
        while (invDel(inv, "obj.bucket_empty").success) {
            invAdd(inv, "obj.bucket_water")
            filled++
        }
        if (filled == 0) mes("You need an empty bucket to use the pump.")
    }

    private fun ProtectedAccess.hasHarpoon(): Boolean =
        HARPOONS.any { inv.contains(it) || player.worn.contains(it) }

    private suspend fun ProtectedAccess.fish(special: Boolean) {
        if (!playing()) return
        if (service.round.phase != TemporossPhase.Energy) {
            mes("Tempoross is submerged. Fish the spirit pools instead.")
            return
        }
        if (!hasHarpoon()) {
            mes("You need a harpoon to fish here. There is one in the nearby crate.")
            return
        }
        while (service.round.phase == TemporossPhase.Energy && service.isInside(player)) {
            if (inv.isFull()) {
                mes("Your inventory is too full to hold any more fish.")
                return
            }
            delay(FISH_TICKS)
            val count = if (special && random.of(100) < DOUBLE_CHANCE) 2 else 1
            repeat(count) {
                if (invAdd(inv, "obj.tempoross_raw_harpoonfish").success) {
                    service.award(player, TemporossRound.FISH_POINTS)
                    statAdvance("stat.fishing", fishXp(player.fishingLvl) / 100.0)
                }
            }
        }
    }

    private suspend fun ProtectedAccess.cook() {
        if (!playing()) return
        while (inv.contains("obj.tempoross_raw_harpoonfish") && service.round.active) {
            delay(COOK_TICKS)
            if (invDel(inv, "obj.tempoross_raw_harpoonfish").failure) return
            invAdd(inv, "obj.tempoross_harpoonfish")
            service.award(player, TemporossRound.COOK_POINTS)
            statAdvance("stat.cooking", fishXp(player.cookingLvl) / 100.0)
        }
    }

    private suspend fun ProtectedAccess.deposit(loc: BoundLocInfo) {
        if (!playing()) return
        val cannon = TemporossMap.crateAt(loc.coords) ?: return
        if (service.round.phase != TemporossPhase.Energy) {
            mes("The cannons are quiet while Tempoross is submerged.")
            return
        }
        while (service.round.phase == TemporossPhase.Energy && service.isInside(player)) {
            if (service.round.cannonDisabled(cannon)) {
                mes("This cannon is soaked and can't be loaded right now.")
                return
            }
            val (obj, cooked) =
                when {
                    inv.contains("obj.tempoross_harpoonfish") -> "obj.tempoross_harpoonfish" to true
                    inv.contains("obj.tempoross_raw_harpoonfish") -> "obj.tempoross_raw_harpoonfish" to false
                    else -> return
                }
            delay(LOAD_TICKS)
            if (invDel(inv, obj).failure) return
            if (!service.deposit(player, cooked)) {
                invAdd(inv, obj)
                return
            }
            statAdvance("stat.fishing", fishXp(player.fishingLvl) / 10.0)
        }
    }

    private suspend fun ProtectedAccess.spiritPool() {
        if (!playing()) return
        if (!service.spiritPool()) {
            mes("The spirit pool is still. Wait for Tempoross to submerge.")
            return
        }
        while (service.spiritPool() && service.isInside(player)) {
            delay(SPIRIT_TICKS)
            val damage = 1 + random.of(MAX_HIT)
            if (!service.round.strike(player.slotId, damage)) return
            statAdvance("stat.fishing", fishXp(player.fishingLvl) * SPIRIT_XP_PERCENT / 10_000.0)
        }
    }

    private fun ProtectedAccess.hasOutfit(): Boolean =
        player.hat.isType("obj.spirit_angler_hat") &&
            player.torso.isType("obj.spirit_angler_top") &&
            player.legs.isType("obj.spirit_angler_legs") &&
            player.feet.isType("obj.spirit_angler_boots")

    private fun ProtectedAccess.tether(site: Int) {
        if (!playing()) return
        if (!inv.contains("obj.rope") && !hasOutfit()) {
            mes("You need a rope to tether yourself.")
            return
        }
        if (service.tether(player, site)) {
            mes("You tether yourself to the ${if (site < 2) "mast" else "totem pole"}.")
        }
    }

    private fun ProtectedAccess.repair(site: Int) {
        if (!playing()) return
        if (!inv.contains("obj.hammer")) {
            mes("You need a hammer to repair this.")
            return
        }
        if (service.repair(player, site)) mes("You repair the damage.")
    }

    private fun ProtectedAccess.douse(npc: Npc) {
        if (!playing()) return
        val slot = service.fireSlot(npc) ?: return
        if (invDel(inv, "obj.bucket_water").failure) return
        invAdd(inv, "obj.bucket_empty")
        if (!service.douse(player, slot)) return
        mes("You douse the flames.")
    }

    private fun ProtectedAccess.search(count: Int) {
        val permits = service.permitsOf(player)
        if (permits == 0) {
            mes("You have no reward permits. Earn ${TemporossLoot.FIRST_PERMIT_POINTS} points in a round to get one.")
            return
        }
        if (!inv.contains("obj.net") && !inv.contains("obj.big_net")) {
            mes("You need a fishing net to carry the rewards away.")
            return
        }
        var searched = 0
        while (searched < minOf(count, permits) && inv.hasFreeSpace()) {
            val (drop, amount) = TemporossLoot.roll { random.of(it) }
            invAddOrDrop(objs, drop.obj, amount)
            mes("You find $amount x ${drop.obj.substringAfter('.').replace('_', ' ')}.")
            searched++
        }
        if (searched == 0) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        service.spendPermits(player, searched)
    }

    private fun fishXp(level: Int): Int =
        if (level >= HIGH_LEVEL) {
            XP_HIGH_BASE + XP_HIGH_STEP * (level - HIGH_LEVEL) / HIGH_SPAN
        } else {
            XP_LOW_BASE + XP_LOW_STEP * (level - TemporossService.MIN_FISHING).coerceAtLeast(0) / LOW_SPAN
        }

    private companion object {
        const val ALL = 28
        const val FISH_TICKS = 3
        const val COOK_TICKS = 2
        const val LOAD_TICKS = 1
        const val SPIRIT_TICKS = 4
        const val MAX_HIT = 12
        const val DOUBLE_CHANCE = 50
        const val SPIRIT_XP_PERCENT = 1100
        const val HIGH_LEVEL = 70
        const val XP_LOW_BASE = 450
        const val XP_LOW_STEP = 550
        const val LOW_SPAN = 64
        const val XP_HIGH_BASE = 890
        const val XP_HIGH_STEP = 110
        const val HIGH_SPAN = 29

        val HARPOONS =
            listOf(
                "obj.harpoon",
                "obj.dragon_harpoon",
                "obj.infernal_harpoon",
                "obj.crystal_harpoon",
                "obj.hunting_barbed_harpoon",
            )

        val PUMPS =
            listOf(
                "loc.tempoross_water_pump",
                "loc.tempoross_water_pump_2",
                "loc.tempoross_water_pump_dock",
            )

        val CRATES =
            listOf(
                "loc.tempoross_crate_bucket" to "obj.bucket_empty",
                "loc.tempoross_crate_hammer" to "obj.hammer",
                "loc.tempoross_crate_harpoon" to "obj.harpoon",
                "loc.tempoross_crate_rope" to "obj.rope",
            )
    }
}
