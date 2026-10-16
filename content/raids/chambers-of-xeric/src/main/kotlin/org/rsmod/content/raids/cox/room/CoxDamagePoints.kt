package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.entity.PlayerList

@Singleton
class CoxDamagePoints
@Inject
constructor(private val raids: CoxRaids, private val players: PlayerList) {
    fun award(event: NpcHitEvents.Impact, bonusPerDamage: Int = 0) {
        val hit = event.hit
        if (!hit.isFromPlayer || hit.damage <= 0) return
        val player = hit.resolvePlayerSource(players) ?: return
        val raid = raids.containing(player) ?: return
        val controller = raid.controllerOf(event.npc) ?: return
        if (!controller.awardsPoints(event.npc)) return
        raids.addPoints(raid, player, hit.damage * (controller.pointsPerDamage + bonusPerDamage))
    }
}
