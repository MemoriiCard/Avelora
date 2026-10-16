package org.rsmod.content.skills.hunter

enum class TrapKind(
    val obj: String,
    val level: Int,
    val noun: String,
    val setLoc: String,
    val failingLoc: String,
    val brokenLoc: String,
) {
    BirdSnare(
        obj = "obj.hunting_ojibway_bird_snare",
        level = 1,
        noun = "bird snare",
        setLoc = "loc.hunting_ojibway_trap",
        failingLoc = "loc.hunting_ojibway_trap_failing",
        brokenLoc = "loc.hunting_ojibway_trap_broken",
    ),
    BoxTrap(
        obj = "obj.hunting_box_trap",
        level = 27,
        noun = "box trap",
        setLoc = "loc.hunting_boxtrap_empty",
        failingLoc = "loc.hunting_boxtrap_failing",
        brokenLoc = "loc.hunting_boxtrap_failed",
    ),
}

data class Loot(val obj: String, val min: Int, val max: Int = min)

/**
 * A creature caught with a [trap]. [catchLow] and [catchHigh] are the success curve on the 0-255
 * scale used by `statRandom`, at Hunter level 1 and 99 respectively.
 *
 * [trappingLoc] is the loc shown while the creature is being caught; box traps have one per
 * direction, so it holds a `%s` that is filled with the side the creature came from (n/e/s/w).
 */
data class HunterCreature(
    val npc: String,
    val level: Int,
    val xp: Double,
    val trap: TrapKind,
    val trappingLoc: String,
    val fullLoc: String,
    val loot: List<Loot>,
    val catchLow: Int,
    val catchHigh: Int,
)

object HunterCreatures {
    private val birdLoot = listOf(Loot("obj.bones", 1), Loot("obj.spit_raw_bird_meat", 1))

    private fun bird(
        npc: String,
        level: Int,
        xp: Double,
        variant: String,
        feather: String,
        catchLow: Int,
        catchHigh: Int,
    ) =
        HunterCreature(
            npc = npc,
            level = level,
            xp = xp,
            trap = TrapKind.BirdSnare,
            trappingLoc = "loc.hunting_ojibway_trap_trapping_$variant",
            fullLoc = "loc.hunting_ojibway_trap_full_$variant",
            loot = birdLoot + Loot(feather, 5, 10),
            catchLow = catchLow,
            catchHigh = catchHigh,
        )

    private fun boxed(
        npc: String,
        level: Int,
        xp: Double,
        variant: String,
        loot: String,
        catchLow: Int,
        catchHigh: Int,
    ) =
        HunterCreature(
            npc = npc,
            level = level,
            xp = xp,
            trap = TrapKind.BoxTrap,
            trappingLoc = "loc.hunting_boxtrap_trapping_${variant}_%s",
            fullLoc = "loc.hunting_boxtrap_full_$variant",
            loot = listOf(Loot(loot, 1)),
            catchLow = catchLow,
            catchHigh = catchHigh,
        )

    val all: List<HunterCreature> =
        listOf(
            bird("npc.hunting_bird_jungle", 1, 34.0, "jungle", "obj.hunting_jungle_feather", 100, 255),
            bird("npc.hunting_bird_desert", 5, 47.0, "desert", "obj.hunting_desert_feather", 95, 250),
            bird("npc.hunting_bird_woodland", 9, 61.2, "woodland", "obj.hunting_woodland_feather", 90, 245),
            bird("npc.hunting_bird_polar", 11, 64.5, "polar", "obj.hunting_polar_feather", 85, 240),
            bird("npc.multicoloured_bird", 19, 95.2, "coloured", "obj.hunting_stripy_bird_feather", 80, 235),
            boxed("npc.hunting_ferret", 27, 115.2, "ferret", "obj.hunting_ferret", 70, 230),
            boxed("npc.hunting_chinchompa", 53, 198.4, "chinchompa", "obj.chinchompa_captured", 60, 225),
            boxed("npc.hunting_chinchompa_big", 63, 265.0, "chinchompa_big", "obj.chinchompa_big_captured", 50, 215),
            boxed("npc.hunting_chinchompa_black", 73, 315.0, "chinchompa_black", "obj.chinchompa_black", 40, 205),
        )

    /** Most traps a player may have set at once for their Hunter level. */
    fun maxTraps(level: Int): Int =
        when {
            level >= 80 -> 5
            level >= 60 -> 4
            level >= 40 -> 3
            else -> 2
        }
}
