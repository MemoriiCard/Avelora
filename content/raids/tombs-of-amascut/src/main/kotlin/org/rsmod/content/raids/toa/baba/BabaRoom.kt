package org.rsmod.content.raids.toa.baba

import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.boss.ToaCombat
import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class BabaRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.Baba, services, onCleared) {
    private class Slam(val centre: CoordGrid, val landsAt: Int)

    private class Rockfall(val spots: List<CoordGrid>, val landsAt: Int)

    private class Boulders(val column: Int, val startedAt: Int)

    private var baba: Npc? = null
    private var attacks = 0
    private var phasesDone = 0
    private var slam: Slam? = null
    private var rockfall: Rockfall? = null
    private val boulders = mutableListOf<Boulders>()
    private var bouldersLeft = 0
    private var nextBoulderAt = 0
    private val baboons = mutableListOf<Npc>()

    override fun begin() {
        baba = spawn("npc.toa_baba", BabaRules.BOSS, scaledHp(BabaRules.BASE_HP), BabaRules.STAT)
        tell("Ba-Ba stirs from her slumber.")
    }

    override fun tick() {
        val boss = baba ?: return
        val percent = ToaCombat.percent(boss.hitpoints, boss.baseHitpointsLvl)
        val phase = BabaRules.nextPhase(percent, phasesDone)
        if (phase != null) startPhase(phase)
        val busy = slam != null || rockfall != null || bouldersLeft > 0
        if (!busy && clock % BabaRules.ATTACK_RATE == 0) attack(boss)
        landSlam()
        landRockfall()
        rollBoulders()
        for (baboon in baboons.toList()) chase(baboon, BabaRules.BABOON_HIT, BabaRules.BABOON_RATE)
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc === baba) {
            for (baboon in baboons.toList()) remove(baboon)
            baboons.clear()
            slam = null
            rockfall = null
            bouldersLeft = 0
            tell("<col=ef1020>Ba-Ba has been defeated!</col>")
            finish()
        } else if (baboons.any { it === npc }) {
            baboons.removeAll { it === npc }
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        baboons.removeAll { it === npc }
        if (npc === baba) baba = null
    }

    private fun attack(boss: Npc) {
        val number = attacks++
        when {
            BabaRules.isRockfall(number) -> startRockfall()
            BabaRules.isSlam(number) -> startSlam(boss)
            else -> autoAttack(boss, number)
        }
    }

    private fun autoAttack(boss: Npc, number: Int) {
        val target = nearestPlayer(boss.coords) ?: return
        val style = BabaRules.style(number)
        hurtStyled(target, style, scaledDamage(services.random.of(0, BabaRules.MAX_HIT)))
    }

    private fun startSlam(boss: Npc) {
        val centre = CoordGrid(boss.coords.x + BabaRules.SIZE / 2, boss.coords.z + BabaRules.SIZE / 2, boss.coords.level)
        slam = Slam(centre, clock + BabaRules.SLAM_DELAY)
        tell("Ba-Ba rears up. Step out of the shadow!")
    }

    private fun landSlam() {
        val current = slam ?: return
        if (clock < current.landsAt) return
        slam = null
        val arm = BabaRules.slamArm(ToaInvocation.ShakingThingsUp in invocations)
        for (player in playersInRoom()) {
            if (BabaRules.inCross(player.coords, current.centre, arm)) {
                hurtStyled(player, ToaStyle.Melee, scaledDamage(services.random.of(BabaRules.SLAM_MAX / 2, BabaRules.SLAM_MAX)))
            }
        }
    }

    private fun startRockfall() {
        val spots =
            List(BabaRules.ROCKFALL_SPOTS) {
                world(
                    CoordGrid(
                        BabaRules.ARENA_X.first + services.random.of(BabaRules.ARENA_X.count()),
                        BabaRules.ARENA_Z.first + services.random.of(BabaRules.ARENA_Z.count()),
                        0,
                    )
                )
            }
        rockfall = Rockfall(spots, clock + BabaRules.SLAM_DELAY)
        tell("Rocks tumble from the ceiling. Stand beside debris or a friend!")
    }

    private fun landRockfall() {
        val current = rockfall ?: return
        if (clock < current.landsAt) return
        rockfall = null
        val players = playersInRoom()
        for (player in players) {
            val damage = scaledDamage(services.random.of(BabaRules.ROCKFALL_MAX / 2, BabaRules.ROCKFALL_MAX))
            val neighbours =
                players.count { it !== player && it.coords.chebyshevDistance(player.coords) <= 1 } +
                    current.spots.count { it.chebyshevDistance(player.coords) <= 1 }
            hurt(player, BabaRules.splitDamage(damage, neighbours))
        }
    }

    private fun startPhase(index: Int) {
        phasesDone = index + 1
        bouldersLeft = BabaRules.BOULDER_ROWS * (if (ToaInvocation.Boulderdash in invocations) 2 else 1)
        nextBoulderAt = clock
        tell("Ba-Ba screams and boulders begin to roll!")
        repeat(BabaRules.baboonCount(teamSize)) {
            baboons += spawn("npc.toa_baba_baboon", BabaRules.BOSS.translate(-2 - it, 3), BabaRules.BABOON_HP)
        }
    }

    private fun rollBoulders() {
        if (bouldersLeft > 0 && clock >= nextBoulderAt) {
            val x = BabaRules.ARENA_X
            boulders += Boulders(x.first + services.random.of(x.count()), clock)
            bouldersLeft--
            nextBoulderAt = clock + BabaRules.BOULDER_GAP
        }
        val finished = mutableListOf<Boulders>()
        for (row in boulders) {
            if (clock - row.startedAt > 1) {
                finished += row
                val column = raid.coords(room, CoordGrid(row.column, BabaRules.ARENA_Z.first, 0)).x
                for (player in playersInRoom()) {
                    if (kotlin.math.abs(player.coords.x - column) <= 1) {
                        hurt(player, scaledDamage(services.random.of(BabaRules.BOULDER_MAX / 2, BabaRules.BOULDER_MAX)))
                    }
                }
            }
        }
        boulders.removeAll(finished.toSet())
    }
}
