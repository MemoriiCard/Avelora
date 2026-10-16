package org.rsmod.content.raids.toa.zebak

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

class ZebakRoom(raid: ToaRaid, services: CoxRoomServices, onCleared: () -> Unit) :
    ToaBossRoom(raid, ToaRoom.Zebak, services, onCleared) {
    private class Roar(val spots: List<CoordGrid>, val markers: List<Npc>, val landsAt: Int)

    private class Wave(val startedAt: Int, val gapCenter: Int)

    private var zebak: Npc? = null
    private var attacks = 0
    private var specialsDone = 0
    private var enraged = false
    private var roar: Roar? = null
    private val waves = mutableListOf<Wave>()
    private var nextWaveAt = 0
    private var wavesLeft = 0

    override fun begin() {
        zebak =
            spawn("npc.toa_zebak", ZebakRules.BOSS, scaledHp(ZebakRules.BASE_HP), ZebakRules.STAT)
        tell("Zebak rises from the water.")
    }

    override fun tick() {
        val boss = zebak ?: return
        val percent = ToaCombat.percent(boss.hitpoints, boss.baseHitpointsLvl)
        if (!enraged && percent <= ZebakRules.ENRAGE_PERCENT) enrage(boss)
        if (clock % ZebakRules.attackRate(percent) == 0 && roar == null && waves.isEmpty()) attack(boss)
        checkSpecial(percent)
        landRoar()
        sweepWaves()
    }

    override fun onNpcKilled(npc: Npc, hero: Player) {
        if (npc !== zebak) return
        clearMarkers()
        waves.clear()
        wavesLeft = 0
        tell("<col=ef1020>Zebak has been defeated!</col>")
        finish()
    }

    override fun onNpcRemoved(npc: Npc) {
        if (npc === zebak) zebak = null
    }

    private fun enrage(boss: Npc) {
        enraged = true
        transmog(boss, "npc.toa_zebak_enraged")
        tell("Zebak is enraged!")
    }

    private fun attack(boss: Npc) {
        val target = nearestPlayer(boss.coords) ?: return
        val adjacent = boss.coords.chebyshevDistance(target.coords) <= ZebakRules.SIZE / 2 + ZebakRules.MELEE_REACH
        val style = ZebakRules.style(attacks++, adjacent)
        val damage = scaledDamage(services.random.of(0, ZebakRules.maxHit(style)))
        hurtStyled(target, style, damage)
    }

    private fun checkSpecial(percent: Int) {
        if (roar != null || waves.isNotEmpty() || wavesLeft > 0) return
        val index = ZebakRules.nextSpecial(percent, specialsDone) ?: return
        specialsDone = index + 1
        if (ToaInvocation.NotJustAHead in invocations) blitz()
        if (ZebakRules.isRoar(index)) startRoar() else startWaves()
    }

    private fun blitz() {
        for (player in playersInRoom()) {
            hurtStyled(player, ToaStyle.Magic, scaledDamage(services.random.of(1, ZebakRules.BLITZ_MAX)))
        }
    }

    private fun startRoar() {
        val spots =
            List(ZebakRules.SAFE_SPOTS) {
                CoordGrid(
                    ZebakRules.SAFE_X.first + services.random.of(ZebakRules.SAFE_X.count()),
                    ZebakRules.SAFE_Z.first + services.random.of(ZebakRules.SAFE_Z.count()),
                    0,
                )
            }
        val markers = spots.map { spawn("npc.toa_zebak_safespot", it, 1) }
        roar = Roar(spots, markers, clock + ZebakRules.ROAR_DELAY)
        tell("Zebak inhales deeply. Stand beside a stone!")
    }

    private fun landRoar() {
        val current = roar ?: return
        if (clock < current.landsAt) return
        roar = null
        val radius = ZebakRules.safeRadius(ToaInvocation.UpsetStomach in invocations)
        for (player in playersInRoom()) {
            val at = raid.source(player.coords) ?: continue
            val safe = current.spots.any { it.chebyshevDistance(at) <= radius }
            if (!safe) hurt(player, scaledDamage(services.random.of(ZebakRules.ROAR_MAX / 2, ZebakRules.ROAR_MAX)))
        }
        clearMarkers(current)
    }

    private fun clearMarkers(current: Roar? = roar) {
        current?.markers?.forEach { remove(it) }
        if (current === roar) roar = null
    }

    private fun startWaves() {
        wavesLeft = ZebakRules.WAVES
        nextWaveAt = clock
        tell("The water surges. Find the gap in the waves!")
    }

    private fun sweepWaves() {
        if (wavesLeft > 0 && clock >= nextWaveAt) {
            val z = ZebakRules.WAVE_Z
            waves += Wave(clock, z.first + 2 + services.random.of(z.count() - 4))
            wavesLeft--
            nextWaveAt = clock + ZebakRules.WAVE_GAP_TICKS
        }
        val finished = mutableListOf<Wave>()
        for (wave in waves) {
            val x = ZebakRules.WAVE_X.first + (clock - wave.startedAt)
            if (x > ZebakRules.WAVE_X.last) {
                finished += wave
                continue
            }
            val sourceGap = ZebakRules.WAVE_Z.first
            for (player in playersInRoom()) {
                val source = raid.source(player.coords) ?: continue
                if (!ZebakRules.onWave(x, source.x)) continue
                if (source.z < sourceGap || ZebakRules.inGap(wave.gapCenter, source.z)) continue
                hurt(player, scaledDamage(services.random.of(ZebakRules.WAVE_MIN, ZebakRules.WAVE_MAX)))
            }
        }
        waves.removeAll(finished.toSet())
    }
}
