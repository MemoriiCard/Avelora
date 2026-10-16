package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class ToaSafeDeathHook @Inject constructor(private val raids: ToaRaids) : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        raids.containing(context.player) ?: return null
        return PlayerDeathHandling(
            keepCount = Int.MAX_VALUE,
            dropReceiver = context.player,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )
    }
}

class ToaRespawnHook @Inject constructor(private val raids: ToaRaids) : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        val raid = raids.containing(player) ?: return null
        return raids.respawnCoords(raid)
    }
}

class ToaDeathCleanupHook @Inject constructor(private val raids: ToaRaids) :
    PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        val raid = raids.containing(player) ?: return
        raids.onDeath(raid, player)
    }
}
