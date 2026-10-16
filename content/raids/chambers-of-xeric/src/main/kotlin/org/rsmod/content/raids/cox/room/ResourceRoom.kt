package org.rsmod.content.raids.cox.room

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.layout.CoxRoomType
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

/**
 * A resource room between the combat rooms: herb patches, a gourd tree, a geyser and a storage
 * unit, plus either fishing spots guarded by a cave snake or a few bats to net. It never blocks
 * the way on. The tier of the food caught here follows the party's average Fishing or Hunter level.
 */
class ResourceRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    override val blocksExit: Boolean = false

    private val patches = mutableMapOf<CoordGrid, Patch>()
    private val respawnAt = mutableListOf<Int>()
    private var snake: Npc? = null

    val batRoom: Boolean = room.type == CoxRoomType.FarmingBats

    val foodTier: Int =
        tierFor(raid.party.members.map { if (batRoom) it.hunterLvl else it.fishingLvl })

    override fun spawn() {
        for (loc in findLocs(PATCH_EMPTY)) patches[loc.coords] = Patch(loc.coords, loc.angle, loc.shape)
        if (batRoom) {
            repeat(batCount(raid.scaling.partySize)) { spawnBat() }
        } else {
            val spot = findLocs(FISHING_SPOT).randomOrNull()
            val near = spot?.coords ?: local(CENTRE, CENTRE)
            snake = spawnAt(SNAKE, standTile(near, SNAKE_SIZE), stats = null, required = false)
            snake?.movementLocked = true
            snake?.ignoreCombatInteractions = true
        }
    }

    override fun onTick() {
        for (patch in patches.values) grow(patch)
        if (batRoom) respawnBats()
        if (!batRoom && services.cycle % SNAKE_RATE == 0) snakeStrikes()
    }

    fun patchAt(coords: CoordGrid): Patch? = patches[coords]

    fun batCaught(bat: Npc) {
        if (!owns(bat)) return
        services.npcRepo.del(bat, Int.MAX_VALUE)
        respawnAt += services.cycle + BAT_RESPAWN
    }

    fun plant(patch: Patch, herb: Herb) {
        patch.herb = herb
        patch.plantedAt = services.cycle
        patch.stage = 0
        show(patch, herb.loc("seed"))
    }

    /** Empties a fully grown [patch] and returns how many herbs it gave. */
    fun harvest(patch: Patch): Int {
        if (!patch.grown) return 0
        wipe(patch)
        return herbYield(raid.scaling.partySize)
    }

    fun wipe(patch: Patch) {
        patch.herb = null
        patch.stage = -1
        show(patch, PATCH_EMPTY)
    }

    private fun grow(patch: Patch) {
        val herb = patch.herb ?: return
        if (patch.grown) return
        val age = services.cycle - patch.plantedAt
        val stage = STAGE_AGES.count { age >= it }
        if (stage == patch.stage) return
        patch.stage = stage
        show(patch, herb.loc(STAGE_NAMES[stage]))
    }

    private fun show(patch: Patch, loc: String) {
        services.boss.locRepo.add(patch.coords, loc, Int.MAX_VALUE, patch.angle, patch.shape)
    }

    private fun spawnBat() {
        val x = services.random.of(BAT_MIN, BAT_MAX)
        val z = services.random.of(BAT_MIN, BAT_MAX)
        spawnAt("npc.raids_bat_$foodTier", standTile(local(x, z), 1), stats = null, required = false)
    }

    private fun respawnBats() {
        if (respawnAt.isEmpty()) return
        val due = respawnAt.count { services.cycle >= it }
        if (due == 0) return
        respawnAt.removeAll { services.cycle >= it }
        repeat(due) { spawnBat() }
    }

    private fun snakeStrikes() {
        val snake = snake ?: return
        for (player in playersInRoom()) {
            if (player.coords.chebyshevDistance(snake.coords) > SNAKE_REACH) continue
            val fish = RAW_FISH.firstOrNull { player.inv.count(it) > 0 }
            if (fish != null && services.random.of(0, 1) == 0) {
                player.invDel(player.inv, fish)
                player.mes("The cave snake snatches a fish from you!")
            } else {
                val damage = services.random.of(SNAKE_MIN, SNAKE_MAX)
                player.queueHit(1, HitType.Typeless, damage, services.boss.playerHitModifier)
                player.mes("The cave snake bites you!")
            }
        }
    }

    inner class Patch(val coords: CoordGrid, val angle: LocAngle, val shape: LocShape) {
        var herb: Herb? = null
        var plantedAt = 0
        var stage = -1

        val grown: Boolean
            get() = stage == STAGE_NAMES.lastIndex
    }

    enum class Herb(
        val key: String,
        val seed: String,
        val item: String,
        val level: Int,
        val plantXp: Double,
        val harvestXp: Double,
    ) {
        Golpar("golpar", "obj.raids_seed_golpar", "obj.raids_golpar", 27, 4.0, 10.0),
        Buchu("buchu", "obj.raids_seed_buchuleaf", "obj.raids_buchuleaf", 39, 6.0, 15.0),
        Noxifer("noxifer", "obj.raids_seed_noxifer", "obj.raids_noxifer", 55, 12.0, 30.0),
        ;

        fun loc(stage: String): String = "loc.raids_patch_${key}_$stage"
    }

    companion object {
        const val PATCH_EMPTY = "loc.raids_patch_empty"
        const val FISHING_SPOT = "loc.raids_floor_water_edge1_fishing"
        const val SNAKE = "npc.raids_fishing_snake"

        val RAW_FISH = (0..6).map { "obj.raids_fish${it}_raw" }
        val FISH_NAMES = listOf("pysk", "suphi", "leckish", "brawk", "mycil", "roqed", "kyren")
        val BAT_NAMES = listOf("guanic", "prael", "giral", "phluxia", "kryket", "murng", "psykk")

        private val STAGE_AGES = listOf(12, 24, 36, 50)
        private val STAGE_NAMES = listOf("seed", "growth1", "growth2", "growth3", "fullygrown")

        fun tierFor(levels: List<Int>): Int =
            if (levels.isEmpty()) 0 else (levels.average().toInt() / LEVELS_PER_TIER).coerceIn(0, 6)

        fun herbYield(partySize: Int): Int = BASE_YIELD + partySize.coerceAtMost(YIELD_PARTY_CAP) / 2

        fun batCount(partySize: Int): Int = (2 + partySize / 6).coerceAtMost(4)

        const val LEVELS_PER_TIER = 15
        private const val BASE_YIELD = 4
        private const val YIELD_PARTY_CAP = 14
        private const val BAT_RESPAWN = 30
        private const val BAT_MIN = 6
        private const val BAT_MAX = 26
        private const val SNAKE_SIZE = 3
        private const val SNAKE_RATE = 8
        private const val SNAKE_REACH = 3
        private const val SNAKE_MIN = 1
        private const val SNAKE_MAX = 4
        private const val CENTRE = 15
    }
}
