package org.rsmod.content.raids.toa.kephri

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

class KephriRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.Kephri, services, onCleared) {
    private enum class Stage {
        Scarabs,
        Children,
        Vulnerable,
    }

    private class Fireball(val tile: CoordGrid, val landsAt: Int)

    private class Egg(val tile: CoordGrid, val bursts: Int)

    private var kephri: Npc? = null
    private var stage = Stage.Scarabs
    private var shieldsDone = 0
    private val scarabs = mutableListOf<Npc>()
    private val guardians = mutableListOf<Npc>()
    private val fireballs = mutableListOf<Fireball>()
    private val eggs = mutableListOf<Egg>()
    private var enraged = false

    val shielded: Boolean
        get() = stage != Stage.Vulnerable

    fun incoming(damage: Int): Int = if (shielded) 0 else damage

    override fun begin() {
        val boss = spawn("npc.toa_kephri_boss_shielded", KephriRules.BOSS, scaledHp(KephriRules.BASE_HP), KephriRules.STAT)
        kephri = boss
        shieldsDone = 1
        beginShield()
        tell("Kephri raises her shield and the scarabs swarm.")
    }

    override fun tick() {
        val boss = kephri ?: return
        val percent = ToaCombat.percent(boss.hitpoints, boss.baseHitpointsLvl)
        if (stage == Stage.Vulnerable) {
            checkNextShield(boss, percent)
            if (!enraged && percent <= KephriRules.ENRAGE_PERCENT) {
                enraged = true
                tell("Kephri is enraged!")
            }
        }
        if (clock % KephriRules.attackRate(percent) == 0) fireball(boss)
        if (shielded) shieldHazards(boss)
        landFireballs()
        burstEggs()
        for (scarab in scarabs.toList()) chase(scarab, KephriRules.SCARAB_HIT, KephriRules.SCARAB_RATE)
        for (guardian in guardians.toList()) chase(guardian, KephriRules.GUARDIAN_HIT, KephriRules.GUARDIAN_RATE)
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        when {
            npc === kephri -> {
                clearHazards()
                tell("<col=ef1020>Kephri has been defeated!</col>")
                finish()
            }
            scarabs.any { it === npc } -> {
                scarabs.removeAll { it === npc }
                if (scarabs.isEmpty() && stage == Stage.Scarabs) summonChildren()
            }
            guardians.any { it === npc } -> {
                guardians.removeAll { it === npc }
                if (guardians.isEmpty() && stage == Stage.Children) breakShield()
            }
        }
    }

    override fun onNpcRemoved(npc: Npc) {
        scarabs.removeAll { it === npc }
        guardians.removeAll { it === npc }
        if (npc === kephri) kephri = null
    }

    private fun beginShield() {
        stage = Stage.Scarabs
        kephri?.let { transmog(it, "npc.toa_kephri_boss_shielded") }
        val count = KephriRules.shieldScarabs(teamSize)
        repeat(count) {
            val door = KephriRules.SCARAB_DOORS[it % KephriRules.SCARAB_DOORS.size]
            scarabs += spawn("npc.toa_kephri_shield_scarab", door, KephriRules.SCARAB_HP)
        }
        if (ToaInvocation.MoreOverlords in invocations) {
            scarabs += spawn("npc.toa_scabaras_scarab", KephriRules.SCARAB_DOORS.first(), KephriRules.SCARAB_HP * 2)
        }
    }

    private fun summonChildren() {
        stage = Stage.Children
        tell("The shield falls. Kephri calls her children!")
        for ((name, tile) in KephriRules.GUARDIANS) {
            guardians += spawn(name, tile, KephriRules.GUARDIAN_HP)
        }
    }

    private fun breakShield() {
        stage = Stage.Vulnerable
        kephri?.let { transmog(it, "npc.toa_kephri_boss_enrage") }
        tell("Kephri is exposed. Strike now!")
    }

    private fun checkNextShield(boss: Npc, percent: Int) {
        val next = KephriRules.nextShield(percent, shieldsDone) ?: return
        shieldsDone = next + 1
        beginShield()
        tell("Kephri raises her shield again.")
    }

    private fun fireball(boss: Npc) {
        val target = nearestPlayer(boss.coords) ?: return
        val tile = target.coords
        fireballs += Fireball(tile, clock + KephriRules.FIREBALL_DELAY)
    }

    private fun landFireballs() {
        val due = fireballs.filter { it.landsAt <= clock }
        if (due.isEmpty()) return
        fireballs.removeAll(due.toSet())
        for (ball in due) {
            for (player in playersInRoom()) {
                if (ball.tile.chebyshevDistance(player.coords) <= 1) {
                    val damage = scaledDamage(services.random.of(0, KephriRules.FIREBALL_MAX))
                    hurtStyled(player, ToaStyle.Magic, damage)
                }
            }
        }
    }

    private fun shieldHazards(boss: Npc) {
        if (clock % KephriRules.DUNG_EVERY == 0) {
            val targets = playersInRoom().shuffled().take(if (ToaInvocation.BlowingMud in invocations) 2 else 1)
            for (player in targets) hurt(player, scaledDamage(services.random.of(1, KephriRules.DUNG_MAX)))
        }
        if (clock % KephriRules.EGGS_EVERY == 0) layEggs()
    }

    private fun layEggs() {
        repeat(KephriRules.eggCount(ToaInvocation.LivelyLarvae in invocations)) {
            val tile =
                world(
                    CoordGrid(
                        KephriRules.ARENA_X.first + services.random.of(KephriRules.ARENA_X.count()),
                        KephriRules.ARENA_Z.first + services.random.of(KephriRules.ARENA_Z.count()),
                        0,
                    )
                )
            eggs += Egg(tile, clock + KephriRules.EGG_FUSE)
        }
        tell("Kephri lays eggs across the chamber.")
    }

    private fun burstEggs() {
        val due = eggs.filter { it.bursts <= clock }
        if (due.isEmpty()) return
        eggs.removeAll(due.toSet())
        for (egg in due) {
            for (player in playersInRoom()) {
                if (egg.tile.chebyshevDistance(player.coords) <= 1) {
                    hurt(player, scaledDamage(services.random.of(1, KephriRules.EGG_MAX)))
                }
            }
        }
    }

    private fun clearHazards() {
        fireballs.clear()
        eggs.clear()
        for (npc in scarabs.toList() + guardians.toList()) remove(npc)
        scarabs.clear()
        guardians.clear()
    }
}
