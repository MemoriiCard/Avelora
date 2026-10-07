package org.rsmod.content.bosses.chaosfanatic

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChaosFanatic
@Inject
constructor(private val deps: BossDeps, private val unequipOp: WornUnequipOp) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(this, chaosFanaticSpec(), deps)
        deps.extensionRegistry.register(EXPLOSION_HANDLER) { _, npc, target, _ -> explode(npc, target) }
        deps.extensionRegistry.register(DISARM_HANDLER) { _, _, target, _ -> disarm(target) }
        onEvent<NpcStateEvents.Spawn>(fanaticId) { npc.vars[CALM_ATTACKS_VARN] = 0 }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id == fanaticId) npc.vars[CALM_ATTACKS_VARN] = 0 }
    }

    private val fanaticId by lazy { CHAOS_FANATIC.asRSCM(RSCMType.NPC) }

    private fun explode(npc: Npc, target: Player) {
        val spotanim = BLAST_SPOTANIM.asRSCM(RSCMType.SPOTANIM)
        val impact = SpotanimType(BLAST_IMPACT.asRSCM(RSCMType.SPOTANIM))
        for (tile in blastTiles(target.coords)) {
            deps.bossProjectile(spotanim, npc.coords, tile, START_HEIGHT, 0, START_DELAY, BLAST_TRAVEL, CURVE)
            deps.worldQueues.add(BLAST_LAND_TICKS) {
                deps.worldRepo.spotanimMap(impact, tile)
                if (target.hitpoints <= 0 || target.coords.level != tile.level) return@add
                if (target.coords.chebyshevDistance(tile) > BLAST_RADIUS) return@add
                val damage = deps.random.of(1, BLAST_MAX_HIT)
                target.queueHit(npc, 0, HitType.Typeless, damage, deps.playerHitModifier)
            }
        }
    }

    private fun blastTiles(centre: CoordGrid): List<CoordGrid> {
        val tiles = mutableListOf(centre)
        var attempts = 0
        while (tiles.size < BLAST_COUNT && attempts++ < SCATTER_ATTEMPTS) {
            val tile = centre.translate(deps.random.of(-SCATTER, SCATTER), deps.random.of(-SCATTER, SCATTER))
            if (tile !in tiles && !deps.collision.isWalkBlocked(tile)) tiles += tile
        }
        return tiles
    }

    private fun disarm(player: Player) {
        if (player.worn[WEAPON_SLOT] == null) return
        unequipOp.unequip(player, WEAPON_SLOT, player.worn, player.inv)
    }

    private companion object {
        const val BLAST_SPOTANIM = "spotanim.chaoselemental_spotanim_madness_travel"
        const val BLAST_IMPACT = "spotanim.firewave_impact"
        const val BLAST_COUNT = 3
        const val BLAST_RADIUS = 1
        const val BLAST_MAX_HIT = 31
        const val BLAST_LAND_TICKS = 5
        const val BLAST_TRAVEL = 120
        const val START_DELAY = 30
        const val START_HEIGHT = 43
        const val CURVE = 16
        const val SCATTER = 2
        const val SCATTER_ATTEMPTS = 20
        val WEAPON_SLOT = Wearpos.RightHand.slot
    }
}
