package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Everything a player can do in a resource room: tools, herb patches, gourds, fishing and bats. */
class ResourceScript
@Inject
constructor(
    private val raids: CoxRaids,
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val supplyPoints: CoxSupplyPoints,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(TOOLS) { take(listOf(RAKE, SPADE, DIBBER)) }
        onOpLoc2(TOOLS) { take(listOf(RAKE)) }
        onOpLoc3(TOOLS) { take(listOf(SPADE)) }
        onOpLoc4(TOOLS) { take(listOf(DIBBER)) }

        onOpLoc1(WEEDS) { rake(it.loc) }

        onOpLoc2(ResourceRoom.PATCH_EMPTY) { inspect(it.loc) }
        for (herb in ResourceRoom.Herb.entries) {
            onOpLocU(ResourceRoom.PATCH_EMPTY, herb.seed) { plant(it.loc, herb) }
            for (stage in GROWING) onOpLoc2(herb.loc(stage)) { inspect(it.loc) }
            onOpLoc1(herb.loc(GROWN)) { pick(it.loc) }
            onOpLoc2(herb.loc(GROWN)) { inspect(it.loc) }
            onOpLoc3(herb.loc(GROWN)) { clearPatch(it.loc) }
        }

        onOpLoc1(GOURD_TREE) { pickGourds(1) }
        onOpLoc2(GOURD_TREE) { pickGourds(inv.freeSpace().coerceAtMost(MAX_GOURDS)) }
        onOpLoc1(GEYSER) { fillGourds() }
        onOpLocU(GEYSER, VIAL_EMPTY) { fillGourds() }

        onOpLoc1(ResourceRoom.FISHING_SPOT) { fish(it.loc.coords) }
        for (tier in 0..6) onOpNpc1("npc.raids_bat_$tier") { catchBat(it.npc, tier) }
    }

    private fun ProtectedAccess.take(tools: List<String>) {
        var taken = 0
        for (tool in tools) {
            if (playerContainsObj(tool) || inv.isFull()) continue
            invAdd(inv, tool)
            taken++
        }
        if (taken > 0) anim(PICKUP_SEQ)
        else mes("You already have everything from here that you can carry.")
    }

    private suspend fun ProtectedAccess.rake(weeds: BoundLocInfo) {
        if (!playerContainsObj(RAKE)) {
            mes("You need a rake to clear the weeds.")
            return
        }
        val raid = raids.containing(player) ?: return
        anim(RAKE_SEQ)
        delay(RAKE_DELAY)
        val seed = SEED_CYCLE[raid.rakes.merge(player, 1, Int::plus)!! % SEED_CYCLE.size]
        invAddOrDrop(objRepo, seed)
        statAdvance("stat.farming", RAKE_XP)
        locRepo.del(weeds, Int.MAX_VALUE)
        resetAnim()
        mes("You rake the weeds and find a seed.")
    }

    private fun ProtectedAccess.inspect(patch: BoundLocInfo) {
        val room = roomFor(patch.coords) ?: return
        val state = room.patchAt(patch.coords) ?: return
        val herb = state.herb
        when {
            herb == null -> mes("This herb patch is empty. Plant a seed with a seed dibber.")
            state.grown -> mes("The ${herb.key} is fully grown and ready to pick.")
            else -> mes("The ${herb.key} is still growing.")
        }
    }

    private suspend fun ProtectedAccess.plant(patch: BoundLocInfo, herb: ResourceRoom.Herb) {
        val room = roomFor(patch.coords) ?: return
        val state = room.patchAt(patch.coords) ?: return
        if (state.herb != null) return
        if (player.farmingLvl < herb.level) {
            mes("You need a Farming level of ${herb.level} to plant ${herb.key} seeds.")
            return
        }
        if (!playerContainsObj(DIBBER)) {
            mes("You need a seed dibber to plant seeds.")
            return
        }
        anim(DIBBER_SEQ)
        delay(PLANT_DELAY)
        if (invDel(inv, herb.seed).failure) return
        room.plant(state, herb)
        statAdvance("stat.farming", herb.plantXp)
        resetAnim()
        mes("You plant the seed in the herb patch.")
    }

    private suspend fun ProtectedAccess.pick(patch: BoundLocInfo) {
        val room = roomFor(patch.coords) ?: return
        val state = room.patchAt(patch.coords) ?: return
        val herb = state.herb ?: return
        if (!state.grown) return
        anim(PICK_SEQ)
        delay(PICK_DELAY)
        val count = room.harvest(state)
        if (count == 0) return
        invAddOrDrop(objRepo, herb.item, count)
        statAdvance("stat.farming", herb.harvestXp * count)
        resetAnim()
        mes("You pick $count ${herb.key} from the patch.")
    }

    private fun ProtectedAccess.clearPatch(patch: BoundLocInfo) {
        val room = roomFor(patch.coords) ?: return
        val state = room.patchAt(patch.coords) ?: return
        if (state.herb == null) return
        room.wipe(state)
        mes("You clear the herb patch.")
    }

    private suspend fun ProtectedAccess.pickGourds(count: Int) {
        if (count <= 0 || inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(PICKUP_SEQ)
        delay(PICK_DELAY)
        invAdd(inv, VIAL_EMPTY, count = count, strict = false)
        resetAnim()
        mes(if (count == 1) "You pick a gourd." else "You pick $count gourds.")
    }

    private suspend fun ProtectedAccess.fillGourds() {
        val empties = inv.count(VIAL_EMPTY)
        if (empties == 0) {
            mes("You have no empty gourd vials to fill.")
            return
        }
        anim(FILL_SEQ)
        delay(PICK_DELAY)
        if (invDel(inv, VIAL_EMPTY, count = empties).failure) return
        invAdd(inv, VIAL_WATER, count = empties, strict = false)
        resetAnim()
        mes("You fill the gourd vials with water.")
    }

    private suspend fun ProtectedAccess.fish(spot: CoordGrid) {
        val room = roomFor(spot) ?: return
        val tier = room.foodTier
        val required = tier * ResourceRoom.LEVELS_PER_TIER
        if (!playerContainsObj(ROD)) {
            mes("You need a fishing rod to fish here.")
            return
        }
        if (player.fishingLvl < required) {
            mes("You need a Fishing level of $required to catch fish here.")
            return
        }
        while (true) {
            if (inv.isFull()) {
                mes("Your inventory is too full to hold any more fish.")
                break
            }
            if (!playerContainsObj(WORMS)) {
                mes("You have no cave worms left.")
                break
            }
            anim(FISH_SEQ)
            delay(FISH_DELAY)
            if (invDel(inv, WORMS).failure) break
            if (random.of(1, 100) > catchChance(player.fishingLvl, required)) continue
            invAdd(inv, "obj.raids_fish${tier}_raw", strict = false)
            statAdvance("stat.fishing", xpFor(tier))
            supplyPoints.food(player, tier)
            mes("You catch a ${ResourceRoom.FISH_NAMES[tier]}.")
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.catchBat(bat: Npc, tier: Int) {
        val room = roomFor(bat.coords) ?: return
        val required = tier * ResourceRoom.LEVELS_PER_TIER
        if (player.hunterLvl < required) {
            mes("You need a Hunter level of $required to catch this bat.")
            return
        }
        if (!playerContainsObj(NET) && player.hunterLvl < BARE_HANDS_LEVEL) {
            mes("You need a butterfly net to catch bats.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(NET_SEQ)
        delay(NET_DELAY)
        if (!bat.isSlotAssigned) return
        if (random.of(1, 100) > catchChance(player.hunterLvl, required)) {
            mes("The bat flutters out of your reach.")
            return
        }
        room.batCaught(bat)
        invAdd(inv, "obj.raids_bat${tier}_raw", strict = false)
        statAdvance("stat.hunter", xpFor(tier))
        supplyPoints.food(player, tier)
        resetAnim()
        mes("You catch a ${ResourceRoom.BAT_NAMES[tier]} bat.")
    }

    private fun ProtectedAccess.roomFor(coords: CoordGrid): ResourceRoom? {
        val raid = raids.containing(player) ?: return null
        return raid.roomAt(coords)?.let(raid::controllerOf) as? ResourceRoom
    }

    companion object {
        private const val TOOLS = "loc.raids_farming_tools"
        private const val WEEDS = "loc.raids_weeds"
        private const val GOURD_TREE = "loc.raids_gourd_tree"
        private const val GEYSER = "loc.raids_geyser"
        private const val RAKE = "obj.rake"
        private const val SPADE = "obj.spade"
        private const val DIBBER = "obj.dibber"
        private const val ROD = "obj.fishing_rod"
        private const val NET = "obj.hunting_butterfly_net"
        private const val WORMS = "obj.raids_fishingbait"
        private const val VIAL_EMPTY = "obj.raids_vial_empty"
        private const val VIAL_WATER = "obj.raids_vial_water"
        private const val PICKUP_SEQ = "seq.human_pickupfloor"
        private const val RAKE_SEQ = "seq.farming_raking"
        private const val DIBBER_SEQ = "seq.farming_seed_dibbing"
        private const val PICK_SEQ = "seq.picking_mid"
        private const val FILL_SEQ = "seq.farming_pour_water"
        private const val FISH_SEQ = "seq.human_fishing_casting"
        private const val NET_SEQ = "seq.human_catch"
        private val GROWING = listOf("seed", "growth1", "growth2", "growth3")
        private const val GROWN = "fullygrown"
        private val SEED_CYCLE =
            listOf("obj.raids_seed_golpar", "obj.raids_seed_buchuleaf", "obj.raids_seed_noxifer")
        private const val RAKE_XP = 4.0
        private const val RAKE_DELAY = 3
        private const val PLANT_DELAY = 2
        private const val PICK_DELAY = 2
        private const val FISH_DELAY = 4
        private const val NET_DELAY = 2
        private const val MAX_GOURDS = 20
        private const val BARE_HANDS_LEVEL = 99
        private const val MIN_CATCH = 30
        private const val MAX_CATCH = 85

        fun catchChance(level: Int, required: Int): Int =
            (MIN_CATCH + (level - required)).coerceIn(MIN_CATCH, MAX_CATCH)

        fun xpFor(tier: Int): Double = 20.0 + 15.0 * tier
    }
}
