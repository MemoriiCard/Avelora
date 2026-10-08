package org.rsmod.content.raids.cox.room

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

/**
 * The ice demon sits frozen in the middle of the room. Players chop kindling from the saplings,
 * light the four braziers and keep them fed until the heat thaws the demon. Icefiends stand by the
 * braziers and keep putting them out. Once the demon thaws, the fiends die and the fight starts.
 */
class IceDemonRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private val braziers = mutableListOf<Brazier>()
    private val fiends = mutableMapOf<Npc, Brazier>()
    private lateinit var frozen: Npc
    private var demon: Npc? = null
    private var heat = 0
    var heatNeeded = 0
        private set

    val thawed: Boolean
        get() = demon != null

    override fun spawn() {
        frozen = spawnNpc(FROZEN, CENTRE, CENTRE, stats = null, required = false)
        frozen.movementLocked = true
        frozen.ignoreCombatInteractions = true
        frozen.hideAllOps()
        heatNeeded = heatNeeded(raid.scaling.partySize)

        for (loc in findLocs(BRAZIER_UNLIT)) braziers += Brazier(loc)
        val fiendCount = minOf(raid.scaling.partySize, braziers.size)
        for (brazier in braziers.shuffled().take(fiendCount)) {
            val fiend = spawnAt(FIEND, standTile(brazier.loc.coords.translate(1, 0), 1), stats = null, required = false)
            fiend.movementLocked = true
            fiend.ignoreCombatInteractions = true
            fiends[fiend] = brazier
        }
    }

    override fun onTick() {
        if (thawed) return
        for ((fiend, brazier) in fiends) {
            if (!fiend.isSlotAssigned || !brazier.lit) continue
            if (services.cycle % FIEND_RATE != brazier.phase) continue
            if (services.random.of(1, FIEND_CHANCE) != 1) continue
            fiend.anim(FIEND_SEQ)
            brazier.fuel--
            if (brazier.fuel <= 0) extinguish(brazier)
        }
    }

    override fun onKilled(npc: Npc, hero: Player, dropCoords: CoordGrid) {
        if (npc !== demon) return
        val n = raid.scaling.partySize
        rewardAll("obj.raids_stinkhorn_mushroom", 2 * n + 5)
        rewardAll("obj.raids_endarkened_juice", 2 * n + 5)
        rewardAll("obj.raids_cicely", n + 4)
    }

    fun brazierAt(coords: CoordGrid): Brazier? = braziers.firstOrNull { it.loc.coords == coords }

    /** Lights [brazier] with one kindling. */
    fun light(player: Player, brazier: Brazier) {
        if (brazier.lit || thawed) return
        brazier.lit = true
        brazier.fuel = 1
        swap(brazier, BRAZIER_LIT)
        award(player, POINTS_PER_LIGHT)
        addHeat(1)
    }

    /** Burns [kindling] pieces in a lit brazier. */
    fun fuel(player: Player, brazier: Brazier, kindling: Int) {
        if (!brazier.lit || thawed || kindling <= 0) return
        brazier.fuel += kindling
        award(player, kindling * POINTS_PER_KINDLING)
        addHeat(kindling)
    }

    fun saplingDepletes(): Boolean =
        services.random.of(1, SAPLING_DEPLETE * maxOf(SAPLING_MIN_PARTY, 2 * raid.scaling.partySize)) == 1

    private fun addHeat(amount: Int) {
        heat += amount
        if (heat >= heatNeeded) thaw()
    }

    private fun extinguish(brazier: Brazier) {
        brazier.lit = false
        brazier.fuel = 0
        swap(brazier, BRAZIER_UNLIT)
    }

    private fun thaw() {
        if (thawed) return
        for (fiend in fiends.keys) {
            if (fiend.isSlotAssigned) services.npcRepo.del(fiend, Int.MAX_VALUE)
        }
        fiends.clear()
        for (brazier in braziers) if (brazier.lit) extinguish(brazier)
        val tile = frozen.coords
        services.npcRepo.del(frozen, Int.MAX_VALUE)
        val awake = spawnAt(DEMON, tile, STATS, ranged = true)
        demon = awake
        for (player in playersInRoom()) player.mes("The ice demon thaws out!")
        nearestPlayer(awake)?.let { engage(awake, it) }
    }

    private fun swap(brazier: Brazier, into: String) {
        val loc = brazier.loc
        services.boss.locRepo.add(loc.coords, into, Int.MAX_VALUE, loc.angle, loc.shape)
    }

    inner class Brazier(val loc: LocInfo) {
        var lit = false
        var fuel = 0
        val phase = braziers.size % FIEND_RATE
    }

    companion object {
        const val FROZEN = "npc.raids_icedemon_noncombat"
        const val DEMON = "npc.raids_icedemon_combat"
        const val FIEND = "npc.raids_icefiend"
        const val BRAZIER_UNLIT = "loc.raids_icedemon_brazier_unlit"
        const val BRAZIER_LIT = "loc.raids_icedemon_brazier_lit"
        const val SAPLING = "loc.raids_woodsource_roots"
        const val SAPLING_STUMP = "loc.raids_woodsource_roots_depleted"
        const val KINDLING = "obj.raids_wood"

        val STATS =
            CoxNpcStats(
                hitpoints = 140,
                defence = 160,
                ranged = 390,
                magic = 390,
            )

        fun heatNeeded(partySize: Int): Int = BASE_HEAT + HEAT_PER_EXTRA_PLAYER * (partySize - 1)

        private const val CENTRE = 15
        private const val FIEND_SEQ = "seq.pyrefiend_attack"
        private const val FIEND_RATE = 4
        private const val FIEND_CHANCE = 6
        private const val POINTS_PER_LIGHT = 32
        private const val POINTS_PER_KINDLING = 36
        private const val BASE_HEAT = 40
        private const val HEAT_PER_EXTRA_PLAYER = 16
        private const val SAPLING_DEPLETE = 5
        private const val SAPLING_MIN_PARTY = 3
    }
}
