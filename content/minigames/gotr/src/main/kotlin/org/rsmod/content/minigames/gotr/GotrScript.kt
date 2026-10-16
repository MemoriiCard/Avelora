package org.rsmod.content.minigames.gotr

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseRunecraftingLvl
import org.rsmod.api.player.stat.runecraftingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class GotrScript
@Inject
constructor(
    private val service: GotrService,
    private val random: GameRandom,
    private val pets: PetRewards,
    private val objs: ObjRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.gotr_barrier") { service.passBarrier(player) }
        onOpLoc3("loc.gotr_barrier") { service.passBarrier(player) }
        for ((loc, yield) in REMAINS) {
            onOpLoc1(loc) { mine(yield) }
        }
        onOpLoc1("loc.gotr_workbench") { craftEssence() }
        onOpLoc1("loc.gotr_uncharged_cells") { takeCells(CELL_STACK) }
        onOpLoc2("loc.gotr_uncharged_cells") { takeCells(1) }
        onOpLoc1("loc.gotr_weak_cells") { takeCells(1) }
        onOpLoc2("loc.gotr_weak_cells") { takeChisel() }
        onOpLoc1("loc.gotr_guardian_spawn_east") { assemble(GotrKind.Elemental) }
        onOpLoc1("loc.gotr_guardian_spawn_west") { assemble(GotrKind.Catalytic) }
        for (rune in GotrRune.entries) {
            onOpLoc1(rune.portalLoc) { imbue(rune) }
            onOpLocU(rune.portalLoc, "obj.gotr_cell_uncharged") { chargeCell(rune) }
        }
        onOpLoc1("loc.gotr_cell_tile_inactive") { placeCell(it.loc) }
        onOpNpc1("npc.gotr_great_guardian") { powerUp() }
        onOpNpc2("npc.gotr_great_guardian") { powerUp() }
        onOpLoc1("loc.gotr_reward_rift") { search(1) }
        onOpLoc2("loc.gotr_reward_rift") { check() }
        onOpLoc3("loc.gotr_reward_rift") { search(BIG_SEARCH) }
        onPlayerSoftTimer(GotrService.TIMER) { service.tick() }
        onPlayerLogout { service.leave(player, toLobby = false) }
        onPlayerLogin {
            if (GotrMap.inside(player.coords) && player.coords.z > GotrMap.BARRIER_Z) {
                PathingEntityCommon.telejump(player, collision, GotrMap.LOBBY_BARRIER_SIDE)
            }
        }
    }

    private fun ProtectedAccess.playing(): Boolean {
        if (!service.isInside(player)) return false
        if (!service.round.active) {
            mes("The Great Guardian hasn't awoken yet.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasPickaxe(): Boolean =
        PICKAXES.any { inv.contains(it) || player.worn.contains(it) }

    private suspend fun ProtectedAccess.mine(yield: Int) {
        if (!playing()) return
        if (!hasPickaxe()) {
            mes("You need a pickaxe to mine the guardian remains.")
            return
        }
        while (service.round.active && service.isInside(player)) {
            if (inv.isFull()) {
                mes("Your inventory is too full to hold any more fragments.")
                return
            }
            delay(MINE_TICKS)
            if (invAdd(inv, "obj.gotr_guardian_fragment", yield).failure) return
            statAdvance("stat.mining", MINE_XP * yield)
        }
    }

    private suspend fun ProtectedAccess.craftEssence() {
        if (!service.isInside(player)) return
        if (!inv.contains("obj.chisel")) {
            mes("You need a chisel to work the guardian fragments.")
            return
        }
        if (!inv.contains("obj.gotr_guardian_fragment")) {
            mes("You have no guardian fragments to work.")
            return
        }
        while (inv.contains("obj.gotr_guardian_fragment") && service.isInside(player)) {
            delay(WORK_TICKS)
            val count = inv.count("obj.gotr_guardian_fragment").coerceAtMost(WORK_BATCH)
            if (invDel(inv, "obj.gotr_guardian_fragment", count).failure) return
            if (invAdd(inv, "obj.gotr_guardian_essence", count).failure) {
                invAdd(inv, "obj.gotr_guardian_fragment", count)
                mes("You don't have enough inventory space to do that.")
                return
            }
        }
    }

    private fun ProtectedAccess.takeCells(amount: Int) {
        if (!service.isInside(player)) return
        val held = inv.count("obj.gotr_cell_uncharged")
        val room = CELL_LIMIT - held
        if (room <= 0) {
            mes("You can only carry $CELL_LIMIT uncharged cells.")
            return
        }
        if (invAdd(inv, "obj.gotr_cell_uncharged", amount.coerceAtMost(room)).failure) {
            mes("You don't have enough inventory space to do that.")
        }
    }

    private fun ProtectedAccess.takeChisel() {
        if (!service.isInside(player) || inv.contains("obj.chisel")) return
        if (invAdd(inv, "obj.chisel").failure) mes("You don't have enough inventory space to do that.")
    }

    private fun ProtectedAccess.assemble(kind: GotrKind) {
        if (!playing()) return
        if (!inv.contains("obj.chisel")) {
            mes("You need a chisel to assemble a guardian.")
            return
        }
        if (inv.count("obj.gotr_guardian_essence") < GUARDIAN_COST) {
            mes("You need $GUARDIAN_COST guardian essence to assemble a guardian.")
            return
        }
        if (service.round.guardianCount >= GotrRound.GUARDIAN_LIMIT) {
            mes("The temple already holds as many guardians as it can sustain.")
            return
        }
        if (invDel(inv, "obj.gotr_guardian_essence", GUARDIAN_COST).failure) return
        service.assemble(player, kind)
    }

    private suspend fun ProtectedAccess.imbue(rune: GotrRune) {
        if (!playing()) return
        if (service.round.rune(rune.kind) != rune) {
            mes("The ${rune.name.lowercase()} portal is dormant. Wait for it to open.")
            return
        }
        if (player.baseRunecraftingLvl < rune.level) {
            mes("You need a Runecraft level of ${rune.level} to use this portal.")
            return
        }
        val stone =
            if (rune.kind == GotrKind.Elemental) "obj.gotr_guardian_stone_elemental" else "obj.gotr_guardian_stone_catalytic"
        while (service.round.active && inv.contains("obj.gotr_guardian_essence") && service.isInside(player)) {
            delay(IMBUE_TICKS)
            if (invDel(inv, "obj.gotr_guardian_essence").failure) return
            if (invAdd(inv, stone).failure) {
                invAdd(inv, "obj.gotr_guardian_essence")
                mes("You don't have enough inventory space to do that.")
                return
            }
            invAddOrDrop(objs, rune.runeObj, rune.runesPer(player.runecraftingLvl))
            statAdvance("stat.runecrafting", rune.xp)
            if (random.of(TALISMAN_ODDS) == 0) {
                invAddOrDrop(objs, rune.portalTalismanObj)
                mes("You find a portal talisman among the essence.")
            }
        }
    }

    private fun ProtectedAccess.chargeCell(rune: GotrRune) {
        if (!playing()) return
        if (service.round.rune(rune.kind) != rune) {
            mes("The ${rune.name.lowercase()} portal is dormant.")
            return
        }
        if (player.baseRunecraftingLvl < rune.level) {
            mes("You need a Runecraft level of ${rune.level} to charge a cell here.")
            return
        }
        if (GotrTier.entries.any { inv.contains(it.cellObj) }) {
            mes("You can only carry one charged cell at a time.")
            return
        }
        if (invDel(inv, "obj.gotr_cell_uncharged").failure) return
        invAdd(inv, rune.tier.cellObj)
        mes("The cell crackles with ${rune.tier.name.lowercase()} energy.")
    }

    private fun ProtectedAccess.placeCell(loc: BoundLocInfo) {
        if (!playing()) return
        val tile = GotrMap.tileIndex(loc.coords) ?: return
        val tier = GotrTier.entries.firstOrNull { inv.contains(it.cellObj) }
        if (tier == null) {
            mes("You need a charged cell to place here.")
            return
        }
        if (!service.placeCell(player, tile, tier)) {
            mes("This tile is still recharging.")
            return
        }
        invDel(inv, tier.cellObj)
        invAdd(inv, "obj.gotr_cell_uncharged")
        mes("The cell fuses into the barrier, feeding your energy.")
    }

    private fun ProtectedAccess.powerUp() {
        if (!playing()) return
        val plain = inv.count("obj.gotr_guardian_stone_elemental") + inv.count("obj.gotr_guardian_stone_catalytic")
        val poly = inv.count("obj.gotr_guardian_stone_polyelemental") + inv.count("obj.gotr_guardian_stone_polycatalytic")
        if (plain + poly == 0) {
            mes("You have no guardian stones to offer.")
            return
        }
        for (stone in STONES) invDel(inv, stone, inv.count(stone))
        service.deposit(player, plain, poly)
    }

    private fun ProtectedAccess.check() {
        val (ele, cat) = service.searchesOf(player)
        mes("You have $ele elemental and $cat catalytic points. Each search costs one of each, or ${GotrLoot.PEARL_SEARCH_COST} abyssal pearls.")
    }

    private fun ProtectedAccess.search(count: Int) {
        var searched = 0
        while (searched < count && inv.hasFreeSpace()) {
            val (ele, cat) = service.searchesOf(player)
            when {
                ele > 0 && cat > 0 -> service.spendSearch(player)
                inv.count("obj.abyssal_pearl") >= GotrLoot.PEARL_SEARCH_COST ->
                    invDel(inv, "obj.abyssal_pearl", GotrLoot.PEARL_SEARCH_COST)
                else -> break
            }
            val result = GotrLoot.roll(player.baseRunecraftingLvl) { random.of(it) }
            invAddOrDrop(objs, result.obj, result.amount)
            mes("You find ${result.amount} x ${result.obj.substringAfter('.').replace('_', ' ')}.")
            if (random.of(GotrLoot.PET_ODDS) == 0) pets.give(player, GotrLoot.PET)
            searched++
        }
        if (searched == 0) {
            mes("You need an elemental and a catalytic point, or ${GotrLoot.PEARL_SEARCH_COST} abyssal pearls, to search.")
        }
    }

    private companion object {
        const val BIG_SEARCH = 5
        const val MINE_TICKS = 4
        const val MINE_XP = 5.0
        const val WORK_TICKS = 1
        const val WORK_BATCH = 4
        const val CELL_LIMIT = 10
        const val CELL_STACK = 10
        const val GUARDIAN_COST = 10
        const val IMBUE_TICKS = 1
        const val TALISMAN_ODDS = 250

        val STONES =
            listOf(
                "obj.gotr_guardian_stone_elemental",
                "obj.gotr_guardian_stone_catalytic",
                "obj.gotr_guardian_stone_polyelemental",
                "obj.gotr_guardian_stone_polycatalytic",
            )

        val REMAINS =
            listOf(
                "loc.gotr_essence_tier_1" to 1,
                "loc.gotr_essence_tier_1b" to 1,
                "loc.gotr_essence_tier_1_large" to 2,
                "loc.gotr_essence_tier_1_large_b" to 2,
                "loc.gotr_essence_tier_2" to 3,
                "loc.gotr_essence_tier_3" to 4,
            )

        val PICKAXES =
            listOf(
                "obj.bronze_pickaxe",
                "obj.iron_pickaxe",
                "obj.steel_pickaxe",
                "obj.black_pickaxe",
                "obj.mithril_pickaxe",
                "obj.adamant_pickaxe",
                "obj.rune_pickaxe",
                "obj.dragon_pickaxe",
                "obj.infernal_pickaxe",
                "obj.crystal_pickaxe",
            )
    }
}
