package org.rsmod.content.raids.tob.sotetseg

import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.boss.TobBossRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class SotetsegRoom(
    raid: TobRaid,
    services: CoxRoomServices,
    onCleared: () -> Unit,
) : TobBossRoom(raid, TobRoom.Sotetseg, services, onCleared) {
    private class Ball(val target: Player, val landsAt: Int)

    private var boss: Npc? = null
    private var ticks = 0
    private var attacks = 0
    private var stage = 0
    private var path: List<Pair<Int, Int>> = emptyList()
    private var chosen: Player? = null
    private var mazeEndsAt = -1
    private val balls = mutableListOf<Ball>()

    val inMaze: Boolean
        get() = chosen != null

    override fun inside(coords: CoordGrid): Boolean =
        raid.roomAt(coords).let { it === TobRoom.Sotetseg || it === TobRoom.Maze }

    override fun begin() {
        val hp = TobScaling.hitpoints(BASE_HP, teamSize, mode)
        boss = spawn("npc.tob_sotetseg_combat$suffix", SPAWN, hp, stats = BOSS_STAT)
        tell("Sotetseg awakens.")
    }

    override fun tick() {
        val target = boss ?: return
        ticks++
        landBalls()
        if (inMaze) {
            tickMaze()
            return
        }
        val percent = SotetsegRules.hpPercent(target.hitpoints, target.baseHitpointsLvl)
        val next = SotetsegRules.nextStage(percent, stage)
        if (next != null) {
            stage = next + 1
            startMaze()
            return
        }
        if (ticks % SotetsegRules.ATTACK_RATE == 0) attack(target)
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc !== boss) return
        balls.clear()
        chosen?.let { returnFromMaze(it) }
        chosen = null
        tell("<col=ef1020>Sotetseg has been defeated!</col>")
        finish()
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === boss) boss = null
    }

    fun leaveMaze(player: Player) {
        if (chosen === player) endMaze()
    }

    private fun attack(target: Npc) {
        val players = playersInRoom().filter { raid.roomAt(it.coords) === TobRoom.Sotetseg }
        if (players.isEmpty()) return
        attacks++
        if (attacks % SotetsegRules.BALL_EVERY == 0) {
            fireBall(target, players.random())
            return
        }
        val near = players.filter { it.coords.chebyshevDistance(target.coords) <= SotetsegRules.MELEE_RANGE + 2 }
        if (near.isNotEmpty()) {
            target.anim("seq.tob_sotetseg_attack_melee")
            val max = TobScaling.damage(SotetsegRules.MELEE_MAX, mode)
            near.first().queueHit(1, HitType.Melee, services.random.of(0, max), services.boss.playerHitModifier)
            return
        }
        val ranged = attacks % 2 == 0
        target.anim(if (ranged) "seq.tob_sotetseg_attack_ranged" else "seq.tob_sotetseg_attack_melee")
        val max = TobScaling.damage(SotetsegRules.ORB_MAX, mode)
        val first = players.random()
        val type = if (ranged) HitType.Ranged else HitType.Magic
        first.queueHit(3, type, services.random.of(0, max), services.boss.playerHitModifier)
        val other = players.filter { it !== first }
        if (other.isNotEmpty()) {
            val second = type.let { if (it == HitType.Ranged) HitType.Magic else HitType.Ranged }
            other.random().queueHit(5, second, services.random.of(0, max), services.boss.playerHitModifier)
        }
    }

    private fun fireBall(target: Npc, victim: Player) {
        target.anim("seq.tob_sotetseg_attack_ranged")
        services.lob("spotanim.tob_sotetseg_sharedattack", target.coords, victim.coords) {}
        balls += Ball(victim, clock + SotetsegRules.BALL_DELAY)
        victim.mes("<col=ff0000>Sotetseg launches a deadly ball at you!</col>")
    }

    private fun landBalls() {
        val due = balls.filter { it.landsAt <= clock }
        if (due.isEmpty()) return
        balls.removeAll(due.toSet())
        for (ball in due) {
            val centre = ball.target.coords
            services.spotanimAt("spotanim.tob_sotetseg_sharedattack_impact", centre)
            val hit =
                playersInRoom().filter {
                    it.coords.level == centre.level &&
                        it.coords.chebyshevDistance(centre) <= SotetsegRules.BALL_SPLASH
                }
            val total = SotetsegRules.ballDamage(teamSize, mode)
            for (player in hit) hurt(player, SotetsegRules.ballShare(total, hit.size))
        }
    }

    private fun startMaze() {
        val players = playersInRoom().filter { raid.roomAt(it.coords) === TobRoom.Sotetseg }
        val victim = players.randomOrNull() ?: return
        path = SotetsegRules.path(services.random::of)
        chosen = victim
        mazeEndsAt = clock + SotetsegRules.MAZE_TIMEOUT
        tell("<col=ef1020>Sotetseg drags a soul into the Shadow Realm!</col>")
        PathingEntityCommon.telejump(
            victim,
            services.collision,
            raid.coords(TobRoom.Maze, SotetsegRules.mazeTile(path.first())),
        )
    }

    private fun tickMaze() {
        val victim = chosen ?: return
        if (clock >= mazeEndsAt || raid.roomAt(victim.coords) !== TobRoom.Maze) {
            endMaze()
            return
        }
        if (clock % 2 == 0) {
            for (cell in path) {
                services.spotanimAt("spotanim.tob_sotetseg_zap", raid.coords(TobRoom.Maze, SotetsegRules.mazeTile(cell)))
            }
        }
        val local = raid.source(victim.coords) ?: return
        val x = local.x - SotetsegRules.MAZE_ORIGIN.x
        val z = local.z - SotetsegRules.MAZE_ORIGIN.z
        if (inGrid(x, z) && !SotetsegRules.onPath(path, x, z)) {
            hurt(victim, SotetsegRules.wrongTileDamage(victim.hitpoints))
        }
        if (clock % SotetsegRules.CHIP_RATE == 0) hurt(victim, 1 + services.random.of(3))
        for (player in playersInRoom()) {
            if (player === victim || raid.roomAt(player.coords) !== TobRoom.Sotetseg) continue
            val arena = raid.source(player.coords) ?: continue
            val ax = arena.x - SotetsegRules.ARENA_ORIGIN.x
            val az = arena.z - SotetsegRules.ARENA_ORIGIN.z
            if (inGrid(ax, az) && !SotetsegRules.onPath(path, ax, az)) {
                hurt(player, SotetsegRules.wrongTileDamage(player.hitpoints))
            }
        }
    }

    private fun inGrid(x: Int, z: Int): Boolean =
        x in 0 until SotetsegRules.GRID_WIDTH && z in 0 until SotetsegRules.GRID_HEIGHT

    private fun endMaze() {
        val victim = chosen ?: return
        chosen = null
        path = emptyList()
        returnFromMaze(victim)
    }

    private fun returnFromMaze(player: Player) {
        if (raid.roomAt(player.coords) === TobRoom.Maze) {
            PathingEntityCommon.telejump(player, services.collision, raid.arrival(TobRoom.Sotetseg))
        }
    }

    private companion object {
        const val BASE_HP = 4000
        const val BOSS_STAT = 150
        val SPAWN = CoordGrid(3278, 4324, 0)
    }
}
