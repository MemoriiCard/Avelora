package org.rsmod.content.raids.tob.nylocas

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.ScriptContext

class NylocasScript @Inject constructor(deps: BossDeps, private val raids: TobRaids) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val room = raids.at(npc.coords)?.controller as? NylocasRoom
                val uid = hit.sourceUid
                if (room != null && room.bossNpc === npc && hit.isFromPlayer && uid != null) {
                    val attacker = PlayerUid(uid).resolve(deps.playerList)
                    if (attacker != null) {
                        hit.damage = room.reflect(attacker, styleOf(hit.type), hit.damage)
                    }
                }
            },
        )
        deps.extensionRegistry.register(IDLE) { _ -> }
    }

    override val spec: BossSpec =
        boss(*NPCS) {
            stats(attackRate = 4)
            val idle = ability("idle") { include(external(IDLE)) }
            phase("fight") { weightedSelectorRandom { +random(idle, weight = 1) } }
        }

    private fun styleOf(type: HitType): NylocasStyle? =
        when (type) {
            HitType.Melee -> NylocasStyle.Melee
            HitType.Ranged -> NylocasStyle.Ranged
            HitType.Magic -> NylocasStyle.Magic
            else -> null
        }

    private companion object {
        const val IDLE = "tob.nylocas_idle"
        val NPCS =
            listOf("", "_story", "_hard")
                .flatMap { suffix ->
                    val styles = NylocasStyle.entries.map { it.npcKey }
                    styles.flatMap { style ->
                        listOf(
                            "npc.tob_nylocas_incoming_$style$suffix",
                            "npc.tob_nylocas_big_incoming_$style$suffix",
                            "npc.tob_nylocas_fighting_$style$suffix",
                            "npc.tob_nylocas_big_fighting_$style$suffix",
                            "npc.nylocas_boss_$style$suffix",
                        )
                    } + "npc.nylocas_boss_spawning$suffix"
                }
                .toTypedArray()
    }
}
