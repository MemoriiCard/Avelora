package org.rsmod.content.minigames.barbassault

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaGambleScript
@Inject
constructor(
    private val service: BaService,
    private val objs: ObjRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(COMMANDER) { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(
                neutral,
                "You have ${service.honour(player)} honour points. Care to gamble some of them?",
            )
            val tier =
                choice4(
                    GambleTier.Low.label,
                    GambleTier.Low,
                    GambleTier.Medium.label,
                    GambleTier.Medium,
                    GambleTier.High.label,
                    GambleTier.High,
                    "Not now",
                    null,
                ) ?: return@startDialogue
            val problem =
                BaGamble.available(
                    tier,
                    service.honour(player),
                    service.gambleProgress(player),
                    service.queenKills(player),
                )
            if (problem != null) {
                chatNpc(neutral, problem)
                return@startDialogue
            }
            if (!service.spendHonour(player, tier.cost)) return@startDialogue
            service.consumeGamble(player, tier)
            val result = BaGamble.roll(tier) { random.of(it) }
            player.invAddOrDrop(objs, symbolOf(result.item), result.quantity)
            val name = ServerCacheManager.getItem(result.item.obj.asRSCM(RSCMType.OBJ))?.name ?: "reward"
            player.mes("You receive ${result.quantity} x $name.")
        }
    }

    private fun symbolOf(item: GambleItem): String {
        if (!item.noted) return item.obj
        val base = ServerCacheManager.getItem(item.obj.asRSCM(RSCMType.OBJ)) ?: return item.obj
        return if (base.certlink > 0) RSCM.getReverseMapping(RSCMType.OBJ, base.certlink) else item.obj
    }

    companion object {
        const val COMMANDER = "npc.barbassault_barbarian_commander"
    }
}
