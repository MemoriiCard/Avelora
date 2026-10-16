package org.rsmod.content.raids.cox.room

import org.rsmod.api.npc.heal
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.cox.layout.CoxRoom
import org.rsmod.content.raids.cox.raid.CoxRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo

/**
 * Vespula hovers over her abyssal portal and stings the lux grubs around it. A grub stung to death
 * hatches a vespine soldier that flies to the portal and heals it until it explodes. Players feed
 * the grubs medivaemia blossoms to keep them alive while they break the portal, which drains their
 * prayer as it is struck. Destroying the portal clears the room.
 */
class VespulaRoom(raid: CoxRaid, room: CoxRoom, services: CoxRoomServices) :
    CoxRoomController(raid, room, services) {
    private lateinit var portal: Npc
    private lateinit var vespula: Npc
    private val grubs = mutableListOf<Grub>()
    private val soldiers = mutableMapOf<Npc, Int>()
    private var nextSting = 0
    private var lastPortalHit = Int.MIN_VALUE
    private var landedAt = -1
    private var enraged = false

    override fun spawn() {
        val (px, pz) = pick(17 to 21, 23 to 11, 11 to 21)
        val portalTile =
            findLocs(PORTAL_PILLARS).firstOrNull()?.coords
                ?: standTile(local(px, pz), PORTAL_SIZE)
        portal = spawnAt(PORTAL, portalTile, PORTAL_STATS)
        portal.movementLocked = true

        val centre = portalTile.translate(PORTAL_SIZE / 2, PORTAL_SIZE / 2)
        vespula = spawnAt(VESPULA, standTile(centre.translate(0, -VESPULA_OFFSET), VESPULA_SIZE), STATS, required = false, ranged = true)

        for ((dx, dz) in GRUB_OFFSETS) {
            val tile = standTile(centre.translate(dx, dz), GRUB_SIZE)
            val npc = spawnAt(GRUB, tile, stats = null, required = false)
            npc.movementLocked = true
            npc.ignoreCombatInteractions = true
            grubs += Grub(npc)
        }

        for (herb in findLocs(HERB_EMPTY)) growHerb(herb)
    }

    override fun onEngage(first: Player) {
        nextSting = services.cycle + services.random.of(STING_MIN, STING_MAX)
    }

    override fun onTick() {
        val cycle = services.cycle
        if (vespula.isSlotAssigned && vespula.hitpoints > 0) {
            if (cycle >= nextSting) stingGrub()
            tickLanding()
            if (enraged && cycle % ENRAGED_STING_RATE == 0) stingAroundPortal()
        }
        tickSoldiers()
        if (cycle - lastPortalHit <= DRAIN_WINDOW && cycle % DRAIN_RATE == 0) drainPrayer()
        for (grub in grubs) grub.tickRespawn(cycle)
    }

    override fun onNpcRemoved(npc: Npc) {
        if (cleared) return
        if (soldiers.remove(npc) != null) {
            grubs.firstOrNull { it.soldier === npc }?.soldierGone(services.cycle)
        }
    }

    override fun onCleared() {
        if (portal.isSlotAssigned) portal.anim(PORTAL_CLOSING)
        for (npc in npcs.toList()) {
            if (npc !== portal && npc.isSlotAssigned) services.npcRepo.del(npc, Int.MAX_VALUE)
        }
        soldiers.clear()
    }

    fun onPortalHit() {
        lastPortalHit = services.cycle
        if (!enraged && vespula.isSlotAssigned && landedAt < 0) {
            enraged = true
            transmog(vespula, ENRAGED)
        }
    }

    fun isFlying(npc: Npc): Boolean = npc === vespula && landedAt < 0

    fun grubOf(npc: Npc): Grub? = grubs.firstOrNull { it.npc === npc }

    fun pickBlossom(loc: BoundLocInfo) {
        services.boss.locRepo.add(loc.coords, HERB_EMPTY, Int.MAX_VALUE, loc.angle, loc.shape)
        services.boss.worldQueues.add(HERB_REGROW_TICKS) {
            if (!cleared) services.boss.locRepo.add(loc.coords, HERB, Int.MAX_VALUE, loc.angle, loc.shape)
        }
    }

    private fun growHerb(loc: LocInfo) {
        services.boss.locRepo.add(loc.coords, HERB, Int.MAX_VALUE, loc.angle, loc.shape)
    }

    private fun stingGrub() {
        nextSting = services.cycle + services.random.of(STING_MIN, STING_MAX)
        val target = grubs.filter { it.alive }.randomOrNull() ?: return
        vespula.anim(if (landedAt < 0) "seq.vespula_attack_ranged_flying" else "seq.vespula_attack_ranged")
        services.lob(STING_TRAVEL, vespula.coords, target.npc.coords) {
            if (target.alive) target.damage(services.random.of(STING_DAMAGE_MIN, STING_DAMAGE_MAX))
        }
    }

    private fun stingAroundPortal() {
        val size = portal.type.size
        for (player in playersInRoom()) {
            val dx = player.coords.x - portal.coords.x
            val dz = player.coords.z - portal.coords.z
            if (dx !in -1..size || dz !in -1..size) continue
            val damage = services.random.of(ENRAGED_STING_MIN, ENRAGED_STING_MAX)
            player.queueHit(vespula, 1, HitType.Typeless, damage, services.boss.playerHitModifier)
        }
    }

    private fun tickLanding() {
        val cycle = services.cycle
        if (landedAt < 0) {
            if (vespula.hitpoints * 100 > vespula.baseHitpointsLvl * LAND_PERCENT) return
            landedAt = cycle
            enraged = false
            vespula.anim("seq.vespula_landing")
            transmog(vespula, WALKING)
            return
        }
        if (cycle - landedAt < GROUNDED_TICKS || !portal.isSlotAssigned) return
        landedAt = -1
        vespula.anim("seq.vespula_takeoff")
        vespula.resetTransmog()
        vespula.heal(vespula.baseHitpointsLvl, showHitsplat = false)
    }

    private fun tickSoldiers() {
        val cycle = services.cycle
        for ((soldier, hatched) in soldiers.toList()) {
            if (!soldier.isSlotAssigned) continue
            val age = cycle - hatched
            if (age >= SOLDIER_LIFE_TICKS) {
                soldier.anim("seq.vespine_explode")
                services.spotanimAt("spotanim.raids_vespula_vespine_explode", soldier.coords)
                services.npcRepo.del(soldier, Int.MAX_VALUE)
                continue
            }
            if (age > 0 && age % SOLDIER_HEAL_RATE == 0) {
                services.spotanimAt("spotanim.raids_vespula_vespine_healportal", portal.coords.translate(1, 1))
                portal.heal(portal.baseHitpointsLvl * SOLDIER_HEAL_PERCENT / 100, showHitsplat = true)
                if (vespula.isSlotAssigned) {
                    vespula.heal(vespula.baseHitpointsLvl * SOLDIER_HEAL_PERCENT / 100, showHitsplat = true)
                }
            }
        }
    }

    private fun drainPrayer() {
        for (player in playersInRoom()) {
            if (player.prayerLvl > 0) {
                player.statSub("stat.prayer", constant = PORTAL_DRAIN, percent = 0)
            } else {
                player.queueHit(portal, 1, HitType.Typeless, PORTAL_DRAIN, services.boss.playerHitModifier)
            }
        }
    }

    private fun hatch(grub: Grub) {
        grub.npc.anim("seq.luxgrub_death")
        transmog(grub.npc, GRUB_DEAD)
        services.spotanimAt("spotanim.raids_vespula_vespine_hatch", grub.npc.coords)
        val tile = standTile(grub.npc.coords, SOLDIER_SIZE)
        val soldier = spawnAt(SOLDIER, tile, SOLDIER_STATS, required = false)
        soldier.anim("seq.vespine_hatch")
        soldier.ignoreCombatInteractions = true
        soldiers[soldier] = services.cycle
        grub.soldier = soldier
    }

    inner class Grub(val npc: Npc) {
        private val max = GRUB_HITPOINTS
        var hitpoints = max
            private set

        var soldier: Npc? = null
        private var respawnAt = -1

        val alive: Boolean
            get() = hitpoints > 0

        fun damage(amount: Int) {
            hitpoints = (hitpoints - amount).coerceAtLeast(0)
            refresh()
            if (hitpoints == 0) hatch(this)
        }

        fun feed(amount: Int) {
            if (!alive) return
            hitpoints = (hitpoints + amount).coerceAtMost(max)
            refresh()
        }

        fun soldierGone(cycle: Int) {
            soldier = null
            respawnAt = cycle + GRUB_RESPAWN_TICKS
        }

        fun tickRespawn(cycle: Int) {
            if (respawnAt < 0 || cycle < respawnAt) return
            respawnAt = -1
            hitpoints = max
            refresh()
        }

        private fun refresh() {
            npc.hitpoints = hitpoints.coerceAtLeast(1)
            when {
                hitpoints == 0 -> Unit
                hitpoints * 3 < max -> transmog(npc, GRUB_INFECTED)
                hitpoints * 3 < max * 2 -> transmog(npc, GRUB_SICKLY)
                else -> npc.resetTransmog()
            }
        }
    }

    companion object {
        const val PORTAL = "npc.raids_vespula_portal"
        const val VESPULA = "npc.raids_vespula_flying"
        const val ENRAGED = "npc.raids_vespula_enraged"
        const val WALKING = "npc.raids_vespula_walking"
        const val GRUB = "npc.raids_vespula_caterpillar_healthy"
        const val GRUB_SICKLY = "npc.raids_vespula_caterpillar_sickly"
        const val GRUB_INFECTED = "npc.raids_vespula_caterpillar_infected"
        const val GRUB_DEAD = "npc.raids_vespula_caterpillar_dead"
        const val SOLDIER = "npc.raids_vespula_vespine_flying"
        const val HERB = "loc.raids_vespula_herb"
        const val HERB_EMPTY = "loc.raids_vespula_herb_empty"
        const val BLOSSOM = "obj.raids_vespula_herb"
        private const val PORTAL_PILLARS = "loc.raids_vespula_portal"
        private const val PORTAL_CLOSING = "seq.raids_vespular_portal_closing"
        private const val STING_TRAVEL = "spotanim.raids_vespula_poison"

        val VESPULA_TYPES = listOf(VESPULA, ENRAGED, WALKING)
        val GRUB_TYPES = listOf(GRUB, GRUB_SICKLY, GRUB_INFECTED, GRUB_DEAD)

        val STATS =
            CoxNpcStats(
                hitpoints = 200,
                attack = 150,
                strength = 150,
                defence = 88,
                ranged = 150,
                magic = 88,
                defensiveMagic = true,
            )
        val PORTAL_STATS =
            CoxNpcStats(hitpoints = 250, defence = 176, magic = 1, defensiveMagic = true)
        val SOLDIER_STATS =
            CoxNpcStats(
                hitpoints = 100,
                attack = 150,
                strength = 150,
                defence = 40,
                magic = 40,
                defensiveMagic = true,
                single = true,
            )

        const val PORTAL_BONUS_POINTS = 2
        const val BLOSSOM_HEAL = 35

        private const val PORTAL_SIZE = 4
        private const val VESPULA_SIZE = 5
        private const val VESPULA_OFFSET = 5
        private const val GRUB_SIZE = 3
        private const val SOLDIER_SIZE = 3
        private val GRUB_OFFSETS = listOf(-7 to -7, 7 to -7, -7 to 7, 7 to 7)
        private const val GRUB_HITPOINTS = 100
        private const val GRUB_RESPAWN_TICKS = 10
        private const val STING_MIN = 10
        private const val STING_MAX = 15
        private const val STING_DAMAGE_MIN = 15
        private const val STING_DAMAGE_MAX = 30
        private const val ENRAGED_STING_RATE = 4
        private const val ENRAGED_STING_MIN = 5
        private const val ENRAGED_STING_MAX = 20
        private const val LAND_PERCENT = 20
        private const val GROUNDED_TICKS = 33
        private const val SOLDIER_LIFE_TICKS = 33
        private const val SOLDIER_HEAL_RATE = 4
        private const val SOLDIER_HEAL_PERCENT = 2
        private const val DRAIN_WINDOW = 2
        private const val DRAIN_RATE = 2
        private const val PORTAL_DRAIN = 3
        private const val HERB_REGROW_TICKS = 30
    }
}
