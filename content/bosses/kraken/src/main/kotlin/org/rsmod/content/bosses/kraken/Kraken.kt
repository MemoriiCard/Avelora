package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.invtx.invDel
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onApNpcU
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Kraken
@Inject
internal constructor(
    private val deps: BossDeps,
    private val whirlpools: KrakenWhirlpools,
    private val npcDeath: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spec in listOf(krakenSpec(), tentacleSpec(), caveKrakenSpec())) {
            BossCombat.register(this, spec, deps, onModifyHit = { whirlpools.onIncomingHit(npc, hit) })
        }

        val whirlpool = npcType(KRAKEN_WHIRLPOOL)
        val explosive = ServerCacheManager.getItem(EXPLOSIVE.asRSCM(RSCMType.OBJ))
        if (whirlpool != null && explosive != null) {
            onApNpcU(whirlpool, explosive) {
                if (!whirlpools.canExplode(it.npc) || !invDel(inv, EXPLOSIVE).success) {
                    mes("Nothing interesting happens.")
                    return@onApNpcU
                }
                mes("You throw the explosive into the whirlpool.")
                whirlpools.useExplosive(it.npc, player)
            }
        }

        npcType(KRAKEN)?.let { onNpcQueue(it, DEATH) { krakenDeath(this, KRAKEN_DEATH_SEQ) } }
        npcType(CAVE_KRAKEN)?.let { onNpcQueue(it, DEATH) { krakenDeath(this, KRAKEN_DEATH_SEQ) } }
        npcType(TENTACLE)?.let { onNpcQueue(it, DEATH) { tentacleDeath(this) } }
    }

    private suspend fun krakenDeath(access: StandardNpcAccess, seq: String) {
        val npc = access.npc
        val looter = whirlpools.lastAttacker(npc)
        access.noneMode()
        access.hideAllOps()
        access.anim(seq)
        access.delay(KRAKEN_DEATH_TICKS)
        npcDeath.spawnDrops(access, looter?.coords ?: npc.coords)
        if (npc.visType.internalName == KRAKEN) whirlpools.onKrakenDeath(npc)
        submergeAndRespawn(npc)
    }

    private suspend fun tentacleDeath(access: StandardNpcAccess) {
        access.noneMode()
        access.hideAllOps()
        access.anim(TENTACLE_DEATH_SEQ)
        access.delay(TENTACLE_DEATH_TICKS)
        submergeAndRespawn(access.npc)
    }

    private fun submergeAndRespawn(npc: Npc) {
        whirlpools.submerge(npc)
        if (npc.respawns) deps.npcRepo.despawn(npc, npc.type.respawnRate) else deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))

    private companion object {
        const val DEATH = "queue.death"
        const val EXPLOSIVE = "obj.fishing_explosive"
        const val KRAKEN_DEATH_SEQ = "seq.swan_queen_death"
        const val TENTACLE_DEATH_SEQ = "seq.tentacle_monster_death"
        const val KRAKEN_DEATH_TICKS = 4
        const val TENTACLE_DEATH_TICKS = 3
    }
}
