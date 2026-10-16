package org.rsmod.content.raids.tob.nylocas

import org.rsmod.api.player.hit.queueHit
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.boss.TobBossRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class NylocasRoom(
    raid: TobRaid,
    services: CoxRoomServices,
    private val onWipe: () -> Unit,
    onCleared: () -> Unit,
) : TobBossRoom(raid, TobRoom.Nylocas, services, onCleared) {
    private class Mob(
        val npc: Npc,
        val spawn: NylocasSpawn,
        var pillar: Int,
        var nextHit: Int = 0,
    )

    private val pillars = IntArray(NylocasRules.PILLARS.size) { NylocasRules.PILLAR_HP }
    private val mobs = mutableListOf<Mob>()
    private var wave = 0
    private var nextWaveAt = 0
    private var boss: Npc? = null
    private var form: NylocasStyle? = null
    private var formEndsAt = 0
    private var bossSpawnedAt = 0
    private var bossAwake = false

    val bossNpc: Npc?
        get() = boss

    val currentForm: NylocasStyle?
        get() = form

    override fun begin() {
        nextWaveAt = clock + WAVE_START
        tell("The Nylocas stir in the shadows.")
    }

    override fun tick() {
        if (boss == null) {
            spawnWaves()
            tickMobs()
            if (NylocasRules.readyForBoss(wave, mobs.size)) spawnBoss()
        } else {
            tickBoss()
        }
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc === boss) {
            tell("<col=ef1020>Nylocas Vasilias has been defeated!</col>")
            finish()
            return
        }
        mobs.removeAll { it.npc === npc }
    }

    override fun onNpcRemoved(npc: Npc) {
        mobs.removeAll { it.npc === npc }
        if (npc === boss) boss = null
    }

    fun reflect(attacker: Player, type: NylocasStyle?, damage: Int): Int {
        val current = form ?: return damage
        if (!bossAwake || type == null || damage <= 0) return damage
        if (NylocasRules.matches(current, type)) return damage
        val target = boss ?: return damage
        hurt(attacker, damage)
        heal(target, damage)
        return 0
    }

    private fun spawnWaves() {
        if (wave >= NylocasRules.WAVES || clock < nextWaveAt) return
        val spawns = NylocasRules.wave(wave, services.random::of)
        val alive = mobs.sumOf { it.spawn.weight }
        if (alive + spawns.sumOf { it.weight } > NylocasRules.aliveCap(teamSize)) return
        for (spawn in spawns) spawnMob(spawn)
        wave++
        nextWaveAt = clock + NylocasRules.WAVE_GAP
    }

    private fun spawnMob(spawn: NylocasSpawn) {
        val size = if (spawn.big) "big_" else ""
        val npc =
            spawn(
                "npc.tob_nylocas_${size}incoming_${spawn.style.npcKey}$suffix",
                spawn.gate.spawn,
                if (spawn.big) BIG_HP else SMALL_HP,
                stats = MOB_STAT,
            )
        mobs += Mob(npc, spawn, nearestPillar(npc.coords))
    }

    private fun nearestPillar(from: CoordGrid): Int {
        val live = pillars.indices.filter { pillars[it] > 0 }
        if (live.isEmpty()) return -1
        return live.minBy { world(NylocasRules.PILLARS[it]).chebyshevDistance(from) }
    }

    private fun tickMobs() {
        for (mob in mobs.toList()) {
            if (mob.pillar >= 0 && pillars[mob.pillar] <= 0) mob.pillar = nearestPillar(mob.npc.coords)
            val victim = nearestPlayer(mob.npc.coords)
            val range = NylocasRules.MOB_RANGE
            if (victim != null && victim.coords.chebyshevDistance(mob.npc.coords) <= range) {
                attackPlayer(mob, victim)
            } else if (mob.pillar >= 0) {
                approachPillar(mob)
            }
        }
    }

    private fun attackPlayer(mob: Mob, victim: Player) {
        if (clock < mob.nextHit) return
        mob.nextHit = clock + NylocasRules.MOB_RATE
        val type =
            when (mob.spawn.style) {
                NylocasStyle.Melee -> HitType.Melee
                NylocasStyle.Ranged -> HitType.Ranged
                NylocasStyle.Magic -> HitType.Magic
            }
        val reach = if (type == HitType.Melee) 1 else NylocasRules.MOB_RANGE
        if (victim.coords.chebyshevDistance(mob.npc.coords) > reach) {
            mob.npc.walk(victim.coords)
            return
        }
        val max = TobScaling.damage(NylocasRules.MOB_MAX_HIT, mode)
        victim.queueHit(2, type, services.random.of(0, max), services.boss.playerHitModifier)
    }

    private fun approachPillar(mob: Mob) {
        val target = world(NylocasRules.PILLAR_APPROACH[mob.pillar])
        if (mob.npc.coords.chebyshevDistance(target) > 1) {
            if (clock % 2 == 0) mob.npc.walk(target)
            return
        }
        if (clock < mob.nextHit) return
        mob.nextHit = clock + NylocasRules.PILLAR_RATE
        damagePillar(mob.pillar, NylocasRules.pillarDamage(mob.spawn.big, services.random::of))
    }

    private fun damagePillar(index: Int, damage: Int) {
        if (pillars[index] <= 0) return
        pillars[index] -= damage
        if (pillars[index] > 0) return
        pillars[index] = 0
        val at = world(NylocasRules.PILLARS[index])
        services.spotanimAt("spotanim.tob_bloat_blood_splat", at)
        tell("<col=ef1020>A pillar has collapsed!</col>")
        for (player in playersInRoom()) {
            if (player.coords.chebyshevDistance(at) <= NylocasRules.COLLAPSE_RADIUS) {
                hurt(player, NylocasRules.collapseDamage(services.random::of, mode))
            }
        }
        if (pillars.all { it <= 0 } && mode == TobMode.Entry) {
            tell("All the pillars have fallen. The Nylocas overwhelm you.")
            onWipe()
        }
    }

    private fun spawnBoss() {
        for (mob in mobs.toList()) remove(mob.npc)
        mobs.clear()
        val hp = TobScaling.hitpoints(BOSS_HP, teamSize, mode)
        boss = spawn("npc.nylocas_boss_spawning$suffix", BOSS_SPAWN, hp, stats = BOSS_STAT)
        bossSpawnedAt = clock
        bossAwake = false
        tell("Nylocas Vasilias emerges!")
    }

    private fun tickBoss() {
        val target = boss ?: return
        if (!bossAwake) {
            if (clock - bossSpawnedAt < NylocasRules.BOSS_SPAWN_TICKS) return
            bossAwake = true
            switchForm(target)
        }
        if (clock >= formEndsAt) switchForm(target)
        if ((clock - bossSpawnedAt) % NylocasRules.BOSS_RATE == 0) attack(target)
    }

    private fun switchForm(target: Npc) {
        val next = NylocasRules.nextStyle(form, services.random::of)
        form = next
        formEndsAt = clock + NylocasRules.STYLE_TICKS
        transmog(target, "npc.nylocas_boss_${next.npcKey}$suffix")
    }

    private fun attack(target: Npc) {
        val victim = nearestPlayer(target.coords) ?: return
        val type =
            when (form) {
                NylocasStyle.Melee -> HitType.Melee
                NylocasStyle.Ranged -> HitType.Ranged
                NylocasStyle.Magic -> HitType.Magic
                null -> return
            }
        val max = TobScaling.damage(NylocasRules.BOSS_MAX_HIT, mode)
        victim.queueHit(2, type, services.random.of(0, max), services.boss.playerHitModifier)
    }

    private companion object {
        const val WAVE_START = 8
        const val SMALL_HP = 11
        const val BIG_HP = 22
        const val MOB_STAT = 50
        const val BOSS_HP = 2500
        const val BOSS_STAT = 120
        val BOSS_SPAWN = CoordGrid(3294, 4247, 0)
    }
}
