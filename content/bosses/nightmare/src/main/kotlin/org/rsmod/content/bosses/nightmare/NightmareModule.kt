package org.rsmod.content.bosses.nightmare

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

public class NightmareModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(NightmareTotemHook::class.java)
    }
}

internal class NightmareTotemHook @Inject constructor(private val fight: NightmareFight) : NpcAttackValidateHook {
    private val totemIds by lazy {
        NightmareTotem.entries.flatMap { listOf(it.dormant, it.ready, it.charged) }.map { it.asRSCM(RSCMType.NPC) }.toSet()
    }

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc.id !in totemIds || !fight.isTotemShut(npc)) return NpcAttackValidateResult.Pass
        return NpcAttackValidateResult.Deny("The totem can't be charged right now.")
    }
}
