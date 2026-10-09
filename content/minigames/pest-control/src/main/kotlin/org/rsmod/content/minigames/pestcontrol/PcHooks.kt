package org.rsmod.content.minigames.pestcontrol

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class PcDeathHook @Inject constructor(private val service: PcService) : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!service.isPlaying(context.player)) return null
        service.onPlayerDeath(context.player)
        return PlayerDeathHandling(
            keepCount = KEEP_EVERYTHING,
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )
    }

    private companion object {
        private const val KEEP_EVERYTHING = 100
    }
}

class PcRespawnHook @Inject constructor(private val service: PcService) : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? = service.respawnCoords(player)
}

class PcNpcHook @Inject constructor(private val service: PcService) :
    NpcAttackValidateHook, NpcDeathKillHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        when {
            service.isKnight(npc) -> NpcAttackValidateResult.Deny("You can't attack a Void Knight.")
            (service.portalIndex(npc) >= 0 || service.isPest(npc)) && !service.isPlaying(player) ->
                NpcAttackValidateResult.Deny("That isn't part of your game.")
            else -> NpcAttackValidateResult.Pass
        }

    override fun onKill(context: NpcDeathKillContext) {
        service.onNpcKilled(context.npc, context.hero)
    }
}

class PcTeleportHook @Inject constructor(private val service: PcService) :
    PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (type != TeleportType.Exempt && service.isPlaying(player)) {
            "You can't teleport out of Pest Control."
        } else {
            null
        }
}
