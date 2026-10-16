package org.rsmod.content.raids.tob.verzik

import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.raids.cox.room.CoxRoomServices
import org.rsmod.content.raids.tob.boss.TobBossRoom
import org.rsmod.content.raids.tob.layout.TobRoom
import org.rsmod.content.raids.tob.party.TobMode
import org.rsmod.content.raids.tob.party.TobScaling
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

private var Player.protectFromMagic by intVarBit("varbit.prayer_protectfrommagic")

class VerzikRoom(
    raid: TobRaid,
    services: CoxRoomServices,
    onCleared: () -> Unit,
) : TobBossRoom(raid, TobRoom.Verzik, services, onCleared) {
    private enum class Phase {
        Throne,
        Flight,
        Spider,
        Dead,
    }

    private class Pillar(val npc: Npc, val tile: CoordGrid, var hp: Int)

    private class Web(val npc: Npc, val victim: Player, val explodesAt: Int)

    private class Athanatos(val npc: Npc, val diesAt: Int)

    private var boss: Npc? = null
    private var phase = Phase.Throne
    private var phaseTicks = 0
    private var pendingPhase = false
    private var autos = 0
    private var specials = 0
    private var nextPrimary = 0
    private val pillars = mutableListOf<Pillar>()
    private val nylocas = mutableListOf<Npc>()
    private val athanatos = mutableListOf<Athanatos>()
    private val webs = mutableListOf<Web>()

    override fun begin() {
        val hp = TobScaling.hitpoints(VerzikRules.PHASE1_HP, teamSize, mode)
        boss = spawn("npc.verzik_phase1$suffix", VerzikRules.THRONE, hp, stats = BOSS_STAT)
        for (tile in VerzikRules.PILLARS) {
            val npc = spawn(pillarType(), tile, PILLAR_HP, stats = 1)
            pillars += Pillar(npc, tile, PILLAR_HP)
        }
        tell("Verzik Vitur rises from her throne.")
    }

    override fun tick() {
        val target = boss ?: return
        phaseTicks++
        if (pendingPhase) {
            pendingPhase = false
            advancePhase(target)
            return
        }
        when (phase) {
            Phase.Throne -> throne(target)
            Phase.Flight -> flight(target)
            Phase.Spider -> spider(target)
            Phase.Dead -> {}
        }
        tickWebs()
        tickAthanatos(target)
        tickNylocas()
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc === boss && phase == Phase.Spider) {
            phase = Phase.Dead
            clearHelpers()
            tell("<col=ef1020>Verzik Vitur has been defeated!</col>")
            finish()
            return
        }
        nylocas.removeAll { it === npc }
        athanatos.removeAll { it.npc === npc }
        webs.removeAll { it.npc === npc }
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === boss) boss = null
        nylocas.removeAll { it === npc }
        athanatos.removeAll { it.npc === npc }
        webs.removeAll { it.npc === npc }
        pillars.removeAll { it.npc === npc }
    }

    /** Caps hits on the first two forms so they hand over instead of dying. */
    fun incoming(npc: Npc, damage: Int): Int {
        if (npc !== boss || phase == Phase.Spider || phase == Phase.Dead) return damage
        val dealt = damage.coerceAtMost((npc.hitpoints - 1).coerceAtLeast(0))
        if (npc.hitpoints - dealt <= 1) pendingPhase = true
        return dealt
    }

    private fun throne(target: Npc) {
        if (phaseTicks % VerzikRules.P1_RATE != 0) return
        target.anim("seq.tob_sotetseg_attack_ranged")
        val live = pillars.map { it.tile }
        val max = TobScaling.damage(VerzikRules.P1_MAX, mode)
        for (player in playersInRoom()) {
            if (VerzikRules.blocked(centre(target), player.coords, live.map { world(it) })) continue
            val damage = services.random.of(0, max)
            hurt(player, VerzikRules.protectedDamage(damage, player.protectFromMagic != 0))
        }
        repeat(VerzikRules.PILLAR_HITS) { hitPillar() }
    }

    private fun hitPillar() {
        val pillar = pillars.randomOrNull() ?: return
        pillar.hp -= VerzikRules.pillarHit(services.random.of(31))
        if (pillar.hp <= 0) collapse(pillar)
    }

    private fun collapse(pillar: Pillar) {
        pillars.remove(pillar)
        val at = world(pillar.tile)
        services.spotanimAt("spotanim.tob_bloat_blood_splat", at)
        for (player in playersInRoom()) {
            if (player.coords.chebyshevDistance(at) <= VerzikRules.COLLAPSE_RADIUS) {
                hurt(player, services.random.of(0, TobScaling.damage(VerzikRules.COLLAPSE_MAX, mode)))
            }
        }
        remove(pillar.npc)
    }

    private fun advancePhase(target: Npc) {
        phaseTicks = 0
        autos = 0
        specials = 0
        when (phase) {
            Phase.Throne -> {
                for (pillar in pillars.toList()) collapse(pillar)
                val hp = TobScaling.hitpoints(VerzikRules.PHASE2_HP, teamSize, mode)
                transmog(target, "npc.verzik_phase2$suffix")
                target.baseHitpointsLvl = hp
                target.hitpoints = hp
                target.teleport(services.collision, world(VerzikRules.PHASE2_SPOT))
                phase = Phase.Flight
                tell("<col=ef1020>Verzik takes flight!</col>")
            }
            Phase.Flight -> {
                clearHelpers()
                val hp = TobScaling.hitpoints(VerzikRules.PHASE3_HP, teamSize, mode)
                transmog(target, "npc.verzik_phase3$suffix")
                target.baseHitpointsLvl = hp
                target.hitpoints = hp
                target.teleport(services.collision, world(VerzikRules.PHASE3_SPOT))
                phase = Phase.Spider
                tell("<col=ef1020>Verzik reveals her true form!</col>")
            }
            else -> {}
        }
    }

    private fun flight(target: Npc) {
        val percent = VerzikRules.hpPercent(target.hitpoints, target.baseHitpointsLvl)
        if (phaseTicks % VerzikRules.P2_RATE == 0) {
            val victim = nearestPlayer(centre(target)) ?: return
            val ranged = services.random.of(2) == 0
            val damage = services.random.of(0, TobScaling.damage(VerzikRules.P2_MAX, mode))
            victim.queueHit(
                2,
                if (ranged) HitType.Ranged else HitType.Magic,
                damage,
                services.boss.playerHitModifier,
            )
            if (ranged && percent <= VerzikRules.LOW_PERCENT) victim.statSub("stat.prayer", damage, 0)
        }
        if (phaseTicks % VerzikRules.BALL_RATE == 0) lightning(target)
        if (phaseTicks % VerzikRules.NYLOCAS_RATE == 0) summonNylocas()
        if (phaseTicks % VerzikRules.ATHANATOS_RATE == 0) summonAthanatos()
    }

    private fun lightning(target: Npc) {
        var current = playersInRoom().randomOrNull() ?: return
        val hit = mutableSetOf<Player>()
        repeat(VerzikRules.BALL_LINKS) {
            hit += current
            val damage = VerzikRules.LINK_MIN + services.random.of(VerzikRules.LINK_MAX - VerzikRules.LINK_MIN + 1)
            hurt(current, TobScaling.damage(damage, mode))
            current = playersInRoom().filter { it !in hit }.minByOrNull { it.coords.chebyshevDistance(current.coords) } ?: return
        }
    }

    private fun summonNylocas() {
        val types = listOf("melee", "ranged", "magic")
        val count = teamSize.coerceAtLeast(2)
        repeat(count) {
            if (nylocas.size >= VerzikRules.MAX_NYLOCAS) return
            val tile = randomTile()
            val npc = spawn("npc.verzik_nylocas_${types[services.random.of(3)]}$suffix", tile, NYLO_HP, stats = 50)
            nylocas += npc
        }
    }

    private fun summonAthanatos() {
        val npc = spawn("npc.tob_verzik_phase2_armourednylocas$suffix", randomTile(), ATHAN_HP, stats = 60)
        athanatos += Athanatos(npc, clock + VerzikRules.ATHANATOS_LIFE)
    }

    private fun tickNylocas() {
        for (npc in nylocas.toList()) {
            val victim = nearestPlayer(npc.coords) ?: continue
            if (victim.coords.chebyshevDistance(npc.coords) <= 1) {
                services.spotanimAt("spotanim.tob_bloat_blood_splat", npc.coords)
                for (player in playersInRoom()) {
                    if (player.coords.chebyshevDistance(npc.coords) <= 1) {
                        hurt(player, TobScaling.damage(VerzikRules.explode(services.random.of(64)), mode))
                    }
                }
                nylocas.remove(npc)
                remove(npc)
            } else if (clock % 2 == 0) {
                npc.walk(victim.coords)
            }
        }
    }

    private fun tickAthanatos(target: Npc) {
        for (entry in athanatos.toList()) {
            if (clock < entry.diesAt) continue
            heal(target, entry.npc.hitpoints)
            athanatos.remove(entry)
            remove(entry.npc)
        }
    }

    private fun spider(target: Npc) {
        val percent = VerzikRules.hpPercent(target.hitpoints, target.baseHitpointsLvl)
        if (phaseTicks % VerzikRules.phase3Rate(percent) != 0) return
        autos++
        val special = VerzikRules.special(autos, specials)
        if (special != null) {
            specials++
            when (special) {
                VerzikSpecial.Nylocas -> summonNylocas()
                VerzikSpecial.Webs -> webs()
                VerzikSpecial.Ball -> greenBall()
            }
            return
        }
        val victim = nearestPlayer(centre(target)) ?: return
        val near = victim.coords.chebyshevDistance(target.coords) <= 4
        val roll = services.random.of(if (near) 3 else 2)
        val (type, max) =
            when (roll) {
                0 -> HitType.Magic to VerzikRules.RANGED_MAX
                1 -> HitType.Ranged to VerzikRules.RANGED_MAX
                else -> HitType.Melee to VerzikRules.MELEE_MAX
            }
        victim.queueHit(
            2,
            type,
            services.random.of(0, TobScaling.damage(max, mode)),
            services.boss.playerHitModifier,
        )
    }

    private fun webs() {
        for (player in playersInRoom()) {
            val npc = spawn("npc.verzik_web_npc$suffix", sourceOf(player.coords), WEB_HP, stats = 1)
            webs += Web(npc, player, clock + VerzikRules.WEB_DELAY)
        }
        tell("<col=ef1020>Verzik flings webs! Break them or take the blow.</col>")
    }

    private fun tickWebs() {
        for (web in webs.toList()) {
            if (clock < web.explodesAt) continue
            webs.remove(web)
            val damage = VerzikRules.WEB_MIN + services.random.of(VerzikRules.WEB_MAX - VerzikRules.WEB_MIN + 1)
            hurt(web.victim, TobScaling.damage(damage, mode))
            remove(web.npc)
        }
    }

    private fun greenBall() {
        var current = playersInRoom().randomOrNull() ?: return
        val hit = mutableSetOf<Player>()
        repeat(2) {
            hit += current
            val damage = VerzikRules.GREEN_MIN + services.random.of(VerzikRules.GREEN_MAX - VerzikRules.GREEN_MIN + 1)
            hurt(current, TobScaling.damage(damage, mode))
            current = playersInRoom().filter { it !in hit }.minByOrNull { it.coords.chebyshevDistance(current.coords) } ?: return
        }
    }

    private fun clearHelpers() {
        for (npc in nylocas.toList()) remove(npc)
        for (entry in athanatos.toList()) remove(entry.npc)
        for (web in webs.toList()) remove(web.npc)
        nylocas.clear()
        athanatos.clear()
        webs.clear()
    }

    private fun pillarType(): String =
        when (mode) {
            TobMode.Entry -> "npc.verzik_story_pillar_npc"
            TobMode.Normal -> "npc.verzik_pillar_npc"
            TobMode.Hard -> "npc.verzik_hard_pillar_npc"
        }

    private fun sourceOf(world: CoordGrid): CoordGrid = room.toSource(raid.southWest, world)

    private fun randomTile(): CoordGrid =
        CoordGrid(
            VerzikRules.ARENA_X.first + services.random.of(VerzikRules.ARENA_X.count()),
            VerzikRules.ARENA_Z.first + services.random.of(VerzikRules.ARENA_Z.count()),
            0,
        )

    private fun centre(target: Npc): CoordGrid = target.coords.translate(1, 1)

    private companion object {
        const val BOSS_STAT = 200
        const val PILLAR_HP = 185
        const val NYLO_HP = 25
        const val ATHAN_HP = 180
        const val WEB_HP = 10
    }
}
