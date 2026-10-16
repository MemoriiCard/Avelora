package org.rsmod.content.bosses.royaltitans

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.ScriptContext

class RoyalTitans @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec: BossSpec = RoyalTitanSpecs.build(Titan.BRANDA)

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        BossCombat.register(this, RoyalTitanSpecs.build(Titan.ELDRIC), deps)

        deps.extensionRegistry.register(RoyalTitanSpecs.ELEMENTAL_HANDLER) { _, elemental, _, _ ->
            deps.worldQueues.add(EXPLODE_TICKS) { explode(elemental) }
        }
    }

    private fun explode(elemental: Npc) {
        if (elemental.hitpoints <= 0) return
        val fire = elemental.type.name.contains("fire", ignoreCase = true)
        elemental.anim(if (fire) FIRE_EXPLODE_SEQ else ICE_EXPLODE_SEQ)
        for (player in deps.playerList) {
            if (player.hitpoints <= 0) continue
            val near =
                kotlin.math.abs(player.coords.x - elemental.coords.x) <= BLAST_RADIUS &&
                    kotlin.math.abs(player.coords.z - elemental.coords.z) <= BLAST_RADIUS &&
                    player.coords.level == elemental.coords.level
            if (!near) continue
            val damage = EXPLODE_MIN + deps.random.of(EXPLODE_MAX - EXPLODE_MIN + 1)
            player.finishNpcHit(elemental, 1, HitType.Typeless, damage, deps.playerHitModifier)
        }
        deps.worldQueues.add(1) { deps.npcRepo.del(elemental, Int.MAX_VALUE) }
    }

    private companion object {
        const val EXPLODE_TICKS = 9
        const val EXPLODE_MIN = 8
        const val EXPLODE_MAX = 10
        const val BLAST_RADIUS = 1
        const val FIRE_EXPLODE_SEQ = "seq.fire_minion_explode01"
        const val ICE_EXPLODE_SEQ = "seq.ice_minion_explode01"
    }
}
