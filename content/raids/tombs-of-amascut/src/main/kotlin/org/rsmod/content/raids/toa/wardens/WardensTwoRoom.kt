package org.rsmod.content.raids.toa.wardens

import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaCombat
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class WardensTwoRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    WardensBase(raid, ToaRoom.WardensTwo, services, onCleared) {
    private class Phantom(val npc: Npc, val expiresAt: Int)

    private var warden: Npc? = null
    private var tumeken: Npc? = null
    private var attacks = 0
    private var specials = 0
    private var enraged = false
    private val phantoms = mutableListOf<Phantom>()

    override fun begin() {
        warden =
            spawn("npc.toa_warden_elidinis_phase3", WardensRules.P3_WARDEN, scaledHp(WardensRules.P3_HP), WardensRules.P3_STAT)
        tumeken = spawn("npc.toa_warden_tumeken_phase3_inactive", WardensRules.P3_TUMEKEN, 1)
        tell("The Wardens fuse into a single, furious being.")
    }

    override fun tick() {
        val boss = warden ?: return
        landStrikes()
        val percent = ToaCombat.percent(boss.hitpoints, boss.baseHitpointsLvl)
        if (WardensRules.shouldEnrage(percent, enraged)) enrage(boss)
        if (clock % WardensRules.P3_ATTACK_RATE == 0) attack(boss)
        for (phantom in phantoms.toList()) {
            if (clock >= phantom.expiresAt) {
                phantoms.remove(phantom)
                remove(phantom.npc)
            } else {
                chase(phantom.npc, WardensRules.PHANTOM_HIT, WardensRules.PHANTOM_RATE)
            }
        }
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc === warden) {
            clearStrikes()
            for (phantom in phantoms.toList()) remove(phantom.npc)
            phantoms.clear()
            tumeken?.let { remove(it) }
            tumeken = null
            tell("<col=ef1020>The Wardens have been defeated!</col>")
            finish()
        } else {
            phantoms.removeAll { it.npc === npc }
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === warden) warden = null
        if (npc === tumeken) tumeken = null
        phantoms.removeAll { it.npc === npc }
    }

    private fun enrage(boss: Npc) {
        enraged = true
        heal(boss, WardensRules.enrageHeal(boss.baseHitpointsLvl))
        tell("<col=ff0000>The Wardens are enraged! The ground itself turns against you.</col>")
    }

    private fun attack(boss: Npc) {
        val number = attacks++
        val target = nearestPlayer(boss.coords)
        if (target != null) {
            hurtStyled(target, WardensRules.p3Style(number), scaledDamage(services.random.of(0, WardensRules.P3_MAX_HIT)))
        }
        if (WardensRules.isP3Special(number, enraged)) special(boss, WardensRules.p3Special(specials++, enraged))
    }

    private fun special(boss: Npc, kind: WardensRules.Special) {
        when (kind) {
            WardensRules.Special.Lightning -> lightning()
            WardensRules.Special.Phantoms -> summonPhantoms()
            WardensRules.Special.Siphon -> siphon(boss)
        }
    }

    private fun lightning() {
        for (player in playersInRoom()) queueStrike(player.coords, WardensRules.LIGHTNING_MAX)
        tell("Red lightning gathers on the floor beneath you!")
    }

    private fun summonPhantoms() {
        if (phantoms.isNotEmpty()) return
        val expiry = clock + WardensRules.PHANTOM_LIFETIME
        val spots = listOf(WardensRules.P3_PHANTOM_A, WardensRules.P3_PHANTOM_B)
        for (spot in spots) {
            val npc = spawn("npc.toa_kephri_scarab_rangekite", spot, scaledHp(WardensRules.PHANTOM_HP))
            phantoms += Phantom(npc, expiry)
        }
        tell("Phantoms of the fallen guardians rise to protect the Wardens!")
    }

    private fun siphon(boss: Npc) {
        val players = playersInRoom()
        for (player in players) player.statSub("stat.prayer", WardensRules.SIPHON_DRAIN, 0)
        heal(boss, WardensRules.siphonHeal(boss.baseHitpointsLvl, players.size))
        tell("The Wardens siphon your energy and heal.")
    }
}
