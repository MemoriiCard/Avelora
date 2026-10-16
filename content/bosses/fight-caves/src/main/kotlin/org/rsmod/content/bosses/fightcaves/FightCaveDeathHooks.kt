package org.rsmod.content.bosses.fightcaves

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PlayerSafeDeathHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

private val fightCaveKey by lazy { InstanceSettingsRow.getRow(FightCaveInstance.SETTINGS_ROW).key }

internal class FightCaveRespawnHook
@Inject
constructor(private val instances: InstanceManager, private val runs: FightCaveRuns) : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        val session = instances.sessionForPlayer(player)?.takeIf { it.key == fightCaveKey } ?: return null
        runs.runFor(session)?.let { runs.forfeit(player, it, FightCaveArena.OUTSIDE) }
        return FightCaveArena.OUTSIDE
    }
}

internal class FightCaveSafeDeathHook @Inject constructor(private val instances: InstanceManager) : PlayerSafeDeathHook {
    override fun isSafeDeath(context: PlayerDeathContext): Boolean =
        instances.sessionForPlayer(context.player)?.key == fightCaveKey
}
