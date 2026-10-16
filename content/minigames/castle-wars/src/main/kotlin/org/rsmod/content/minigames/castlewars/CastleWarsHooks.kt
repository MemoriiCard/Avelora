package org.rsmod.content.minigames.castlewars

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPAttackValidateResult
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class CastleWarsDeathHook @Inject constructor(private val service: CastleWarsService) :
    PlayerDeathHook, PlayerDeathCleanupHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!service.isPlaying(context.player)) return null
        return PlayerDeathHandling(
            keepCount = KEEP_EVERYTHING,
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )
    }

    override fun cleanup(player: Player) {
        if (service.isPlaying(player)) service.onDeath(player)
    }

    private companion object {
        private const val KEEP_EVERYTHING = 100
    }
}

class CastleWarsRespawnHook @Inject constructor(private val service: CastleWarsService) :
    PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        val team = service.teamOf(player)?.takeIf { service.isPlaying(player) } ?: return null
        return CastleWarsMap.SPAWN.getValue(team)
    }
}

class CastleWarsPvPHook @Inject constructor(private val service: CastleWarsService) :
    PvPAttackValidateHook {
    override fun validate(attacker: Player, target: Player): PvPAttackValidateResult {
        val mine = service.isPlaying(attacker)
        val theirs = service.isPlaying(target)
        return when {
            !mine && !theirs -> PvPAttackValidateResult.Pass
            mine != theirs -> PvPAttackValidateResult.Deny("That player is not in your game.")
            service.teamOf(attacker) == service.teamOf(target) ->
                PvPAttackValidateResult.Deny("You can't attack your own team.")
            else -> PvPAttackValidateResult.Pass
        }
    }
}

class CastleWarsTeleportHook @Inject constructor() : PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (type != TeleportType.Exempt && CastleWarsMap.inside(player.coords)) {
            "You can't teleport out of Castle Wars. Use the exit portal instead."
        } else {
            null
        }
}
