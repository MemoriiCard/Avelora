package org.rsmod.content.bosses.nex

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

public class NexModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(NexMageShieldHook::class.java)
    }
}

internal class NexMageShieldHook @Inject constructor(private val fight: NexFight) : NpcAttackValidateHook {
    private val mageIds by lazy { NexMage.entries.map { it.npc.asRSCM(RSCMType.NPC) }.toSet() }

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc.id !in mageIds || !fight.isShielded(npc)) return NpcAttackValidateResult.Pass
        return NpcAttackValidateResult.Deny("${npc.name} is shielded by Nex's power.")
    }
}
