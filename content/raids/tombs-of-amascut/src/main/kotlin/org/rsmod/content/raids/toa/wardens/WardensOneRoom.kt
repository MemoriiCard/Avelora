package org.rsmod.content.raids.toa.wardens

import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class WardensOneRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    WardensBase(raid, ToaRoom.WardensOne, services, onCleared) {
    private enum class Stage {
        Obelisk,
        Warden,
        Core,
    }

    private var stage = Stage.Obelisk
    private var obelisk: Npc? = null
    private var warden: Npc? = null
    private var core: Npc? = null
    private val bystanders = mutableListOf<Npc>()
    private var ejections = 0
    private var attacks = 0
    private var coreExpiresAt = 0
    private var regrowAt = -1
    private var coreAt: CoordGrid = WardensRules.P2_WARDEN

    override fun begin() {
        obelisk =
            spawn("npc.toa_wardens_p1_obelisk_npc", WardensRules.P1_OBELISK, scaledHp(WardensRules.OBELISK_HP), WardensRules.OBELISK_STAT)
        bystanders += spawn("npc.toa_warden_elidinis_phase1_inactive", WardensRules.P1_ELIDINIS, 1)
        bystanders += spawn("npc.toa_warden_tumeken_phase1_inactive", WardensRules.P1_TUMEKEN, 1)
        tell("The obelisk hums as it channels power into the Wardens. Destroy it!")
    }

    override fun tick() {
        landStrikes()
        when (stage) {
            Stage.Obelisk -> if (clock % WardensRules.BEAM_EVERY == 0) beam()
            Stage.Warden -> wardenTick()
            Stage.Core -> coreTick()
        }
        if (regrowAt in 0..clock) {
            regrowAt = -1
            spawnWarden()
        }
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        when {
            npc === obelisk -> obeliskDown()
            npc === warden -> wardenDown(npc)
            npc === core -> coreDown()
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === obelisk) obelisk = null
        if (npc === warden) warden = null
        if (npc === core) core = null
    }

    private fun beam() {
        val players = playersInRoom()
        if (players.isEmpty()) return
        repeat(WardensRules.beamTargets(teamSize)) {
            queueStrike(players[services.random.of(players.size)].coords, WardensRules.BEAM_MAX)
        }
        tell("The obelisk charges a converging beam. Move!")
    }

    private fun obeliskDown() {
        clearStrikes()
        for (npc in bystanders.toList()) remove(npc)
        bystanders.clear()
        stage = Stage.Warden
        tell("The obelisk shatters. Elidinis' Warden awakens!")
        spawnWarden()
    }

    private fun spawnWarden() {
        attacks = 0
        warden =
            spawn("npc.toa_warden_elidinis_phase2_mage", WardensRules.P2_WARDEN, scaledHp(WardensRules.ELIDINIS_HP), WardensRules.ELIDINIS_STAT)
        stage = Stage.Warden
    }

    private fun wardenTick() {
        val boss = warden ?: return
        if (clock % WardensRules.P2_ATTACK_RATE != 0) return
        val target = nearestPlayer(boss.coords) ?: return
        val number = attacks++
        val style = WardensRules.p2Style(number)
        transmog(boss, if (number % 2 == 0) "npc.toa_warden_elidinis_phase2_mage" else "npc.toa_warden_elidinis_phase2_range")
        hurtStyled(target, style, scaledDamage(services.random.of(0, WardensRules.P2_MAX_HIT)))
        if (WardensRules.isP2Lightning(number)) {
            for (player in playersInRoom()) queueStrike(player.coords, WardensRules.P2_LIGHTNING_MAX)
            tell("Lightning crackles overhead. Do not stand still!")
        }
    }

    private fun wardenDown(npc: Npc) {
        coreAt = raid.source(npc.coords) ?: WardensRules.P2_WARDEN
        stage = Stage.Core
        coreExpiresAt = clock + WardensRules.coreWindow(ejections)
        core = spawn("npc.toa_warden_elidinis_core", coreAt, 1)
        tell("The Warden's core is ejected. Strike it before it recovers!")
    }

    private fun coreTick() {
        val current = core ?: return
        if (clock < coreExpiresAt) return
        remove(current)
        core = null
        stage = Stage.Warden
        tell("The core retreats and the Warden recovers.")
        regrowAt = clock + WardensRules.REGROW_DELAY
    }

    private fun coreDown() {
        ejections++
        if (ejections >= WardensRules.EJECTIONS) {
            clearStrikes()
            tell("<col=ef1020>The Warden's core has been shattered!</col>")
            finish()
            return
        }
        tell("The core cracks. The Warden reforms, stronger than before (${ejections}/${WardensRules.EJECTIONS}).")
        stage = Stage.Warden
        regrowAt = clock + WardensRules.REGROW_DELAY
    }
}
