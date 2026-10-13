package org.rsmod.content.minigames.colosseum

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PlayerSafeDeathHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

private val colosseumKey by lazy { InstanceSettingsRow.getRow(ColosseumInstance.SETTINGS_ROW).key }

internal class ColosseumRespawnHook
@Inject
constructor(private val instances: InstanceManager, private val runs: ColosseumRuns) :
    PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        val session = instances.sessionForPlayer(player)?.takeIf { it.key == colosseumKey } ?: return null
        runs.runFor(session)?.let { runs.forfeit(player, it) }
        return ColosseumArena.LOBBY
    }
}

internal class ColosseumSafeDeathHook @Inject constructor(private val instances: InstanceManager) :
    PlayerSafeDeathHook {
    override fun isSafeDeath(context: PlayerDeathContext): Boolean =
        instances.sessionForPlayer(context.player)?.key == colosseumKey
}
