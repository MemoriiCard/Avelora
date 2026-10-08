package org.rsmod.content.raids.cox.room

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

/**
 * A relief keeps releasing glowing orbs that drift between the jewelled crabs and the room's four
 * crystals. Players recolour crabs by hitting them (melee red, ranged green, magic blue); an orb
 * that touches a coloured crab takes its colour, and a crystal only lights up for the orb colour it
 * wants. Lighting all four crystals clears the room.
 */
class CrabsRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private val crabs = mutableListOf<Npc>()
    private val crabColours = mutableMapOf<Npc, Tint>()
    private val crystals = mutableListOf<Crystal>()
    private var source: CoordGrid = CoordGrid.NULL
    private var orb: Npc? = null
    private var orbColour = Tint.White
    private var target: Target? = null
    private var respawnAt = 0

    override fun spawn() {
        source = findLocs(RELIEF).firstOrNull()?.coords?.translate(0, 1) ?: local(CENTRE, CENTRE)
        for ((name, wants) in CRYSTALS) {
            for (loc in findLocs(name)) crystals += Crystal(loc, wants)
        }
        val count = crabCount(raid.scaling.partySize)
        repeat(count) { index ->
            val x = CRAB_X + (index % CRAB_COLUMNS) * CRAB_GAP
            val z = CRAB_Z + (index / CRAB_COLUMNS) * CRAB_GAP
            crabs += spawnNpc(Tint.None.crab, x, z, STATS, required = false)
        }
    }

    override fun onEngage(first: Player) {
        respawnAt = services.cycle
    }

    override fun onTick() {
        val cycle = services.cycle
        for ((crab, tint) in crabColours.toMap()) {
            if (cycle >= tint.expiresAt(crabExpiry[crab] ?: 0)) uncolour(crab)
        }
        val current = orb
        if (current == null || !current.isSlotAssigned) {
            if (cycle >= respawnAt) releaseOrb()
            return
        }
        val goal = target?.coords(this) ?: run {
            pickTarget()
            return
        }
        if (current.coords.chebyshevDistance(goal) > 1) {
            step(current, goal)
            return
        }
        arrive()
    }

    fun colourCrab(player: Player, crab: Npc, tint: Tint) {
        if (!crabs.contains(crab) || tint == Tint.White || tint == Tint.None) return
        if (crabColours[crab] == tint) return
        crabColours[crab] = tint
        crabExpiry[crab] = services.cycle
        transmog(crab, tint.crab)
        award(player, POINTS_PER_COLOUR)
    }

    fun isCrab(npc: Npc): Boolean = crabs.contains(npc)

    private val crabExpiry = mutableMapOf<Npc, Int>()

    private fun uncolour(crab: Npc) {
        crabColours.remove(crab)
        crabExpiry.remove(crab)
        if (crab.isSlotAssigned) transmog(crab, Tint.None.crab)
    }

    private fun releaseOrb() {
        val npc = spawnAt(Tint.White.orb, standTile(source, 1), stats = null, required = false)
        npc.movementLocked = true
        npc.ignoreCombatInteractions = true
        npc.hideAllOps()
        orb = npc
        orbColour = Tint.White
        target = null
    }

    private fun pickTarget() {
        val options = buildList<Target> {
            crabs.filter { it.isSlotAssigned }.forEach { add(Target.CrabTarget(it)) }
            crystals.filter { !it.lit }.forEach { add(Target.CrystalTarget(it)) }
        }
        target = options.randomOrNull()
    }

    private fun step(npc: Npc, goal: CoordGrid) {
        val dx = (goal.x - npc.coords.x).coerceIn(-1, 1)
        val dz = (goal.z - npc.coords.z).coerceIn(-1, 1)
        PathingEntityCommon.telejump(npc, services.collision, npc.coords.translate(dx, dz))
    }

    private fun arrive() {
        val reached = target ?: return
        target = null
        when (reached) {
            is Target.CrabTarget -> touchCrab(reached.crab)
            is Target.CrystalTarget -> touchCrystal(reached.crystal)
        }
    }

    private fun touchCrab(crab: Npc) {
        val tint = crabColours[crab] ?: return
        val current = orb ?: return
        if (orbColour == Tint.White) {
            orbColour = tint
            transmog(current, tint.orb)
        } else if (orbColour != tint) {
            destroyOrb()
        }
    }

    private fun touchCrystal(crystal: Crystal) {
        if (orbColour == crystal.wants) {
            crystal.lit = true
            val loc = crystal.loc
            services.boss.locRepo.add(loc.coords, WHITE_CRYSTAL, Int.MAX_VALUE, loc.angle, loc.shape)
            for (player in playersInRoom()) player.mes("A crystal blazes white.")
            if (crystals.all { it.lit }) clear()
        }
        destroyOrb()
    }

    private fun destroyOrb() {
        orb?.takeIf { it.isSlotAssigned }?.let { services.npcRepo.del(it, Int.MAX_VALUE) }
        orb = null
        respawnAt = services.cycle + ORB_DELAY
    }

    enum class Tint(
        val crab: String,
        val orb: String,
        private val lifetime: Int = COLOUR_TICKS,
    ) {
        None("npc.raids_lasercrabs_crab_grey", "npc.raids_lasercrabs_energy_white"),
        White("npc.raids_lasercrabs_crab_grey", "npc.raids_lasercrabs_energy_white"),
        Red("npc.raids_lasercrabs_crab_red", "npc.raids_lasercrabs_energy_red"),
        Green("npc.raids_lasercrabs_crab_green", "npc.raids_lasercrabs_energy_green"),
        Blue("npc.raids_lasercrabs_crab_blue", "npc.raids_lasercrabs_energy_blue");

        fun expiresAt(from: Int): Int = from + lifetime
    }

    private class Crystal(val loc: LocInfo, val wants: Tint) {
        var lit = false
    }

    private sealed interface Target {
        fun coords(room: CrabsRoom): CoordGrid?

        class CrabTarget(val crab: Npc) : Target {
            override fun coords(room: CrabsRoom): CoordGrid? =
                crab.takeIf { it.isSlotAssigned }?.coords
        }

        class CrystalTarget(val crystal: Crystal) : Target {
            override fun coords(room: CrabsRoom): CoordGrid = crystal.loc.coords
        }
    }

    companion object {
        const val RELIEF = "loc.raids_lasercrabs_xeric_relief"
        const val HAMMER = "obj.hammer"
        private const val WHITE_CRYSTAL = "loc.raids_lasercrabs_smallcrystal_white"
        private val CRYSTALS =
            listOf(
                "loc.raids_lasercrabs_smallcrystal_black" to Tint.White,
                "loc.raids_lasercrabs_smallcrystal_yellow" to Tint.Blue,
                "loc.raids_lasercrabs_smallcrystal_cyan" to Tint.Red,
                "loc.raids_lasercrabs_smallcrystal_magenta" to Tint.Green,
            )

        val CRAB_TYPES =
            listOf(Tint.None, Tint.Red, Tint.Green, Tint.Blue).map { it.crab }.distinct()

        val STATS = CoxNpcStats(hitpoints = 9999, attack = 100, strength = 100, defence = 1, single = true)

        fun crabCount(partySize: Int): Int = (3 + partySize / 6).coerceAtMost(6)

        private const val CENTRE = 15
        private const val CRAB_X = 12
        private const val CRAB_Z = 12
        private const val CRAB_GAP = 3
        private const val CRAB_COLUMNS = 3
        private const val COLOUR_TICKS = 8
        private const val ORB_DELAY = 3
        private const val POINTS_PER_COLOUR = 20
    }
}
