package org.rsmod.content.minigames.lms

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class LmsDeathHook @Inject constructor(private val service: LmsService) : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!service.isPlaying(context.player)) return null
        service.onDeath(context.player, context.killer)
        return PlayerDeathHandling(
            keepCount = 0,
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.DESTROY,
        )
    }
}

class LmsRespawnHook @Inject constructor() : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? =
        if (LmsMap.inArena(player.coords)) LmsMap.LOBBY else null
}

class LmsTeleportHook @Inject constructor(private val service: LmsService) :
    PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (type != TeleportType.Exempt && service.isPlaying(player)) {
            "You can't teleport out of Last Man Standing."
        } else {
            null
        }
}
