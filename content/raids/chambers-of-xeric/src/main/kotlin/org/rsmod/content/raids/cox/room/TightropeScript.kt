package org.rsmod.content.raids.cox.room

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.cox.raid.CoxRaids
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.ScriptContext

class TightropeScript
@Inject
constructor(
    deps: BossDeps,
    private val points: CoxDamagePoints,
    private val raids: CoxRaids,
    private val locRepo: LocRepository,
) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { points.award(this) })
        onOpLoc1(TightropeRoom.ROPE_END) { cross(it.loc) }
        onOpLoc1(TightropeRoom.KEYSTONE_LOC) { takeKeystone(it.loc) }
    }

    private suspend fun ProtectedAccess.cross(end: BoundLocInfo) {
        val raid = raids.containing(player) ?: return
        val room = raid.roomAt(end.coords)?.let(raid::controllerOf) as? TightropeRoom ?: return
        val landing = room.crossingFrom(end.coords) ?: return
        if (player.agilityLvl < room.agilityRequired) {
            mes("You need an Agility level of ${room.agilityRequired} to cross the tightrope.")
            return
        }
        room.arouse(player)
        anim(ROPE_SEQ)
        delay(room.distance(end.coords, landing).coerceAtLeast(1))
        resetAnim()
        telejump(landing)
    }

    private fun ProtectedAccess.takeKeystone(loc: BoundLocInfo) {
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        invAdd(inv, TightropeRoom.KEYSTONE)
        locRepo.del(loc, KEYSTONE_RESPAWN)
    }

    override val spec: BossSpec =
        boss(TightropeRoom.RANGER, TightropeRoom.MAGE) {
            stats(attackRate = 4)
            val arrow = ability("arrow") { missile("seq.human_bow", "spotanim.adamant_arrow_travel", HitType.Ranged) }
            val bolt =
                ability("bolt") {
                    missile("seq.human_caststrike_staff", "spotanim.fireblast_travel", HitType.Magic, impact = "spotanim.fireblast_impact")
                }
            phase("fight") { weightedSelectorRandom { +random(arrow, weight = 1); +random(bolt, weight = 1) } }
        }

    private companion object {
        const val ROPE_SEQ = "seq.myq3_human_tightrope"
        const val KEYSTONE_RESPAWN = 200
    }
}
