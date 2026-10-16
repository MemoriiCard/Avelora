package org.rsmod.content.minigames.soulwars

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
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPAttackValidateResult
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class SoulWarsDeathHook @Inject constructor(private val service: SoulWarsService) :
    PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!service.isPlaying(context.player)) return null
        context.killer?.let { service.onPlayerKill(it, context.player) }
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

class SoulWarsRespawnHook @Inject constructor(private val service: SoulWarsService) :
    PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        val team = service.teamOf(player)?.takeIf { service.isPlaying(player) } ?: return null
        return SoulWarsMap.GRAVEYARD.getValue(team)
    }
}

class SoulWarsPvPHook @Inject constructor(private val service: SoulWarsService) :
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

class SoulWarsAvatarHook @Inject constructor(private val service: SoulWarsService) :
    NpcAttackValidateHook, NpcDeathKillHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        val avatarTeam = service.teamOfAvatar(npc) ?: return NpcAttackValidateResult.Pass
        val team = service.teamOf(player)?.takeIf { service.isPlaying(player) }
        return if (team == null || team == avatarTeam) {
            NpcAttackValidateResult.Deny("You can only attack the enemy avatar.")
        } else {
            NpcAttackValidateResult.Pass
        }
    }

    override fun onKill(context: NpcDeathKillContext) {
        service.teamOfAvatar(context.npc)?.let { service.onAvatarKilled(it) }
    }
}

class SoulWarsTeleportHook @Inject constructor(private val service: SoulWarsService) :
    PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (type != TeleportType.Exempt && service.isPlaying(player)) {
            "You can't teleport out of Soul Wars. Use the exit portal instead."
        } else {
            null
        }
}
