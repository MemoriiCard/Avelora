package org.rsmod.content.bosses.smokedevil

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SmokeDevilSmoke
@Inject
constructor(private val npcList: NpcList, private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onArea(SmokeDevilDungeon.AREA) { startSmoke() }
        onAreaExit(SmokeDevilDungeon.AREA) { clearQueue(SMOKE_QUEUE) }
        onPlayerQueue(SMOKE_QUEUE) { smoke() }
        onEvent<PlayerHitEvents.Impact> { if (isSmokeDevilHit()) inhale(player) }
    }

    private fun ProtectedAccess.startSmoke() {
        clearQueue(SMOKE_QUEUE)
        player.queue(SMOKE_QUEUE, SMOKE_INTERVAL)
    }

    private fun ProtectedAccess.smoke() {
        if (!inArea(SmokeDevilDungeon.AREA, player.coords)) return
        player.queue(SMOKE_QUEUE, SMOKE_INTERVAL)
        if (SmokeProtection.isProtected(player)) return
        val damage = minOf(random.of(1, SMOKE_MAX_DAMAGE), player.stat(HITPOINTS) - 1)
        if (damage <= 0) {
            say("*wheeze*")
            return
        }
        say("*cough*")
        queueHit(delay = 1, type = HitType.Typeless, damage = damage)
    }

    private fun PlayerHitEvents.Impact.isSmokeDevilHit(): Boolean {
        if (!hit.isFromNpc) return false
        val source = hit.resolveNpcSource(npcList) ?: return false
        return source.visType.internalName in SMOKE_DEVILS
    }

    private fun inhale(player: Player) {
        if (SmokeProtection.isProtected(player)) return
        for (stat in INHALE_DRAINED) {
            val drain = player.stat(stat) - 1
            if (drain > 0) player.statSub(stat, drain, 0)
        }
        player.mes("You inhale the smoke and feel your strength drain away!")
    }

    private companion object {
        const val SMOKE_QUEUE = "queue.smoke_devil_dungeon_smoke"
        const val SMOKE_INTERVAL = 10
        const val SMOKE_MAX_DAMAGE = 5
        const val HITPOINTS = "stat.hitpoints"
        val SMOKE_DEVILS = setOf(SMOKE_DEVIL, SUPERIOR_SMOKE_DEVIL, THERMY)
        val INHALE_DRAINED =
            listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic", "stat.agility")
    }
}
