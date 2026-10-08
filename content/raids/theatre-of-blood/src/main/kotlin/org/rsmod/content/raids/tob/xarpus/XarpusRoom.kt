package org.rsmod.content.raids.tob.xarpus

import org.rsmod.api.player.hit.queueHit
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.boss.TobBossRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class XarpusRoom(
    raid: TobRaid,
    services: CoxRoomServices,
    onCleared: () -> Unit,
) : TobBossRoom(raid, TobRoom.Xarpus, services, onCleared) {
    private enum class Phase {
        Recovery,
        Poison,
        Counter,
    }

    private class Remains(val tile: CoordGrid, val until: Int, var blocked: Boolean = false)

    private var boss: Npc? = null
    private var phase = Phase.Recovery
    private var ticks = 0
    private var spawned = 0
    private var stacks = 0
    private var facing = Quadrant.North
    private var facingUntil = 0
    private val remains = mutableListOf<Remains>()
    private val acid = mutableSetOf<CoordGrid>()

    val inRecovery: Boolean
        get() = phase == Phase.Recovery

    override fun begin() {
        val max = TobScaling.hitpoints(XarpusRules.BASE_HP, teamSize, mode)
        boss = spawn("npc.tob_xarpus_combat$suffix", XarpusRules.SPAWN, max, stats = BOSS_STAT)
        boss?.hitpoints = max * XarpusRules.START_PERCENT / 100
        tell("The ground trembles as Xarpus rises.")
    }

    override fun tick() {
        val target = boss ?: return
        ticks++
        when (phase) {
            Phase.Recovery -> recovery(target)
            Phase.Poison -> poison(target)
            Phase.Counter -> counter(target)
        }
        acidDamage()
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc !== boss) return
        tell("<col=ef1020>Xarpus has been defeated!</col>")
        finish()
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === boss) boss = null
    }

    /** Returns the damage the attacker deals, applying the phase 3 counter when they hit from the faced side. */
    fun incoming(attacker: Player, damage: Int): Int {
        val target = boss ?: return damage
        return when (phase) {
            Phase.Recovery -> 0
            Phase.Poison -> damage
            Phase.Counter -> {
                if (damage > 0 && XarpusRules.quadrantOf(centre(target), attacker.coords) == facing) {
                    hurt(attacker, XarpusRules.counterDamage(services.random.of(26), stacks))
                }
                damage
            }
        }
    }

    private fun recovery(target: Npc) {
        if (spawned < XarpusRules.REMAINS && ticks % XarpusRules.REMAINS_RATE == 0) {
            val tile = world(randomTile())
            services.spotanimAt("spotanim.tob_xarpus_exhumed_energyorb", tile)
            remains += Remains(tile, clock + XarpusRules.REMAINS_TICKS)
            spawned++
        }
        for (pile in remains.toList()) {
            if (playersInRoom().any { it.coords == pile.tile }) pile.blocked = true
            if (clock < pile.until) continue
            remains.remove(pile)
            if (!pile.blocked) {
                stacks++
                heal(target, XarpusRules.healEach(target.baseHitpointsLvl))
            }
        }
        if (spawned >= XarpusRules.REMAINS && remains.isEmpty()) {
            phase = Phase.Poison
            tell("Xarpus's attention turns to you.")
        }
    }

    private fun poison(target: Npc) {
        val percent = XarpusRules.hpPercent(target.hitpoints, target.baseHitpointsLvl)
        if (percent <= XarpusRules.COUNTER_PERCENT) {
            phase = Phase.Counter
            facingUntil = 0
            tell("<col=ef1020>Xarpus lets out a screech and stops spitting.</col>")
            return
        }
        if (ticks % XarpusRules.POISON_RATE != 0) return
        val victim = playersInRoom().randomOrNull() ?: return
        target.anim("seq.tob_xarpus_attack_ranged")
        victim.queueHit(
            2,
            HitType.Typeless,
            services.random.of(0, XarpusRules.AUTO_MAX),
            services.boss.playerHitModifier,
        )
        services.lob("spotanim.tob_xarpus_acidspit", centre(target), victim.coords) {
            for (tile in XarpusRules.acidTiles(victim.coords)) {
                acid += tile
                services.spotanimAt("spotanim.tob_xarpus_acidsplash", tile)
            }
        }
    }

    private fun counter(target: Npc) {
        if (clock >= facingUntil) {
            facing = Quadrant.entries[services.random.of(Quadrant.entries.size)]
            facingUntil = clock + XarpusRules.QUADRANT_TICKS
            tell("Xarpus stares ${facing.name.lowercase()}.")
        }
    }

    private fun acidDamage() {
        val target = boss ?: return
        for (player in playersInRoom()) {
            if (player.coords in acid) {
                hurt(player, XarpusRules.acidDamage(services.random.of(5), stacks))
            }
            if (phase != Phase.Recovery && XarpusRules.under(target.coords, player.coords)) {
                hurt(player, 1 + services.random.of(XarpusRules.UNDER_MAX))
            }
        }
    }

    private fun randomTile(): CoordGrid =
        CoordGrid(
            XarpusRules.ARENA_X.first + services.random.of(XarpusRules.ARENA_X.count()),
            XarpusRules.ARENA_Z.first + services.random.of(XarpusRules.ARENA_Z.count()),
            XarpusRules.CENTRE.level,
        )

    private fun centre(target: Npc): CoordGrid =
        target.coords.translate(XarpusRules.SIZE / 2, XarpusRules.SIZE / 2)

    private companion object {
        const val BOSS_STAT = 220
    }
}
