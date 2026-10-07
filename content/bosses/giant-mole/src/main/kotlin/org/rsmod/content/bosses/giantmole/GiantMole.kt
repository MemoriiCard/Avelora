package org.rsmod.content.bosses.giantmole

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.interrupt
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class GiantMole @Inject constructor(deps: BossDeps, private val darkness: MoleHoleDarkness) :
    BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register("giantmole.burrow") { _, npc, _, _ -> burrow(npc) }
    }

    override val spec = giantMoleSpec(::canBurrow)

    private fun canBurrow(npc: Npc): Boolean =
        npc.hitpoints > BURROW_MIN_HP && !deps.encounter(npc).invulnerable

    private fun burrow(npc: Npc) {
        val encounter = deps.encounter(npc)
        if (encounter.invulnerable) return
        encounter.invulnerable = true
        deps.interrupt(npc)
        deps.suppressAttacks(npc, BURROW_TICKS + 1)
        npc.noneMode()
        npc.anim("seq.mole_burrow_down")
        mapSpotanim("spotanim.mole_mude_hole_spotanim", npc.centre())
        if (deps.random.of(DIRT_ONE_IN) == 0) {
            throwDirt(npc)
        }
        deps.worldQueues.add(BURROW_TICKS) {
            PathingEntityCommon.telejump(npc, deps.collision, nextSpot(npc.coords))
            npc.anim("seq.mole_burrow_up")
            mapSpotanim("spotanim.mole_mud_hole_up_spotanim", npc.centre())
            encounter.invulnerable = false
            npc.defaultMode()
        }
    }

    private fun throwDirt(npc: Npc) {
        for (player in deps.playerList) {
            if (!npc.isWithinDistance(player, 1)) continue
            val doused = player.inv.extinguish() or player.worn.extinguish()
            if (!doused) continue
            player.mes("The Giant Mole's dirt extinguishes your light source!")
            darkness.update(player)
        }
    }

    private fun Inventory.extinguish(): Boolean {
        var doused = false
        for (slot in indices) {
            val obj = this[slot] ?: continue
            for ((lit, unlit) in MoleHoleDarkness.OPEN_FLAMES) {
                if (!obj.isType(lit)) continue
                this[slot] = InvObj(unlit, obj.count)
                doused = true
            }
        }
        return doused
    }

    private fun nextSpot(from: CoordGrid): CoordGrid {
        val far = MoleHole.BURROW_SPOTS.filter { it.chebyshevDistance(from) > MIN_BURROW_DISTANCE }
        val spots = far.ifEmpty { MoleHole.BURROW_SPOTS }
        return spots[deps.random.of(spots.size)]
    }

    private fun mapSpotanim(spot: String, coords: CoordGrid) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), coords, 0, 0)
    }

    private fun Npc.centre(): CoordGrid = coords.translate(size / 2, size / 2)

    private companion object {
        const val BURROW_TICKS = 3
        const val DIRT_ONE_IN = 2
        const val MIN_BURROW_DISTANCE = 10
    }
}
