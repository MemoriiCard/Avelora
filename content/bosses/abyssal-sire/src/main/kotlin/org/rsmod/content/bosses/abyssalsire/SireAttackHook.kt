package org.rsmod.content.bosses.abyssalsire

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.stat.statBase
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class SireAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        val name = RSCM.getReverseMapping(RSCMType.NPC, npc.type.id)
        if (name !in GATED) return NpcAttackValidateResult.Pass
        if (player.statBase("stat.slayer") < AbyssalNexus.SLAYER_LEVEL) {
            return NpcAttackValidateResult.Deny("You need a Slayer level of ${AbyssalNexus.SLAYER_LEVEL} to fight the Sire.")
        }
        val task = player.vars["varp.slayer_target"]
        if (task == 0 || TASK_TYPES.none { SlayerTaskManager.isTaskNpcType(it, task) }) {
            return NpcAttackValidateResult.Deny("You need an Abyssal demon or Abyssal Sire Slayer task to fight the Sire.")
        }
        return NpcAttackValidateResult.Pass
    }

    private companion object {
        val GATED = setOf(SIRE_SLEEPING, LUNG)

        val TASK_TYPES by lazy {
            listOf(SIRE_SLEEPING, "npc.slayer_abyssal").mapNotNull {
                ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC))
            }
        }
    }
}
