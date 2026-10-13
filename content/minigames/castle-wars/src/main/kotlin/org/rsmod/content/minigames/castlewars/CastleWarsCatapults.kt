package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal enum class CatapultState {
    Working,
    Burning,
    Broken,
}

internal object CatapultRules {
    const val MAX_AXIS = 30
    const val COOLDOWN_TICKS = 8
    const val BURN_TICKS = 25
    const val MIN_DAMAGE = 5
    const val MAX_DAMAGE = 15
    const val SPLASH_RADIUS = 2
    const val SCATTER = 2
    const val ACCURATE_ONE_IN = 2

    val HOME =
        mapOf(
            CwTeam.Saradomin to CoordGrid(2413, 3088, 1),
            CwTeam.Zamorak to CoordGrid(2384, 3117, 1),
        )

    fun target(team: CwTeam, x: Int, z: Int): CoordGrid {
        val home = HOME.getValue(team)
        return if (team == CwTeam.Saradomin) home.translate(-x, z).copy(level = 0) else home.translate(x, -z).copy(level = 0)
    }

    fun scatter(target: CoordGrid, roll: (Int) -> Int): CoordGrid {
        if (roll(ACCURATE_ONE_IN) == 0) return target
        val span = 2 * SCATTER + 1
        return target.translate(roll(span) - SCATTER, roll(span) - SCATTER)
    }

    fun splash(centre: CoordGrid): List<CoordGrid> =
        (-SPLASH_RADIUS..SPLASH_RADIUS).flatMap { dx ->
            (-SPLASH_RADIUS..SPLASH_RADIUS).map { dz -> centre.translate(dx, dz) }
        }

    fun symbol(team: CwTeam, state: CatapultState): String =
        "loc.castlewars_catapult_" + team.name.lowercase() + if (state == CatapultState.Working) "" else "_" + state.name.lowercase()
}

internal class CatapultLedger {
    private val states = CwTeam.entries.associateWith { CatapultState.Working }.toMutableMap()
    private val burnsOutAt = HashMap<CwTeam, Int>()
    private val readyAt = HashMap<CwTeam, Int>()

    fun state(team: CwTeam): CatapultState = states.getValue(team)

    fun cooldownLeft(team: CwTeam, cycle: Int): Int = ((readyAt[team] ?: 0) - cycle).coerceAtLeast(0)

    fun operate(team: CwTeam, cycle: Int): Boolean {
        if (state(team) != CatapultState.Working || cooldownLeft(team, cycle) > 0) return false
        readyAt[team] = cycle + CatapultRules.COOLDOWN_TICKS
        return true
    }

    fun ignite(team: CwTeam, cycle: Int): Boolean {
        if (state(team) != CatapultState.Working) return false
        states[team] = CatapultState.Burning
        burnsOutAt[team] = cycle + CatapultRules.BURN_TICKS
        return true
    }

    fun extinguish(team: CwTeam): Boolean {
        if (state(team) != CatapultState.Burning) return false
        states[team] = CatapultState.Working
        burnsOutAt.remove(team)
        return true
    }

    fun repair(team: CwTeam): Boolean {
        if (state(team) != CatapultState.Broken) return false
        states[team] = CatapultState.Working
        return true
    }

    fun update(cycle: Int): List<CwTeam> {
        val broken = CwTeam.entries.filter { state(it) == CatapultState.Burning && (burnsOutAt[it] ?: Int.MAX_VALUE) <= cycle }
        for (team in broken) {
            states[team] = CatapultState.Broken
            burnsOutAt.remove(team)
        }
        return broken
    }

    fun reset() {
        CwTeam.entries.forEach { states[it] = CatapultState.Working }
        burnsOutAt.clear()
        readyAt.clear()
    }
}

@Singleton
class CastleWarsCatapults
@Inject
constructor(
    private val locs: LocRepository,
    private val players: PlayerList,
    private val hitModifier: StandardPlayerHitModifier,
    private val random: GameRandom,
) {
    private val ledger = CatapultLedger()

    internal fun state(team: CwTeam): CatapultState = ledger.state(team)

    internal fun operate(team: CwTeam, cycle: Int): Boolean = ledger.operate(team, cycle)

    internal fun cooldownLeft(team: CwTeam, cycle: Int): Int = ledger.cooldownLeft(team, cycle)

    internal fun ignite(team: CwTeam, cycle: Int): Boolean = ledger.ignite(team, cycle).also { if (it) show(team) }

    internal fun extinguish(team: CwTeam): Boolean = ledger.extinguish(team).also { if (it) show(team) }

    internal fun repair(team: CwTeam): Boolean = ledger.repair(team).also { if (it) show(team) }

    fun update(cycle: Int) {
        ledger.update(cycle).forEach(::show)
    }

    fun reset() {
        ledger.reset()
        CwTeam.entries.forEach(::show)
    }

    internal fun fire(team: CwTeam, x: Int, z: Int): CoordGrid {
        val impact = CatapultRules.scatter(CatapultRules.target(team, x, z)) { random.of(it) }
        val area = CatapultRules.splash(impact).toSet()
        for (player in players) {
            if (player.coords !in area || !CastleWarsMap.inside(player.coords)) continue
            val damage = CatapultRules.MIN_DAMAGE + random.of(CatapultRules.MAX_DAMAGE - CatapultRules.MIN_DAMAGE + 1)
            player.queueHit(delay = 2, type = HitType.Typeless, damage = damage, modifier = hitModifier)
        }
        return impact
    }

    private fun show(team: CwTeam) {
        val loc = locs.findExact(CatapultRules.HOME.getValue(team), LocShape.CentrepieceDiagonal) ?: return
        val type = ServerCacheManager.getObject(CatapultRules.symbol(team, ledger.state(team)).asRSCM(RSCMType.LOC)) ?: return
        locs.change(loc, type, Int.MAX_VALUE)
    }
}
