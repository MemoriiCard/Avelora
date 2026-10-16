package org.rsmod.content.bosses.inferno

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PlayerSafeDeathHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

private val infernoKey by lazy { InstanceSettingsRow.getRow(InfernoInstance.SETTINGS_ROW).key }

internal class InfernoRespawnHook
@Inject
constructor(private val instances: InstanceManager, private val runs: InfernoRuns) : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        val session = instances.sessionForPlayer(player)?.takeIf { it.key == infernoKey } ?: return null
        runs.runFor(session)?.let { runs.forfeit(player, it, InfernoArena.OUTSIDE) }
        return InfernoArena.OUTSIDE
    }
}

internal class InfernoSafeDeathHook @Inject constructor(private val instances: InstanceManager) : PlayerSafeDeathHook {
    override fun isSafeDeath(context: PlayerDeathContext): Boolean =
        instances.sessionForPlayer(context.player)?.key == infernoKey
}
