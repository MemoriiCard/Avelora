package org.rsmod.content.bosses.crazyarchaeologist

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CrazyArchaeologist @Inject constructor(private val deps: BossDeps) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, crazyArchaeologistSpec(), deps, onLethal = { it.say(DEATH_SHOUT) })
        deps.extensionRegistry.register(RAIN_HANDLER) { _, npc, target, _ -> rainOfKnowledge(npc, target) }
    }

    private fun rainOfKnowledge(npc: Npc, target: Player) {
        val tiles = scatter(target.coords, count = BOOKS, spread = 1)
        val splitter = tiles[deps.random.of(tiles.size)]
        for (tile in tiles) {
            throwBook(npc, target, npc.coords, tile, delay = START_DELAY)
            if (tile == splitter) {
                deps.worldQueues.add(LAND_TICKS) {
                    for (split in scatter(tile, count = SPLIT_BOOKS, spread = SPLIT_SPREAD, includeCentre = false)) {
                        throwBook(npc, target, tile, split, delay = 0)
                    }
                }
            }
        }
    }

    private fun throwBook(npc: Npc, target: Player, from: CoordGrid, tile: CoordGrid, delay: Int) {
        deps.bossProjectile(BOOK.asRSCM(RSCMType.SPOTANIM), from, tile, START_HEIGHT, 0, delay, TRAVEL - delay, CURVE)
        deps.worldQueues.add(LAND_TICKS) {
            deps.worldRepo.spotanimMap(SpotanimType(EXPLOSION.asRSCM(RSCMType.SPOTANIM)), tile)
            if (target.hitpoints <= 0 || target.coords.level != tile.level) return@add
            if (target.coords.chebyshevDistance(tile) > BLAST_RADIUS) return@add
            target.queueHit(npc, 0, HitType.Typeless, deps.random.of(1, RAIN_MAX_HIT), deps.playerHitModifier)
        }
    }

    private fun scatter(centre: CoordGrid, count: Int, spread: Int, includeCentre: Boolean = true): List<CoordGrid> {
        val tiles = if (includeCentre) mutableListOf(centre) else mutableListOf()
        var attempts = 0
        while (tiles.size < count && attempts++ < SCATTER_ATTEMPTS) {
            val tile = centre.translate(deps.random.of(-spread, spread), deps.random.of(-spread, spread))
            if (tile != centre && tile !in tiles && !deps.collision.isWalkBlocked(tile)) tiles += tile
        }
        return tiles
    }

    private companion object {
        const val DEATH_SHOUT = "Ow!"
        const val BOOK = "spotanim.crazy_archaeologist_book_special"
        const val EXPLOSION = "spotanim.firewave_impact"
        const val BOOKS = 3
        const val SPLIT_BOOKS = 2
        const val SPLIT_SPREAD = 2
        const val BLAST_RADIUS = 1
        const val RAIN_MAX_HIT = 24
        const val LAND_TICKS = 4
        const val TRAVEL = 120
        const val START_DELAY = 30
        const val START_HEIGHT = 43
        const val CURVE = 30
        const val SCATTER_ATTEMPTS = 20
    }
}
