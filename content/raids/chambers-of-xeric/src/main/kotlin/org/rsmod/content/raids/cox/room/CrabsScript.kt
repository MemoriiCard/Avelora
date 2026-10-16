package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc2
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.ScriptContext

class CrabsScript
@Inject
constructor(deps: BossDeps, private val raids: CoxRaids, private val services: CoxRoomServices) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = { hit.damage = 0 },
            onHit = {
                val player = hit.resolvePlayerSource(deps.playerList) ?: return@register
                val room = roomOf(npc) ?: return@register
                val tint =
                    when (hit.type) {
                        HitType.Melee -> CrabsRoom.Tint.Red
                        HitType.Ranged -> CrabsRoom.Tint.Green
                        HitType.Magic -> CrabsRoom.Tint.Blue
                        else -> return@register
                    }
                room.colourCrab(player, npc, tint)
            },
        )
        for (type in CrabsRoom.CRAB_TYPES) onOpNpc2(type) { smash(it.npc) }
    }

    private fun roomOf(npc: Npc): CrabsRoom? =
        raids.at(npc.coords)?.controllerOf(npc) as? CrabsRoom

    private fun ProtectedAccess.smash(crab: Npc) {
        val room = roomOf(crab) ?: return
        if (!playerContainsObj(CrabsRoom.HAMMER)) {
            mes("You need a hammer to smash the crab.")
            return
        }
        anim(SMASH_SEQ)
        room.colourCrab(player, crab, CrabsRoom.Tint.Red)
    }

    override val spec: BossSpec =
        boss(*CrabsRoom.CRAB_TYPES.toTypedArray()) {
            stats(attackRate = 4)
            val pinch = ability("pinch") { melee("seq.horror_crab_attack", MeleeAttackType.Crush) }
            phase("fight") { weightedSelectorRandom { +random(pinch, weight = 1) } }
        }

    private companion object {
        const val SMASH_SEQ = "seq.human_pickupfloor"
    }
}
