package org.rsmod.content.raids.toa.akkha

import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.toa.boss.ToaBossRoom
import org.rsmod.content.raids.toa.boss.ToaCombat
import org.rsmod.content.raids.toa.boss.ToaStyle
import org.rsmod.content.raids.toa.invocation.ToaInvocation
import org.rsmod.content.raids.toa.layout.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class AkkhaRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.Akkha, services, onCleared) {
    private class Orb(val tile: CoordGrid, val bursts: Int)

    private var akkha: Npc? = null
    private var attacks = 0
    private var specials = 0
    private var styleIndex = 0
    private var enraged = false
    private var detonateAt = -1
    private var marked = emptyList<Player>()
    private var blastAt = -1
    private var litQuadrants = emptySet<Int>()
    private var orbsUntil = -1
    private val orbs = mutableListOf<Orb>()
    private val lastTile = mutableMapOf<Player, CoordGrid>()

    private val currentStyle: ToaStyle
        get() = AkkhaRules.style(styleIndex)

    override fun begin() {
        akkha = spawn("npc.akkha_melee", AkkhaRules.BOSS, scaledHp(AkkhaRules.BASE_HP), AkkhaRules.STAT)
        tell("Akkha awakens.")
    }

    override fun tick() {
        val boss = akkha ?: return
        val percent = ToaCombat.percent(boss.hitpoints, boss.baseHitpointsLvl)
        if (AkkhaRules.shouldEnrage(percent, enraged)) enrage(boss)
        if (clock % AkkhaRules.ATTACK_RATE == 0 && detonateAt < 0 && blastAt < 0 && orbsUntil < 0) {
            attack(boss)
        }
        if (enraged && clock % AkkhaRules.ENRAGE_ORB_EVERY == 0) enrageOrbs()
        resolveDetonate()
        resolveBlast()
        trailOrbs()
        burstOrbs()
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc !== akkha) return
        orbs.clear()
        tell("<col=ef1020>Akkha has been defeated!</col>")
        finish()
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === akkha) akkha = null
    }

    private fun enrage(boss: Npc) {
        enraged = true
        transmog(boss, "npc.akkha_enrage")
        tell("Akkha is enraged, shrouded in white light!")
    }

    private fun attack(boss: Npc) {
        if (AkkhaRules.isSpecialTurn(attacks)) {
            special(boss)
            attacks++
            switchStyle()
            return
        }
        attacks++
        val style = currentStyle
        val targets =
            if (style == ToaStyle.Melee) listOfNotNull(nearestPlayer(boss.coords))
            else playersInRoom()
        for (player in targets) {
            val damage = scaledDamage(services.random.of(0, AkkhaRules.MAX_HIT))
            hurtStyled(player, style, damage)
        }
        if (ToaInvocation.StayVigilant in invocations && services.random.of(AkkhaRules.STAY_VIGILANT_SWITCH) == 0) {
            switchStyle()
        }
    }

    private fun switchStyle() {
        styleIndex++
        tell("Akkha shifts to ${currentStyle.name.lowercase()}.")
    }

    private fun special(boss: Npc) {
        val kind = AkkhaRules.special(specials++, teamSize)
        if (ToaInvocation.DoubleTrouble in invocations && services.random.of(2) == 0) {
            startSpecial(AkkhaRules.special(specials++, teamSize))
        }
        startSpecial(kind)
    }

    private fun startSpecial(kind: AkkhaRules.Special) {
        when (kind) {
            AkkhaRules.Special.Detonate -> {
                marked = playersInRoom()
                detonateAt = clock + AkkhaRules.DETONATE_DELAY
                tell("Akkha marks you. Do not share a row or column!")
            }
            AkkhaRules.Special.MemoryBlast -> {
                litQuadrants = setOf(services.random.of(4), services.random.of(4))
                blastAt = clock + AkkhaRules.BLAST_DELAY
                tell("The chamber lights up. Stand in a glowing quarter!")
            }
            AkkhaRules.Special.TrailingOrbs -> {
                orbsUntil = clock + AkkhaRules.ORB_SPECIAL_TICKS
                lastTile.clear()
                for (player in playersInRoom()) lastTile[player] = player.coords
                tell("Orbs gather in your footsteps. Hold still!")
            }
        }
    }

    private fun resolveDetonate() {
        if (detonateAt < 0 || clock < detonateAt) return
        detonateAt = -1
        for (victim in marked) {
            if (victim.hitpoints <= 0) continue
            val shared = marked.any { it !== victim && AkkhaRules.sharesLine(victim.coords, it.coords) }
            if (shared) hurt(victim, scaledDamage(services.random.of(AkkhaRules.DETONATE_MAX / 2, AkkhaRules.DETONATE_MAX)))
        }
        marked = emptyList()
    }

    private fun resolveBlast() {
        if (blastAt < 0 || clock < blastAt) return
        blastAt = -1
        val centre = world(AkkhaRules.CENTER)
        for (player in playersInRoom()) {
            val quarter = AkkhaRules.quadrant(player.coords.x, player.coords.z, centre.x, centre.z)
            if (quarter !in litQuadrants) {
                hurt(player, scaledDamage(services.random.of(AkkhaRules.BLAST_MAX / 2, AkkhaRules.BLAST_MAX)))
            }
        }
        litQuadrants = emptySet()
    }

    private fun trailOrbs() {
        if (orbsUntil < 0) return
        if (clock >= orbsUntil) {
            orbsUntil = -1
            lastTile.clear()
            return
        }
        for (player in playersInRoom()) {
            val previous = lastTile[player]
            if (previous != null && previous != player.coords) {
                orbs += Orb(previous, clock + AkkhaRules.ORB_FUSE)
            }
            lastTile[player] = player.coords
        }
    }

    private fun burstOrbs() {
        val due = orbs.filter { it.bursts <= clock }
        if (due.isEmpty()) return
        orbs.removeAll(due.toSet())
        for (orb in due) {
            for (player in playersInRoom()) {
                if (player.coords == orb.tile) {
                    hurt(player, scaledDamage(services.random.of(1, AkkhaRules.ORB_MAX)))
                }
            }
        }
    }

    private fun enrageOrbs() {
        for (player in playersInRoom()) {
            val damage = scaledDamage(services.random.of(1, AkkhaRules.ENRAGE_ORB_MAX))
            if (level < AkkhaRules.PRAYER_PIERCE_LEVEL) hurtStyled(player, ToaStyle.Magic, damage)
            else hurt(player, damage)
        }
    }
}
