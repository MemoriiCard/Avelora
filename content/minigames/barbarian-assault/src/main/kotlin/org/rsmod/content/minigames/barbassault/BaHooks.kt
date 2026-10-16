package org.rsmod.content.minigames.barbassault

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

class BaDeathHook @Inject constructor(private val service: BaService) : PlayerDeathHook {
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

class BaRespawnHook @Inject constructor(private val service: BaService) : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? = service.respawnCoords(player)
}

class BaMonsterHook @Inject constructor(private val service: BaService) :
    NpcAttackValidateHook, NpcDeathKillHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (service.isMonster(npc) && !service.isPlaying(player)) {
            NpcAttackValidateResult.Deny("That Penance isn't part of your run.")
        } else {
            NpcAttackValidateResult.Pass
        }

    override fun onKill(context: NpcDeathKillContext) {
        service.onMonsterKilled(context.npc, context.hero)
    }
}

class BaTeleportHook @Inject constructor(private val service: BaService) :
    PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (type != TeleportType.Exempt && service.isPlaying(player)) {
            "You can't teleport out of Barbarian Assault. Climb the ladder instead."
        } else {
            null
        }
}
