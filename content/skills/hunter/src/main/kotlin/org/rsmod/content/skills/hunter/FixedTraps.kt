package org.rsmod.content.skills.hunter

import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

/**
 * A trap built on a loc that already exists in the world (a deadfall boulder or a young tree).
 * [idleLoc] is what the site looks like with nothing set and what it reverts to.
 */
class FixedSite(
    val idleLoc: String,
    val setLoc: String,
    val failingLoc: String,
    val noun: String,
    val maxTraps: (Int) -> Int,
)

class FixedCatch(
    val npc: String?,
    val level: Int,
    val xp: Double,
    val trappingLoc: String,
    val fullLoc: String,
    val loot: List<Loot>,
    val catchLow: Int,
    val catchHigh: Int,
    val name: String,
)

enum class FixedState {
    Set,
    Catching,
    Failing,
    Caught,
}

class FixedTrap(
    val owner: Player,
    val site: FixedSite,
    val coords: CoordGrid,
    var loc: LocInfo,
    var state: FixedState,
    var expiresAt: Int,
    val returned: List<String>,
    var prey: FixedCatch? = null,
)

class FixedTraps {
    private val byTile = HashMap<CoordGrid, FixedTrap>()

    operator fun get(coords: CoordGrid): FixedTrap? = byTile[coords]

    fun add(trap: FixedTrap) {
        byTile[trap.coords] = trap
    }

    fun remove(trap: FixedTrap) {
        byTile.remove(trap.coords, trap)
    }

    fun ownedBy(player: Player, site: FixedSite): List<FixedTrap> =
        byTile.values.filter { it.owner === player && it.site === site }
}

object FixedSites {
    val deadfall =
        FixedSite(
            idleLoc = "loc.hunting_deadfall_boulder",
            setLoc = "loc.hunting_deadfall_trap",
            failingLoc = "loc.hunting_deadfall_failing",
            noun = "deadfall",
            maxTraps = { 2 },
        )

    val deadfallPrey: List<FixedCatch> =
        listOf(
            deadfallPrey(
                "npc.huntingbeast_claws",
                23,
                102.4,
                "claw",
                listOf(Loot("obj.bones", 1), Loot("obj.huntingbeast_wild_meat", 1), Loot("obj.huntingbeast_claws", 1)),
                75,
                235,
                "wild kebbit",
            ),
            deadfallPrey(
                "npc.huntingbeast_barbedtail",
                33,
                134.4,
                "barbed",
                listOf(Loot("obj.bones", 1), Loot("obj.huntingbeast_barbed_meat", 1), Loot("obj.hunting_barbed_harpoon", 1)),
                65,
                225,
                "barb-tailed kebbit",
            ),
            deadfallPrey(
                "npc.huntingbeast_spiky",
                37,
                147.2,
                "spike",
                listOf(Loot("obj.bones", 1), Loot("obj.huntingbeast_spike", 1)),
                60,
                220,
                "prickly kebbit",
            ),
            deadfallPrey(
                "npc.huntingbeast_sabreteeth",
                51,
                160.0,
                "sabre",
                listOf(Loot("obj.bones", 1), Loot("obj.huntingbeast_sabreteeth", 1)),
                50,
                210,
                "sabre-toothed kebbit",
            ),
        )

    private fun deadfallPrey(
        npc: String,
        level: Int,
        xp: Double,
        variant: String,
        loot: List<Loot>,
        low: Int,
        high: Int,
        name: String,
    ) =
        FixedCatch(
            npc = npc,
            level = level,
            xp = xp,
            trappingLoc = "loc.hunting_deadfall_trapping_$variant",
            fullLoc = "loc.hunting_deadfall_full_$variant",
            loot = loot,
            catchLow = low,
            catchHigh = high,
            name = name,
        )

    private fun net(variant: String) =
        FixedSite(
            idleLoc = "loc.hunting_sapling_up_$variant",
            setLoc = "loc.hunting_sapling_set_$variant",
            failingLoc = "loc.hunting_sapling_failing_$variant",
            noun = "net trap",
            maxTraps = HunterCreatures::maxTraps,
        )

    private fun salamander(variant: String, level: Int, xp: Double, loot: String, low: Int, high: Int, name: String) =
        FixedCatch(
            npc = null,
            level = level,
            xp = xp,
            trappingLoc = "loc.hunting_sapling_catching_$variant",
            fullLoc = "loc.hunting_sapling_full_$variant",
            loot = listOf(Loot(loot, 1)),
            catchLow = low,
            catchHigh = high,
            name = name,
        )

    val nets: List<Pair<FixedSite, FixedCatch>> =
        listOf(
            net("swamp") to salamander("green", 29, 152.0, "obj.green_salamander", 90, 245, "swamp lizard"),
            net("orange") to salamander("orange", 47, 224.0, "obj.orange_salamander", 75, 235, "orange salamander"),
            net("red") to salamander("red", 59, 272.0, "obj.red_salamander", 60, 225, "red salamander"),
            net("black") to salamander("black", 67, 319.2, "obj.black_salamander", 50, 215, "black salamander"),
            net("mountain") to salamander("mountain", 79, 344.0, "obj.mountain_salamander", 40, 205, "tecu salamander"),
        )

    val all: List<FixedSite> = listOf(deadfall) + nets.map { it.first }
}
