package org.rsmod.content.raids.cox.room

import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.clearInteractionRoute
import org.rsmod.api.npc.heal
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.route.walkTo
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

/**
 * Vasa walks to one of the four glowing crystals and drains it, banking 1% of his health every two
 * ticks. Destroying the crystal first wastes the bank. Between crystals he teleports the party to
 * him and explodes, splitting the damage between everyone left standing beside him.
 */
class VasaRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private lateinit var vasa: Npc
    private var centre: CoordGrid = CoordGrid.NULL
    private val crystals = mutableListOf<Npc>()
    private var target: Npc? = null
    private var state = State.Dormant
    private var stateTicks = 0
    private var banked = 0
    private var destroyedCrystals = 0
    private val teleported = mutableListOf<Player>()

    override fun spawn() {
        vasa = spawnNpc(VASA, CENTRE_X, CENTRE_Z, STATS, ranged = true)
        centre = vasa.coords
        transmog(vasa, DORMANT)
        vasa.hideAllOps()
        vasa.ignoreCombatInteractions = true
        for ((x, z) in CRYSTAL_SPOTS) {
            crystals += spawnNpc(CRYSTAL, x, z, CRYSTAL_STATS, required = false)
        }
    }

    override fun onTick() {
        if (!vasa.isSlotAssigned || vasa.hitpoints <= 0) return
        stateTicks++
        when (state) {
            State.Dormant -> if (playersInRoom().any { it.coords.chebyshevDistance(centre) <= WAKE_RANGE }) wake()
            State.Walking -> Unit
            State.Draining -> drainTick()
            State.Special -> specialTick()
            State.Fighting -> if (stateTicks >= FIGHT_TICKS) walkTo(centre) { startSpecial() }
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (crystals.removeAll { it === npc }) {
            destroyedCrystals++
            if (npc === target && state == State.Draining) {
                banked = 0
                for (player in playersInRoom()) player.mes("The crystal shatters, and Vasa's stolen energy is lost!")
                finishDrain()
            }
        }
    }

    override fun onCleared() {
        for (crystal in crystals) {
            if (crystal.isSlotAssigned) services.npcRepo.del(crystal, Int.MAX_VALUE)
        }
    }

    private fun wake() {
        vasa.resetTransmog()
        vasa.showAllOps()
        vasa.anim("seq.vasa_spawn")
        vasa.ignoreCombatInteractions = false
        goToCrystal()
    }

    private fun goToCrystal() {
        val next = crystals.filter { it.isSlotAssigned && it.hitpoints > 0 }.randomOrNull()
        if (next == null) {
            state = State.Fighting
            stateTicks = 0
            vasa.movementLocked = false
            nearestPlayer(vasa)?.let { engage(vasa, it) }
            return
        }
        target = next
        walkTo(standTile(next.coords.translate(0, -3), vasa.type.size)) { startDrain() }
    }

    private fun startDrain() {
        state = State.Draining
        stateTicks = 0
        banked = 0
        transmog(vasa, HEALING)
        services.boss.encounter(vasa).invulnerable = true
        vasa.movementLocked = true
        vasa.ignoreCombatInteractions = false
        nearestPlayer(vasa)?.let { engage(vasa, it) }
    }

    private fun drainTick() {
        if (stateTicks % DRAIN_PULSE == 0) {
            banked += maxOf(1, vasa.baseHitpointsLvl / 100)
        }
        val duration = (DRAIN_TICKS - destroyedCrystals * DRAIN_SPEEDUP).coerceAtLeast(MIN_DRAIN_TICKS)
        if (stateTicks >= duration) {
            vasa.heal(banked, showHitsplat = true)
            vasa.anim("seq.vasa_healed")
            banked = 0
            finishDrain()
        }
    }

    private fun finishDrain() {
        services.boss.encounter(vasa).invulnerable = false
        vasa.resetTransmog()
        vasa.movementLocked = false
        target = null
        walkTo(centre) { startSpecial() }
    }

    private fun startSpecial() {
        state = State.Special
        stateTicks = 0
        vasa.anim("seq.vasa_stun_spawn")
        teleported.clear()
        val players = playersInRoom().shuffled()
        val besideCount = if (players.size == 1) 1 else (players.size + 1) / 2
        for ((index, player) in players.withIndex()) {
            val tile = if (index < besideCount) besideTile() else wallTile()
            PathingEntityCommon.telejump(player, services.collision, tile)
            if (index < besideCount) teleported += player
        }
    }

    private fun specialTick() {
        if (stateTicks < SPECIAL_FUSE) return
        val beside = playersInRoom().filter { isBeside(it) }
        if (beside.isNotEmpty()) {
            val pool = (beside.sumOf { it.hitpoints } - SPECIAL_REDUCTION).coerceAtLeast(0)
            val share = pool / beside.size
            for (player in beside) {
                val damage = if (player.vars[PROTECT_FROM_MAGIC] > 0) 0 else share
                player.queueHit(vasa, 1, HitType.Typeless, damage, services.boss.playerHitModifier)
            }
        }
        services.spotanimAt(EXPLOSION, vasa.coords.translate(2, 2))
        goToCrystal()
    }

    private fun walkTo(dest: CoordGrid, onArrive: () -> Unit) {
        state = State.Walking
        stateTicks = 0
        vasa.clearInteractionRoute()
        vasa.ignoreCombatInteractions = true
        vasa.walkTo(services.routes, dest) {
            vasa.ignoreCombatInteractions = false
            onArrive()
        }
    }

    private fun besideTile(): CoordGrid {
        val size = vasa.type.size
        val ring = buildList {
            for (d in -1..size) {
                add(vasa.coords.translate(d, -1))
                add(vasa.coords.translate(d, size))
                add(vasa.coords.translate(-1, d))
                add(vasa.coords.translate(size, d))
            }
        }
        return ring.filter { canStand(it, 1) }.randomOrNull() ?: vasa.coords.translate(-1, -1)
    }

    private fun wallTile(): CoordGrid {
        repeat(WALL_TILE_ATTEMPTS) {
            val tile = local(services.random.of(1, 30), services.random.of(1, 30))
            if (tile.chebyshevDistance(vasa.coords) >= WALL_DISTANCE && canStand(tile, 1)) return tile
        }
        return besideTile()
    }

    private fun isBeside(player: Player): Boolean {
        val size = vasa.type.size
        val dx = player.coords.x - vasa.coords.x
        val dz = player.coords.z - vasa.coords.z
        return dx in -1..size && dz in -1..size
    }

    private enum class State {
        Dormant,
        Walking,
        Draining,
        Special,
        Fighting,
    }

    companion object {
        const val VASA = "npc.raids_vasanistirio_walking"
        const val CRYSTAL = "npc.raids_vasanistirio_crystal"
        private const val DORMANT = "npc.raids_vasanistirio_dormant"
        private const val HEALING = "npc.raids_vasanistirio_healing"
        val TYPES = listOf(VASA, DORMANT, HEALING)

        val STATS =
            CoxNpcStats(hitpoints = 300, defence = 175, ranged = 230, magic = 230)
        val CRYSTAL_STATS =
            CoxNpcStats(hitpoints = 120, defence = 100, magic = 100, defensiveMagic = true, crystal = true)

        private const val CENTRE_X = 15
        private const val CENTRE_Z = 15
        private val CRYSTAL_SPOTS = listOf(5 to 5, 26 to 5, 5 to 26, 26 to 26)
        private const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"
        private const val EXPLOSION = "spotanim.raids_vasanistirio_magic_impact"
        private const val WAKE_RANGE = 10
        private const val DRAIN_PULSE = 2
        private const val DRAIN_TICKS = 66
        private const val DRAIN_SPEEDUP = 8
        private const val MIN_DRAIN_TICKS = 30
        private const val FIGHT_TICKS = 30
        private const val SPECIAL_FUSE = 5
        private const val SPECIAL_REDUCTION = 5
        private const val WALL_DISTANCE = 8
        private const val WALL_TILE_ATTEMPTS = 20
    }
}
