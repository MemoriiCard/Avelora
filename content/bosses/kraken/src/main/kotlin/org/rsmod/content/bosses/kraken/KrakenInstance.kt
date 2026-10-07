package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.ScriptContext

class KrakenInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val players: PlayerList) :
    InstanceScript(registry) {

    override fun settingsRow(): String = SETTINGS_ROW

    override fun area(): InstanceArea = INSTANCE

    override fun destroyWhenEmpty(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterObject { if (canEnter()) telejump(KrakenCove.INSIDE_LAIR) }
        onOpLoc2(ENTRANCE) { if (canEnter()) defaultInstanceEntry() }
        onOpLoc3(ENTRANCE) { peek() }
        onExitObject {
            if (manager.sessionForPlayer(player) != null) defaultLeaveFlow() else telejump(KrakenCove.OUTSIDE_LAIR)
        }
        onOpLoc1(SURFACE_ENTRANCE) { telejump(KrakenCove.COVE) }
        onOpLoc1(COVE_EXIT) { telejump(KrakenCove.SURFACE) }
    }

    private fun ProtectedAccess.canEnter(): Boolean {
        if (player.statBase("stat.slayer") < KrakenCove.SLAYER_LEVEL) {
            mes("You need a Slayer level of ${KrakenCove.SLAYER_LEVEL} to enter.")
            return false
        }
        val task = SlayerTaskManager.getCurrentSlayerTask(this)
        val onTask = task != null && KRAKEN_TYPES.any { SlayerTaskManager.isTaskNpcType(it, task.id) }
        if (!onTask) {
            mes("You need to be on a Kraken or Cave kraken Slayer task to enter.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.peek() {
        val inside = players.count { manager.sessionForPlayer(it) == null && KrakenCove.inLair(it.coords) }
        mes(if (inside == 1) "You peek inside and see 1 adventurer." else "You peek inside and see $inside adventurers.")
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_kraken"
        private const val ENTRANCE = "loc.slayer_cave_kraken_boss_entrance"
        private const val SURFACE_ENTRANCE = "loc.slayer_cave_kraken_maincave_entrance"
        private const val COVE_EXIT = "loc.slayer_cave_kraken_maincave_exit"

        private val KRAKEN_TYPES by lazy {
            listOf(KRAKEN, CAVE_KRAKEN).mapNotNull { ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC)) }
        }

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(KrakenCove.REGION),
                npcSpawns =
                    KrakenCove.TENTACLE_WHIRLPOOLS.map { InstanceNpc(TENTACLE_WHIRLPOOL, it) } +
                        InstanceNpc(KRAKEN_WHIRLPOOL, KrakenCove.BOSS_WHIRLPOOL),
            )
    }
}
