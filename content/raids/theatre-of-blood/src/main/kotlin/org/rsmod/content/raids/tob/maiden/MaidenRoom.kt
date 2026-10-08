package org.rsmod.content.raids.tob.maiden

import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.boss.TobBossRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class MaidenRoom(
    raid: TobRaid,
    services: CoxRoomServices,
    onCleared: () -> Unit,
) : TobBossRoom(raid, TobRoom.Maiden, services, onCleared) {
    private class Crab(val npc: Npc, var frozenUntil: Int = -1)

    private class Trail(val tile: CoordGrid, val until: Int)

    private class Splat(val tile: CoordGrid, val landsAt: Int)

    private class Spawn(val npc: Npc, val until: Int, var trailAt: CoordGrid? = null)

    private var maiden: Npc? = null
    private var ticks = 0
    private var stage = 0
    private var leaked = 0
    private val crabs = mutableListOf<Crab>()
    private val spawns = mutableListOf<Spawn>()
    private val trails = mutableListOf<Trail>()
    private val splats = mutableListOf<Splat>()

    override fun begin() {
        val hp = TobScaling.hitpoints(BASE_HP, teamSize, mode)
        maiden = spawn("npc.tob_maiden_100$suffix", SPAWN, hp, stats = MAIDEN_STAT)
        maiden?.anim("seq.maiden_spawn")
        tell("The Maiden of Sugadinti rises.")
    }

    override fun tick() {
        val boss = maiden ?: return
        ticks++
        if (ticks >= MaidenRules.FIRST_ATTACK && (ticks - MaidenRules.FIRST_ATTACK) % MaidenRules.ATTACK_RATE == 0) {
            tornado(boss)
        }
        if (ticks >= MaidenRules.FIRST_ATTACK + MaidenRules.SPLAT_OFFSET &&
            (ticks - MaidenRules.FIRST_ATTACK - MaidenRules.SPLAT_OFFSET) % MaidenRules.ATTACK_RATE == 0
        ) {
            throwSplats(boss)
        }
        landSplats(boss)
        checkStages(boss)
        tickCrabs(boss)
        tickSpawns()
        tickTrails(boss)
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc === maiden) {
            for (crab in crabs.toList()) remove(crab.npc)
            for (spawn in spawns.toList()) remove(spawn.npc)
            crabs.clear()
            spawns.clear()
            trails.clear()
            splats.clear()
            tell("<col=ef1020>The Maiden of Sugadinti has been defeated!</col>")
            finish()
            return
        }
        crabs.removeAll { it.npc === npc }
        spawns.removeAll { it.npc === npc }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === maiden) maiden = null
        crabs.removeAll { it.npc === npc }
        spawns.removeAll { it.npc === npc }
    }

    fun isCrab(npc: Npc): Boolean = crabs.any { it.npc === npc }

    fun freeze(npc: Npc) {
        val crab = crabs.firstOrNull { it.npc === npc } ?: return
        crab.frozenUntil = clock + MaidenRules.FREEZE_TICKS
        npc.abortRoute()
    }

    private fun tornado(boss: Npc) {
        val target = nearestPlayer(centre(boss)) ?: return
        boss.anim("seq.maiden_attack_special")
        val damage = services.random.of(0, MaidenRules.maxHit(leaked, mode))
        target.queueHit(2, HitType.Magic, damage, services.boss.playerHitModifier)
        if (damage > 0 && services.random.of(2) == 0) {
            target.statSub("stat.magic", (damage + 1) / 5, 0)
        }
    }

    private fun throwSplats(boss: Npc) {
        val players = playersInRoom()
        if (players.isEmpty()) return
        boss.anim("seq.maiden_attack_blood")
        val furthest = players.maxBy { it.coords.chebyshevDistance(centre(boss)) }
        for (target in players + listOf(furthest, furthest)) {
            val tile = target.coords
            services.lob("spotanim.maiden_blood_proj", centre(boss), tile) {}
            splats += Splat(tile, clock + MaidenRules.SPLAT_DELAY)
        }
    }

    private fun landSplats(boss: Npc) {
        val due = splats.filter { it.landsAt <= clock }
        if (due.isEmpty()) return
        splats.removeAll(due.toSet())
        var anyoneHit = false
        for (splat in due) {
            services.spotanimAt("spotanim.tob_bloat_blood_splat", splat.tile)
            for (player in playersInRoom().filter { it.coords == splat.tile }) {
                val damage = TobScaling.damage(services.random.of(1, MaidenRules.SPLAT_MAX), mode)
                hurt(player, damage)
                heal(boss, damage)
                player.statSub("stat.prayer", damage, 0)
                anyoneHit = true
            }
        }
        if (spawns.size < MaidenRules.MAX_BLOOD_SPAWNS &&
            services.random.of(100) < MaidenRules.bloodSpawnChance(anyoneHit)
        ) {
            spawnBloodSpawn(due.random().tile)
        }
    }

    private fun spawnBloodSpawn(near: CoordGrid) {
        val npc = spawnAtWorld("npc.maiden_blood_slug$suffix", near, BLOOD_SPAWN_HP)
        spawns += Spawn(npc, clock + MaidenRules.BLOOD_SPAWN_TICKS)
    }

    private fun spawnAtWorld(type: String, tile: CoordGrid, hp: Int): Npc {
        val source = room.toSource(raid.southWest, tile)
        return spawn(type, source, hp)
    }

    private fun checkStages(boss: Npc) {
        val percent = MaidenRules.hpPercent(boss.hitpoints, boss.baseHitpointsLvl)
        val next = MaidenRules.nextStage(percent, stage) ?: return
        stage = next + 1
        transmog(boss, "npc.tob_maiden_${MaidenRules.THRESHOLDS[next]}$suffix")
        spawnCrabs()
    }

    private fun spawnCrabs() {
        val count = MaidenRules.crabCount(playersInRoom().size.coerceAtLeast(1), mode)
        val points = CRAB_SPAWNS.shuffled(java.util.Random(services.random.of(Int.MAX_VALUE).toLong()))
        for (index in 0 until count) {
            val npc = spawn("npc.maiden_elemental$suffix", points[index % points.size], CRAB_HP, stats = CRAB_STAT)
            crabs += Crab(npc)
        }
        tell("The Maiden summons Nylocas Matomenos!")
    }

    private fun tickCrabs(boss: Npc) {
        for (crab in crabs.toList()) {
            if (crab.frozenUntil > clock) continue
            val target = approach(boss, crab.npc.coords)
            if (reached(boss, crab.npc.coords)) {
                heal(boss, MaidenRules.crabHeal(crab.npc.hitpoints))
                leaked++
                crabs.remove(crab)
                remove(crab.npc)
            } else if (clock % 2 == 0) {
                crab.npc.walk(target)
            }
        }
    }

    private fun tickSpawns() {
        for (spawn in spawns.toList()) {
            if (spawn.until <= clock) {
                spawns.remove(spawn)
                remove(spawn.npc)
                continue
            }
            val here = spawn.npc.coords
            if (spawn.trailAt != here) {
                spawn.trailAt = here
                trails += Trail(here, clock + MaidenRules.TRAIL_TICKS)
                services.spotanimAt("spotanim.tob_bloat_blood_splat", here)
            }
            if (clock % 2 == 0) {
                nearestPlayer(here)?.let { spawn.npc.walk(it.coords) }
            }
        }
    }

    private fun tickTrails(boss: Npc) {
        trails.removeAll { it.until <= clock }
        if (trails.isEmpty()) return
        for (player in playersInRoom()) {
            if (trails.any { it.tile == player.coords }) {
                val damage = TobScaling.damage(services.random.of(1, MaidenRules.TRAIL_MAX), mode)
                hurt(player, damage)
                heal(boss, damage)
            }
        }
    }

    private fun centre(boss: Npc): CoordGrid = boss.coords.translate(MAIDEN_SIZE / 2, MAIDEN_SIZE / 2)

    private fun reached(boss: Npc, from: CoordGrid): Boolean {
        val bx = boss.coords.x
        val bz = boss.coords.z
        val dx = maxOf(bx - from.x, 0, from.x - (bx + MAIDEN_SIZE - 1))
        val dz = maxOf(bz - from.z, 0, from.z - (bz + MAIDEN_SIZE - 1))
        return maxOf(dx, dz) <= 2
    }

    private fun approach(boss: Npc, from: CoordGrid): CoordGrid =
        CoordGrid(
            boss.coords.x + MAIDEN_SIZE,
            from.z.coerceIn(boss.coords.z, boss.coords.z + MAIDEN_SIZE - 1),
            from.level,
        )

    private companion object {
        const val BASE_HP = 3500
        const val MAIDEN_STAT = 140
        const val MAIDEN_SIZE = 6
        const val CRAB_HP = 100
        const val CRAB_STAT = 100
        const val BLOOD_SPAWN_HP = 120
        val SPAWN = CoordGrid(3162, 4444, 0)
        val CRAB_SPAWNS =
            listOf(3170, 3173, 3176, 3179, 3182).flatMap {
                listOf(CoordGrid(it, 4456, 0), CoordGrid(it, 4437, 0))
            }
    }
}
