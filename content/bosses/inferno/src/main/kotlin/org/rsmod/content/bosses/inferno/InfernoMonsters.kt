package org.rsmod.content.bosses.inferno

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class InfernoMonsters
@Inject
internal constructor(private val deps: BossDeps, private val runs: InfernoRuns) : PluginScript() {
    override fun ScriptContext.startup() {
        val zukRoll: (Npc, Player) -> Int = { npc, target -> runs.zukDamage(npc, target) }
        for (spec in infernoSpecs(zukRoll)) {
            when (spec.npcTypes.first()) {
                InfernoMonster.Jad.npc ->
                    BossCombat.register(this, spec, deps, onHit = {
                        if (npc.hitpoints in 1 until npc.baseHitpointsLvl / 2) runs.summonJadHealers(npc)
                    })
                ZukStats.HEALER,
                ZukStats.JAD_HEALER ->
                    BossCombat.register(this, spec, deps, onHit = {
                        if (hit.isFromPlayer) runs.distractHealer(npc)
                    })
                else -> BossCombat.register(this, spec, deps)
            }
        }
        val registry = deps.extensionRegistry
        registry.register(ZukStats.SET_HANDLER) { _, npc, _, _ -> runs.zukSet(npc) }
        registry.register(ZukStats.JAD_HANDLER) { _, npc, _, _ -> runs.zukJad(npc) }
        registry.register(ZukStats.HEALERS_HANDLER) { _, npc, _, _ -> runs.zukHealers(npc) }

        for ((type, death) in DEATHS) {
            val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(npcType, "queue.death") { runs.onDeath(this, death.first, death.second) }
        }
    }

    internal companion object {
        val DEATHS =
            mapOf(
                InfernoMonster.Nibbler.npc to ("seq.jalnib_death" to 3),
                InfernoMonster.Bat.npc to ("seq.jalmejrah_death" to 3),
                InfernoMonster.Blob.npc to ("seq.jalak_death" to 3),
                InfernoMonster.Meleer.npc to ("seq.jalimkot_death" to 4),
                InfernoMonster.Ranger.npc to ("seq.jalxil_death" to 4),
                InfernoMonster.Mager.npc to ("seq.jalakxil_death" to 4),
                InfernoMonster.Jad.npc to ("seq.jaltokjad_death" to 5),
                ZukStats.NPC to ("seq.zuk_death" to 6),
                ZukStats.HEALER to ("seq.jalmejrah_death" to 3),
                ZukStats.JAD_HEALER to ("seq.lizard_cleric_death" to 3),
                BLOB_MELEE to ("seq.jalak_death" to 3),
                BLOB_RANGE to ("seq.jalak_death" to 3),
                BLOB_MAGE to ("seq.jalak_death" to 3),
            )
    }
}
