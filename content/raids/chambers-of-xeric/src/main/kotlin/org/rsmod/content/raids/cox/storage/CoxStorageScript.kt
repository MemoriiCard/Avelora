package org.rsmod.content.raids.cox.storage

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.content.raids.cox.room.CoxSupplyPoints
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Storage units across the raid. Building or upgrading one converts every unit in the raid. Shared
 * storage takes only raid items, private storage takes anything the player is carrying.
 */
class CoxStorageScript
@Inject
constructor(
    private val raids: CoxRaids,
    private val locRepo: LocRepository,
    private val supplyPoints: CoxSupplyPoints,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc2(CoxStorage.UNITS[0]) { openPrivate() }
        onOpLoc5(CoxStorage.UNITS[0]) { build() }
        for (tier in 1..CoxStorage.MAX_TIER) {
            onOpLoc1(CoxStorage.UNITS[tier]) { openShared() }
            onOpLoc2(CoxStorage.UNITS[tier]) { openPrivate() }
            if (tier < CoxStorage.MAX_TIER) onOpLoc5(CoxStorage.UNITS[tier]) { build() }
        }
        for (tier in 0..CoxStorage.MAX_TIER) {
            onOpLocU(CoxStorage.UNITS[tier]) { depositHeld(tier, it.objType.internalName) }
        }
        onOpLoc2(LOBBY_UNIT) {
            mes("Anything you leave in a private storage unit is sent to your bank when you leave.")
        }
    }

    private suspend fun ProtectedAccess.build() {
        val raid = raids.containing(player) ?: return
        val storage = raid.storage
        val options =
            (storage.tier + 1..CoxStorage.MAX_TIER).map { tier ->
                val planks = CoxStorage.PLANKS[tier] - CoxStorage.PLANKS[storage.tier]
                "${CoxStorage.NAMES[tier].replaceFirstChar(Char::uppercase)} ($planks planks, " +
                    "Construction ${CoxStorage.CONSTRUCTION[tier]})" to tier
            }
        val tier = pickTier(options) ?: return
        val required = CoxStorage.CONSTRUCTION[tier]
        val planks = CoxStorage.PLANKS[tier] - CoxStorage.PLANKS[storage.tier]
        if (player.constructionLvl < required) {
            mes("You need a Construction level of $required to build this.")
            return
        }
        if (HAMMERS.none { playerContainsObj(it) }) {
            mes("You need a hammer to build a storage unit.")
            return
        }
        if (inv.count(CoxItems.PLANK) < planks) {
            mes("You need $planks mallignum root planks to build this.")
            return
        }
        anim(BUILD_SEQ)
        delay(BUILD_DELAY)
        if (invDel(inv, CoxItems.PLANK, count = planks).failure) return
        val built = tier - storage.tier
        storage.upgrade(tier)
        convertUnits(raid, tier)
        statAdvance("stat.construction", XP_PER_TIER * built)
        supplyPoints.storage(player, built)
        resetAnim()
        mes("You build a ${CoxStorage.NAMES[tier]} storage unit.")
    }

    private suspend fun ProtectedAccess.pickTier(options: List<Pair<String, Int>>): Int? {
        val labels = options.map { it.first }
        val tiers = options.map { it.second }
        val title = "Build which tier?"
        return when (options.size) {
            0 -> null
            1 -> choice2(labels[0], tiers[0], "Cancel", null, title = title)
            2 -> choice3(labels[0], tiers[0], labels[1], tiers[1], "Cancel", null, title = title)
            3 ->
                choice4(
                    labels[0], tiers[0], labels[1], tiers[1], labels[2], tiers[2], "Cancel", null,
                    title = title,
                )
            else ->
                choice5(
                    labels[0], tiers[0], labels[1], tiers[1], labels[2], tiers[2],
                    labels[3], tiers[3], "Cancel", null,
                    title = title,
                )
        }
    }

    private suspend fun ProtectedAccess.openShared() {
        val storage = raids.containing(player)?.storage ?: return
        while (true) {
            val choice =
                choice4(
                    "Donate all raid items", Action.DepositAll,
                    "Take an item (${storage.sharedItems().size} kinds)", Action.Withdraw,
                    "Check capacity", Action.Capacity,
                    "Close", Action.Close,
                    title = "Shared storage",
                )
            when (choice) {
                Action.DepositAll -> donateAll(storage)
                Action.Withdraw -> withdrawMenu(storage.sharedItems()) { obj, count -> storage.takeShared(obj, count) }
                Action.Capacity ->
                    mes("Shared storage holds ${storage.sharedTotal} of ${storage.sharedCapacity} items.")
                else -> return
            }
        }
    }

    private suspend fun ProtectedAccess.openPrivate() {
        val storage = raids.containing(player)?.storage ?: return
        while (true) {
            val choice =
                choice4(
                    "Store everything I'm carrying", Action.DepositAll,
                    "Take an item (${storage.privateItems(player).size} kinds)", Action.Withdraw,
                    "Take everything back", Action.WithdrawAll,
                    "Close", Action.Close,
                    title = "Private storage (${storage.privateSlots(player)}/${storage.privateCapacity})",
                )
            when (choice) {
                Action.DepositAll -> storeAll(storage)
                Action.Withdraw ->
                    withdrawMenu(storage.privateItems(player)) { obj, count -> storage.takePrivate(player, obj, count) }
                Action.WithdrawAll -> withdrawEverything(storage)
                else -> return
            }
        }
    }

    private suspend fun ProtectedAccess.depositHeld(tier: Int, obj: String) {
        val storage = raids.containing(player)?.storage ?: return
        val count = inv.count(obj)
        if (count == 0) return
        val title = CoxItems.displayName(obj)
        val toShared =
            if (tier > 0 && CoxItems.fitsShared(obj)) {
                choice3(
                    "Store in private storage", false,
                    "Donate to shared storage", true,
                    "Cancel", null,
                    title = title,
                )
            } else {
                choice2("Store in private storage", false, "Cancel", null, title = title)
            } ?: return
        val moved = if (toShared) storage.donate(obj, count) else storage.store(player, obj, count)
        if (moved <= 0) {
            mes(if (toShared) "Shared storage is full." else "Your private storage is full.")
            return
        }
        invDel(inv, obj, count = moved)
    }

    private fun ProtectedAccess.donateAll(storage: CoxStorage) {
        var moved = 0
        for (obj in heldTypeNames()) {
            if (!CoxItems.fitsShared(obj)) continue
            val donated = storage.donate(obj, inv.count(obj))
            if (donated > 0 && !invDel(inv, obj, count = donated).failure) moved += donated
        }
        mes(if (moved > 0) "You donate $moved items to shared storage." else "You have nothing to donate.")
    }

    private fun ProtectedAccess.storeAll(storage: CoxStorage) {
        var moved = 0
        for (obj in heldTypeNames()) {
            val stored = storage.store(player, obj, inv.count(obj))
            if (stored > 0 && !invDel(inv, obj, count = stored).failure) moved += stored
        }
        mes(if (moved > 0) "You store $moved items." else "You have nothing to store, or storage is full.")
    }

    private fun ProtectedAccess.withdrawEverything(storage: CoxStorage) {
        for ((obj, count) in storage.privateItems(player).toMap()) {
            val room = roomFor(obj)
            val amount = minOf(count, room)
            if (amount <= 0) continue
            val taken = storage.takePrivate(player, obj, amount)
            invAdd(inv, obj, count = taken, strict = false)
        }
    }

    private suspend fun ProtectedAccess.withdrawMenu(
        items: Map<String, Int>,
        take: (String, Int) -> Int,
    ) {
        if (items.isEmpty()) {
            mes("There is nothing stored here.")
            return
        }
        val entries = items.entries.toList()
        var page = 0
        while (true) {
            val pageCount = (entries.size + PAGE - 1) / PAGE
            val slice = entries.drop(page * PAGE).take(PAGE)
            fun label(index: Int) =
                slice.getOrNull(index)?.let { "${CoxItems.displayName(it.key)} (${it.value})" } ?: "-"
            val picked =
                choice5(
                    label(0), 0,
                    label(1), 1,
                    label(2), 2,
                    label(3), 3,
                    if (page + 1 < pageCount) "More..." else "Back", NEXT,
                    title = "Take which item?",
                )
            if (picked == NEXT) {
                if (page + 1 < pageCount) page++ else return
                continue
            }
            val (obj, stored) = slice.getOrNull(picked)?.toPair() ?: continue
            val amount = askAmount(stored)
            val room = roomFor(obj)
            val moved = minOf(amount, room)
            if (moved <= 0) {
                mes("You don't have enough inventory space.")
                return
            }
            val taken = take(obj, moved)
            invAdd(inv, obj, count = taken, strict = false)
            return
        }
    }

    private suspend fun ProtectedAccess.askAmount(max: Int): Int {
        val choice =
            choice4(
                "1", 1,
                "5", 5,
                "All ($max)", max,
                "Other amount", 0,
                title = "How many?",
            )
        val amount = if (choice == 0) countDialog() else choice
        return amount.coerceIn(1, max)
    }

    private fun ProtectedAccess.roomFor(obj: String): Int =
        when {
            CoxItems.isStackable(obj) && (inv.count(obj) > 0 || inv.hasFreeSpace()) -> Int.MAX_VALUE
            CoxItems.isStackable(obj) -> 0
            else -> inv.freeSpace()
        }

    private fun ProtectedAccess.heldTypeNames(): List<String> =
        inv.filterNotNull { true }.map { getInvObj(it).internalName }.distinct()

    private fun convertUnits(raid: CoxRaid, tier: Int) {
        val into = CoxStorage.UNITS[tier]
        val old = CoxStorage.UNITS.map { it.asLoc() }.toSet()
        val origin = raid.southWest
        for (level in 0..LEVELS) {
            for (zx in 0 until ZONES) {
                for (zz in 0 until ZONES) {
                    val corner = CoordGrid(origin.x + zx * ZONE, origin.z + zz * ZONE, level)
                    for (loc in locRepo.findAll(ZoneKey.from(corner)).toList()) {
                        if (loc.id in old) locRepo.add(loc.coords, into, Int.MAX_VALUE, loc.angle, loc.shape)
                    }
                }
            }
        }
    }

    private fun String.asLoc(): Int =
        dev.openrune.rscm.RSCM.run { this@asLoc.asRSCM(dev.openrune.rscm.RSCMType.LOC) }

    private enum class Action {
        DepositAll,
        Withdraw,
        WithdrawAll,
        Capacity,
        Close,
    }

    private companion object {
        const val LOBBY_UNIT = "loc.raids_storage_lobby"
        val HAMMERS = listOf("obj.hammer", "obj.dragon_warhammer", "obj.elder_maul")
        const val BUILD_SEQ = "seq.human_hammer_hit"
        const val BUILD_DELAY = 4
        const val XP_PER_TIER = 150.0
        const val NEXT = -2
        const val PAGE = 4
        const val LEVELS = 3
        const val ZONE = 8
        const val ZONES = CoxRaid.REGION_LENGTH / ZONE
    }
}
