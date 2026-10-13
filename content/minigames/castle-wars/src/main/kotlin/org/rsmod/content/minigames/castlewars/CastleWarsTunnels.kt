package org.rsmod.content.minigames.castlewars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal enum class TunnelStage(val symbol: String?) {
    Open(null),
    Blocked("loc.castlewars_blocked_tunnel_1"),
    Loosened("loc.castlewars_blocked_tunnel_2"),
}

internal object TunnelRules {
    const val CRUSH_DAMAGE = 200

    class Site(val origin: CoordGrid, val angle: Int)

    val SITES =
        listOf(
            Site(CoordGrid(2391, 9501, 0), 1),
            Site(CoordGrid(2400, 9512, 0), 0),
            Site(CoordGrid(2401, 9494, 0), 0),
            Site(CoordGrid(2409, 9503, 0), 1),
        )

    fun tiles(site: Site): List<CoordGrid> =
        listOf(0, 1).flatMap { dx -> listOf(0, 1).map { dz -> site.origin.translate(dx, dz) } }

    fun nearest(coords: CoordGrid): Int =
        SITES.indices.minBy { index ->
            val origin = SITES[index].origin
            maxOf(kotlin.math.abs(origin.x - coords.x), kotlin.math.abs(origin.z - coords.z))
        }
}

internal class TunnelLedger {
    private val stages = MutableList(TunnelRules.SITES.size) { TunnelStage.Open }

    fun stage(index: Int): TunnelStage = stages[index]

    fun collapse(index: Int): Boolean {
        if (stages[index] != TunnelStage.Open) return false
        stages[index] = TunnelStage.Blocked
        return true
    }

    fun clear(index: Int): TunnelStage? =
        when (stages[index]) {
            TunnelStage.Open -> null
            TunnelStage.Blocked -> TunnelStage.Loosened.also { stages[index] = it }
            TunnelStage.Loosened -> TunnelStage.Open.also { stages[index] = it }
        }

    fun reset() = stages.indices.forEach { stages[it] = TunnelStage.Open }
}

@Singleton
class CastleWarsTunnels
@Inject
constructor(
    private val locs: LocRepository,
    private val players: PlayerList,
    private val hitModifier: StandardPlayerHitModifier,
) {
    private val ledger = TunnelLedger()

    internal fun stage(index: Int): TunnelStage = ledger.stage(index)

    internal fun collapse(index: Int): Boolean {
        if (!ledger.collapse(index)) return false
        show(index)
        val area = TunnelRules.tiles(TunnelRules.SITES[index]).toSet()
        for (player in players) {
            if (player.coords in area) {
                player.queueHit(1, HitType.Typeless, TunnelRules.CRUSH_DAMAGE, hitModifier)
            }
        }
        return true
    }

    internal fun clear(index: Int): Boolean {
        ledger.clear(index) ?: return false
        show(index)
        return true
    }

    fun reset() {
        ledger.reset()
        TunnelRules.SITES.indices.forEach(::show)
    }

    private fun show(index: Int) {
        val site = TunnelRules.SITES[index]
        locs.findExact(site.origin, LocShape.CentrepieceStraight)?.let { locs.del(it, Int.MAX_VALUE) }
        val symbol = ledger.stage(index).symbol ?: return
        val type = ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)) ?: return
        locs.add(site.origin, type, Int.MAX_VALUE, LocAngle[site.angle], LocShape.CentrepieceStraight)
    }
}
