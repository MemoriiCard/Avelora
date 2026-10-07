package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

private fun balanceBas(walk: String, ready: String): BasType {
    val walkId = walk.asRSCM(RSCMType.SEQ)
    val readyId = ready.asRSCM(RSCMType.SEQ)
    return BasType(
        readyAnim = readyId,
        turnOnSpot = walkId,
        walkForward = walkId,
        walkBack = walkId,
        walkLeft = walkId,
        walkRight = walkId,
        running = walkId,
    )
}

/** Walks straight to [dest] ignoring collision, using a balancing walk animation the whole way. */
suspend fun ProtectedAccess.balanceWalk(
    dest: CoordGrid,
    walk: String = "seq.human_walk_logbalance",
    ready: String = "seq.human_walk_logbalance_ready",
    via: List<CoordGrid> = emptyList(),
) {
    val previous = player.bas
    player.bas = balanceBas(walk, ready)
    try {
        val path = via + dest
        val tiles = path.zipWithNext { a, b -> a.chebyshevDistance(b) }.sum() + coords.chebyshevDistance(path.first())
        forcedWalk(path, tiles)
    } finally {
        player.bas = previous
    }
}

/** Plays a climb animation, then moves the player to [dest] (usually a different floor). */
suspend fun ProtectedAccess.climbTo(
    dest: CoordGrid,
    anim: String = "seq.human_reachforladder",
    ticks: Int = 2,
) {
    anim(anim)
    delay(ticks)
    telejump(dest, TeleportType.Exempt)
}

/** Rolls the obstacle's success chance against the player's Agility level. */
fun ProtectedAccess.slips(chance: FailChance): Boolean =
    !statRandom(AGILITY, chance.low, chance.high, invisibleBoost = 0)

/** Knocks the player off an obstacle onto [dest], hitting them for a roll of [damage]. */
suspend fun ProtectedAccess.fallTo(dest: CoordGrid, damage: IntRange, message: String) {
    anim("seq.human_wobbleandfall_l")
    delay(2)
    telejump(dest, TeleportType.Exempt)
    anim("seq.human_falling_end")
    mes(message)
    queueHit(delay = 1, type = HitType.Typeless, damage = random.of(damage))
    delay(1)
}

/** Shuffles onto an obstacle's start tile when the route stopped beside it. */
suspend fun ProtectedAccess.stepTo(start: CoordGrid) {
    if (coords != start) {
        forcedWalk(start, coords.chebyshevDistance(start))
    }
}

/** Leaps from the current tile to [dest] over [ticks] game ticks, facing [dir] on landing. */
suspend fun ProtectedAccess.jumpTo(dest: CoordGrid, anim: String, ticks: Int, dir: Int) {
    anim(anim)
    exactMove(coords, dest, delay1 = 0, delay2 = ticks * CLIENT_CYCLES_PER_TICK, dir, TeleportType.Exempt)
    delay(ticks)
}

/**
 * An obstacle's success curve on the 0-255 scale used by [ProtectedAccess.statRandom]: [low] at
 * level 1 rising to [high] at 99. A [high] above 255 means the obstacle stops failing before 99.
 */
data class FailChance(val low: Int, val high: Int)

private const val AGILITY = "stat.agility"
private const val CLIENT_CYCLES_PER_TICK = 30
